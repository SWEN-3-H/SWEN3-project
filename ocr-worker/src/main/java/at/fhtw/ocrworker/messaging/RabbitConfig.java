package at.fhtw.ocrworker.messaging;

import com.rabbitmq.client.amqp.Connection;
import com.rabbitmq.client.amqp.Environment;
import com.rabbitmq.client.amqp.impl.AmqpEnvironmentBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitConfig {

    private static final Logger log =
            LoggerFactory.getLogger(RabbitConfig.class);

    public static final String OCR_QUEUE = "document.ocr";

    @Bean(destroyMethod = "close")
    public Environment amqpEnvironment(
            @Value("${app.rabbitmq.uri}") String uri) {

        return new AmqpEnvironmentBuilder()
                .connectionSettings()
                .uri(uri)
                .environmentBuilder()
                .build();
    }

    @Bean(destroyMethod = "close")
    public Connection amqpConnection(Environment amqpEnvironment) {
        Connection connection = amqpEnvironment
                .connectionBuilder()
                .build();

        try {
            connection.management()
                    .queue(OCR_QUEUE)
                    .quorum()
                    .queue()
                    .declare();

            log.info("RabbitMQ connected: queue={}", OCR_QUEUE);
            return connection;
        } catch (RuntimeException e) {
            connection.close();
            throw e;
        }
    }
}