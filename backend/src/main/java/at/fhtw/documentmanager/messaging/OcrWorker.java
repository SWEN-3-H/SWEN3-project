package at.fhtw.documentmanager.messaging;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;
import com.rabbitmq.client.amqp.Connection;
import com.rabbitmq.client.amqp.Consumer;

@Slf4j
@Component
public class OcrWorker {
    private final Connection connection;
    private final ObjectMapper objectMapper;
    private Consumer consumer;

    public OcrWorker(Connection connection, ObjectMapper objectMapper){
        this.connection = connection;
        this.objectMapper = objectMapper;
    }

    @PostConstruct
    void start(){
        consumer = connection.consumerBuilder().queue(RabbitConfig.OCR_QUEUE).messageHandler((ctx, message) -> {
            try {
                var msg = objectMapper.readValue(message.body(),DocumentUploadedMessage.class);
                log.info("OCR worker received document {} ({}). OCR not implemented yet", msg.documentId(), msg.fileName());
                ctx.accept();
            } catch (Exception e){
                log.error("OCR Worker failed to process message", e);
                ctx.discard();
            }
        }).build();
        log.info("OCR eorker listening on queue {}", RabbitConfig.OCR_QUEUE);
    }

    @PreDestroy
    void stop(){
        if(consumer != null){
            consumer.close();
        }
    }
}
