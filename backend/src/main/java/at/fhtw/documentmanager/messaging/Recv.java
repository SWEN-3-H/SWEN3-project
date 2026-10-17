package at.fhtw.documentmanager.messaging;

import com.rabbitmq.client.amqp.Connection;
import com.rabbitmq.client.amqp.Consumer;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import java.nio.charset.StandardCharsets;

@Slf4j
@Component
public class Recv {

    private final Connection connection;
    private Consumer consumer;

    public Recv(Connection connection){
        this.connection = connection;
    }

    @PostConstruct
    void start() {
        consumer = connection.consumerBuilder().queue(RabbitConfig.Queue_Name)
                .messageHandler((context, message) -> {
                    try {
                        String text = new String(message.body(), StandardCharsets.UTF_8);
                        log.info("Received message from queue '{}': '{}'",RabbitConfig.Queue_Name, text);
                        context.accept();
                    } catch (Exception e){
                        log.error("Faied to process message from queue '{}'",RabbitConfig.Queue_Name);
                        context.discard();
                    }
                }).build();
        log.info("Listening on queue '{}'", RabbitConfig.Queue_Name);
    }

    @PreDestroy
    void stop() {
        if (consumer != null){
            consumer.close();
        }
    }
}
