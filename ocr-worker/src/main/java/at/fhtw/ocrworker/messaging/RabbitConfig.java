package at.fhtw.ocrworker.messaging;

import com.rabbitmq.client.amqp.Connection;
import com.rabbitmq.client.amqp.Environment;
import com.rabbitmq.client.amqp.impl.AmqpEnvironmentBuilder;
import com.rabbitmq.client.amqp.Publisher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitConfig {

    private static final Logger log = LoggerFactory.getLogger(RabbitConfig.class);
    public static final String OCR_QUEUE = "document.ocr";
    public static final String OCR_FAILED_QUEUE = "document.ocr.failed";
    public static final String INDEX_QUEUE = "document.index";

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
            log.info("RabbitMQ queue ready: queue={}", OCR_QUEUE);

            connection.management()
                    .queue(OCR_FAILED_QUEUE)
                    .quorum()
                    .queue()
                    .declare();
            log.info("RabbitMQ queue ready: queue={}", OCR_FAILED_QUEUE);

            connection.management()
                    .queue(INDEX_QUEUE)
                    .quorum()
                    .queue()
                    .declare();
            log.info("RabbitMQ queue ready: queue={}", INDEX_QUEUE);

            return connection;
        } catch (RuntimeException e) {
            connection.close();
            throw e;
        }
    }

    @Bean(destroyMethod = "close")
    public Publisher failedJobPublisher(Connection connection) {
        return connection.publisherBuilder()
                .queue(OCR_FAILED_QUEUE)
                .build();
    }

    @Bean(destroyMethod = "close")
    public Publisher indexJobPublisher(Connection connection) {
        return connection.publisherBuilder()
                .queue(INDEX_QUEUE)
                .build();
    }
}