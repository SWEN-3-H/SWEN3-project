package at.fhtw.ocrworker.messaging;

import at.fhtw.ocrworker.indexing.IndexService;
import at.fhtw.ocrworker.storage.OcrResultStorageService;
import com.rabbitmq.client.amqp.Connection;
import com.rabbitmq.client.amqp.Consumer;
import com.rabbitmq.client.amqp.Publisher;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;

@Component
public class IndexConsumer {

    private static final Logger log =
            LoggerFactory.getLogger(IndexConsumer.class);

    private final Connection connection;
    private final ObjectMapper objectMapper;
    private final OcrResultStorageService resultStorageService;
    private final IndexService indexService;
    private final Publisher failedJobPublisher;
    private Consumer consumer;

    public IndexConsumer(
            Connection connection,
            ObjectMapper objectMapper,
            OcrResultStorageService resultStorageService,
            IndexService indexService,
            @Qualifier("failedJobPublisher") Publisher failedJobPublisher) {

        this.connection = connection;
        this.objectMapper = objectMapper;
        this.resultStorageService = resultStorageService;
        this.indexService = indexService;
        this.failedJobPublisher = failedJobPublisher;
    }

    @PostConstruct
    void start() {
        consumer = connection.consumerBuilder()
                .queue(RabbitConfig.INDEX_QUEUE)
                .messageHandler((context, message) -> {
                    try {
                        IndexDocumentMessage job =
                                objectMapper.readValue(
                                        message.body(),
                                        IndexDocumentMessage.class
                                );

                        if (job == null
                                || job.documentId() == null
                                || job.textObjectKey() == null
                                || job.textObjectKey().isBlank()) {

                            throw new IllegalArgumentException(
                                    "Index job requires documentId and textObjectKey"
                            );
                        }

                        String text = resultStorageService.load(job.textObjectKey());

                        indexService.index(job.documentId(), job.fileName(), text);

                        log.info("Index job done: documentId={}", job.documentId());
                        context.accept();

                    } catch (Exception e) {
                        log.error(
                                "Index job failed; forwarding to failure queue",
                                e
                        );

                        forwardFailure(context, message.body());
                    }
                })
                .build();

        log.info("Index consumer listening: queue={}", RabbitConfig.INDEX_QUEUE);
    }

    private void forwardFailure(Consumer.Context context, byte[] body) {
        try {
            failedJobPublisher.publish(
                    failedJobPublisher.message(body),
                    confirmation -> {
                        if (confirmation.status() == Publisher.Status.ACCEPTED) {
                            log.warn(
                                    "Index job retained in queue={}",
                                    RabbitConfig.OCR_FAILED_QUEUE
                            );

                            context.accept();
                        } else {
                            log.error(
                                    "Failure queue rejected message: status={}",
                                    confirmation.status()
                            );

                            context.requeue();
                        }
                    }
            );
        } catch (Exception e) {
            log.error("Could not forward failed index job", e);
            context.requeue();
        }
    }

    @PreDestroy
    void stop() {
        if (consumer != null) {
            consumer.close();
        }
    }
}