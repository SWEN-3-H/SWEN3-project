package at.fhtw.ocrworker.messaging;

import com.rabbitmq.client.amqp.Message;
import com.rabbitmq.client.amqp.Publisher;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

@Component
public class IndexJobProducer {

    private final Publisher publisher;
    private final ObjectMapper objectMapper;
    private final long confirmTimeoutSeconds;

    public IndexJobProducer(
            @Qualifier("indexJobPublisher") Publisher publisher,
            ObjectMapper objectMapper,
            @Value("${app.indexing.confirm-timeout-seconds}")
            long confirmTimeoutSeconds) {

        if (confirmTimeoutSeconds <= 0) {
            throw new IllegalArgumentException(
                    "Publisher confirmation timeout must be positive"
            );
        }

        this.publisher = publisher;
        this.objectMapper = objectMapper;
        this.confirmTimeoutSeconds = confirmTimeoutSeconds;
    }

    public CompletableFuture<Void> send(IndexDocumentMessage job) {
        CompletableFuture<Void> confirmation = new CompletableFuture<>();

        try {
            byte[] body = objectMapper.writeValueAsBytes(job);

            Message message = publisher.message(body);
            message.contentType("application/json");
            message.durable(true);

            publisher.publish(message, context -> {
                if (context.status() == Publisher.Status.ACCEPTED) {
                    confirmation.complete(null);
                } else {
                    confirmation.completeExceptionally(
                            new MessagingException(
                                    "Indexing job was not accepted: documentId="
                                            + job.documentId()
                                            + ", status=" + context.status(),
                                    context.failureCause()
                            )
                    );
                }
            });

        } catch (Exception e) {
            confirmation.completeExceptionally(
                    new MessagingException(
                            "Could not send indexing job for document "
                                    + job.documentId(),
                            e
                    )
            );
        }

        return confirmation.orTimeout(
                confirmTimeoutSeconds,
                TimeUnit.SECONDS
        );
    }
}