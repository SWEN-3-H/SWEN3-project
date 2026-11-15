package at.fhtw.ocrworker.messaging;

import com.rabbitmq.client.amqp.Message;
import com.rabbitmq.client.amqp.Publisher;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import tools.jackson.databind.ObjectMapper;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class IndexJobProducerTest {

    @Mock
    private Publisher publisher;

    @Mock
    private Message message;

    @Mock
    private Publisher.Context context;

    private IndexJobProducer producer;

    @BeforeEach
    void setup() {
        producer = new IndexJobProducer(
                publisher,
                new ObjectMapper(),
                10
        );

        when(publisher.message(any(byte[].class))).thenReturn(message);
    }

    @Test
    void completesOnlyAfterBrokerAcceptance() {
        var result = producer.send(
                new IndexDocumentMessage(7L, "test.pdf", "ocr-results/7.txt")
        );

        assertThat(result.isDone()).isFalse();

        var callback = ArgumentCaptor.forClass(Publisher.Callback.class);
        verify(publisher).publish(eq(message), callback.capture());

        when(context.status()).thenReturn(Publisher.Status.ACCEPTED);
        callback.getValue().handle(context);

        assertThat(result.isDone()).isTrue();
        assertThat(result.isCompletedExceptionally()).isFalse();
    }

    @Test
    void brokerRejectionFailsTheOperation() {
        var result = producer.send(
                new IndexDocumentMessage(7L, "test.pdf", "ocr-results/7.txt")
        );

        var callback = ArgumentCaptor.forClass(Publisher.Callback.class);
        verify(publisher).publish(eq(message), callback.capture());

        when(context.status()).thenReturn(Publisher.Status.REJECTED);
        callback.getValue().handle(context);

        assertThatThrownBy(() -> result.join())
                .hasCauseInstanceOf(MessagingException.class);
    }

    @Test
    void missingBrokerConfirmationCausesTimeout() {
        producer = new IndexJobProducer(
                publisher,
                new ObjectMapper(),
                1
        );

        var result = producer.send(
                new IndexDocumentMessage(
                        7L,
                        "test.pdf",
                        "ocr-results/7.txt"
                )
        );

        assertThatThrownBy(() -> result.get(3, TimeUnit.SECONDS))
                .hasCauseInstanceOf(TimeoutException.class);
    }
}