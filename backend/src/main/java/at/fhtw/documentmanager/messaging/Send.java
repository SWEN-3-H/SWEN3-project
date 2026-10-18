package at.fhtw.documentmanager.messaging;

import com.rabbitmq.client.amqp.Connection;
import com.rabbitmq.client.amqp.Publisher;
import jakarta.annotation.PreDestroy;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Configuration;
import org.springframework.stereotype.Component;
import java.nio.charset.StandardCharsets;

@Slf4j
@Component
public class Send {

    private final Publisher publisher;

    public Send(Connection connection) {
        this.publisher = connection.publisherBuilder().queue(RabbitConfig.Queue_Name).build();
    }

    public synchronized void send (String text) {
        publisher.publish(
                publisher.message(text.getBytes(StandardCharsets.UTF_8)),
                context -> {
                    if(context.status() == Publisher.Status.ACCEPTED) {
                        log.info("Message sent to queue ''{}': '{}'",RabbitConfig.Queue_Name, text);
                    } else {
                        log.warn("Message not accepted by queue '{}': status={}",RabbitConfig.Queue_Name, context.status());
                    }
                }
        );
    }

    @PreDestroy
    void close() {
        publisher.close();
    }
}
