package at.fhtw.documentmanager.messaging;

import com.rabbitmq.client.amqp.Connection;
import jakarta.annotation.PreDestroy;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import com.rabbitmq.client.amqp.Publisher;
import tools.jackson.databind.ObjectMapper;

@Slf4j
@Component
public class OcrMessageProducer {
    private final Publisher publisher;
    private final ObjectMapper objectMapper;

    public OcrMessageProducer(Connection connection, ObjectMapper objectMapper){
        this.publisher = connection.publisherBuilder().queue(RabbitConfig.OCR_QUEUE).build();
        this.objectMapper = objectMapper;
    }

    public synchronized void sendDocumentUploaded(DocumentUploadedMessage msg) {
        try{
            byte[] body = objectMapper.writeValueAsBytes(msg);
            publisher.publish(publisher.message(body), ctx -> {
                if(ctx.status() == Publisher.Status.ACCEPTED){
                    log.info("OCR job queued for document {}", msg.documentId());
                } else {
                    log.warn("OCR job for document {} not accepted {}", msg.documentId(), ctx.status());
                }
            });
        } catch (Exception e){
            throw new MessagingException("Could not send OCR job for document " + msg.documentId(), e);
        }
    }

    @PreDestroy
    void close(){
        publisher.close();
    }
}
