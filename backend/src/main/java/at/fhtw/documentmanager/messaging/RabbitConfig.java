package at.fhtw.documentmanager.messaging;

import com.rabbitmq.client.amqp.Connection;
import com.rabbitmq.client.amqp.Environment;
import com.rabbitmq.client.amqp.impl.AmqpEnvironmentBuilder;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Slf4j
@Configuration
public class RabbitConfig {

    public static final String Queue_Name = "hello";
    public static final String OCR_QUEUE = "document.ocr";

    @Bean(destroyMethod = "close")
    public Environment aEnviornment(@Value("${app.rabbitmq.uri}") String uri) {
        return new AmqpEnvironmentBuilder().connectionSettings().uri(uri).environmentBuilder().build();
    }

    @Bean(destroyMethod = "close")
    public Connection aConnection(Environment aEnviornment ) {
        Connection connection = aEnviornment.connectionBuilder().build();
        connection.management().queue(Queue_Name).quorum().queue().declare();
        connection.management().queue(OCR_QUEUE).quorum().queue().declare();
        log.info("Connected to RabbitMQ, queue '{} declared, queue '{} declared", Queue_Name, OCR_QUEUE);
        return connection;
    }
}
