package at.fhtw.ocrworker.messaging;

import com.rabbitmq.client.amqp.Connection;
import com.rabbitmq.client.amqp.Consumer;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;

@Component
public class OcrConsumer {

    private static final Logger log =
            LoggerFactory.getLogger(OcrConsumer.class);

    private final Connection connection;
    private final ObjectMapper objectMapper;

    private Consumer consumer;

    public OcrConsumer(
            Connection connection,
            ObjectMapper objectMapper) {

        this.connection = connection;
        this.objectMapper = objectMapper;
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

                        if (job.documentId() == null
                                || job.objectKey() == null
                                || job.objectKey().isBlank()) {

                            throw new IllegalArgumentException(
                                    "OCR job requires documentId and objectKey"
                            );
                        }

                        log.info(
                                "OCR job received: documentId={}, "
                                        + "fileName={}, objectKey={}. "
                                        + "OCR not implemented yet",
                                job.documentId(),
                                job.fileName(),
                                job.objectKey()
                        );

                        context.accept();
                    } catch (Exception e) {
                        log.error("Could not process OCR message", e);
                        context.discard();
                    }
                })
                .build();

        log.info("OCR consumer listening: queue={}", RabbitConfig.OCR_QUEUE);
    }

    @PreDestroy
    void stop() {
        if (consumer != null) {
            consumer.close();
        }
    }
}