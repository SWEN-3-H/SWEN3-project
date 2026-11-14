package at.fhtw.ocrworker.messaging;

import at.fhtw.ocrworker.ocr.OcrService;
import at.fhtw.ocrworker.storage.DocumentDownloadService;
import at.fhtw.ocrworker.storage.OcrResultStorageService;
import com.rabbitmq.client.amqp.Connection;
import com.rabbitmq.client.amqp.Consumer;
import com.rabbitmq.client.amqp.Publisher;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

@Component
public class OcrConsumer {

    private static final Logger log =
            LoggerFactory.getLogger(OcrConsumer.class);

    private final Connection connection;
    private final ObjectMapper objectMapper;
    private final DocumentDownloadService downloadService;
    private final Publisher failedJobPublisher;
    private final OcrService ocrService;
    private Consumer consumer;
    private final OcrResultStorageService resultStorageService;

    public OcrConsumer(
            Connection connection,
            ObjectMapper objectMapper,
            DocumentDownloadService downloadService,
            Publisher failedJobPublisher,
            OcrService ocrService,
            OcrResultStorageService resultStorageService) {

        this.connection = connection;
        this.objectMapper = objectMapper;
        this.downloadService = downloadService;
        this.failedJobPublisher = failedJobPublisher;
        this.ocrService = ocrService;
        this.resultStorageService = resultStorageService;
    }

    @PostConstruct
    void start() {
        consumer = connection.consumerBuilder()
                .queue(RabbitConfig.OCR_QUEUE)
                .messageHandler((context, message) -> {
                    try {
                        DocumentUploadedMessage job =
                                objectMapper.readValue(
                                        message.body(),
                                        DocumentUploadedMessage.class
                                );

                        if (job == null
                                || job.documentId() == null
                                || job.objectKey() == null
                                || job.objectKey().isBlank()) {

                            throw new IllegalArgumentException(
                                    "OCR job requires documentId and objectKey"
                            );
                        }

                        process(job);
                        context.accept();

                    } catch (Exception e) {
                        log.error(
                                "OCR job failed; forwarding to failure queue",
                                e
                        );

                        forwardFailure(context, message.body());
                    }
                })
                .build();

        log.info("OCR consumer listening: queue={}", RabbitConfig.OCR_QUEUE);
    }

    private void process(DocumentUploadedMessage job) throws IOException {
        Path pdf = downloadService.download(job.objectKey());

        try {
            log.info(
                    "PDF downloaded: documentId={}, size={} bytes",
                    job.documentId(),
                    Files.size(pdf)
            );

            String text = ocrService.extractText(pdf);

            String resultKey = resultStorageService.store(
                    job.documentId(),
                    text
            );

            log.info(
                    "OCR job completed: documentId={}, characters={}, resultKey={}",
                    job.documentId(),
                    text.length(),
                    resultKey
            );

        } finally {
            try {
                Files.deleteIfExists(pdf);
            } catch (IOException e) {
                log.warn("Could not remove temporary PDF: path={}", pdf, e);
            }
        }
    }
    private void forwardFailure(Consumer.Context context, byte[] body) {
        try {
            failedJobPublisher.publish(
                    failedJobPublisher.message(body),
                    confirmation -> {
                        if (confirmation.status() == Publisher.Status.ACCEPTED) {
                            log.warn(
                                    "OCR job retained in queue={}",
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
            log.error("Could not forward failed OCR job", e);
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