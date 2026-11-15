package at.fhtw.ocrworker.messaging;

import at.fhtw.ocrworker.ocr.OcrException;
import at.fhtw.ocrworker.ocr.OcrService;
import at.fhtw.ocrworker.storage.DocumentDownloadService;
import at.fhtw.ocrworker.storage.OcrResultStorageService;
import at.fhtw.ocrworker.storage.StorageException;
import com.rabbitmq.client.amqp.Connection;
import com.rabbitmq.client.amqp.Consumer;
import com.rabbitmq.client.amqp.ConsumerBuilder;
import com.rabbitmq.client.amqp.Message;
import com.rabbitmq.client.amqp.Publisher;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import tools.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.CompletableFuture;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OcrConsumerTest {

    private static final String PDF_KEY = "documents/test-key.pdf";
    private static final String RESULT_KEY = "ocr-results/7.txt";
    private static final String TEXT = "Rechnung über einen Laptop";

    @Mock
    private Connection connection;

    @Mock
    private ConsumerBuilder consumerBuilder;

    @Mock
    private Consumer rabbitConsumer;

    @Mock
    private Consumer.Context context;

    @Mock
    private Message incomingMessage;

    @Mock
    private Publisher failedJobPublisher;

    @Mock
    private Message failureMessage;

    @Mock
    private Publisher.Context failureConfirmation;

    @Mock
    private DocumentDownloadService downloadService;

    @Mock
    private OcrService ocrService;

    @Mock
    private OcrResultStorageService resultStorageService;

    @Mock
    private IndexJobProducer indexJobProducer;

    @TempDir
    Path temporaryDirectory;

    private Consumer.MessageHandler handler;
    private byte[] validBody;

    @BeforeEach
    void setup() {
        ObjectMapper objectMapper = new ObjectMapper();

        validBody = objectMapper.writeValueAsBytes(
                new DocumentUploadedMessage(
                        7L,
                        "test.pdf",
                        PDF_KEY
                )
        );

        when(connection.consumerBuilder()).thenReturn(consumerBuilder);
        when(consumerBuilder.queue(RabbitConfig.OCR_QUEUE))
                .thenReturn(consumerBuilder);

        when(consumerBuilder.messageHandler(any()))
                .thenReturn(consumerBuilder);

        when(consumerBuilder.build()).thenReturn(rabbitConsumer);

        OcrConsumer consumer = new OcrConsumer(
                connection,
                objectMapper,
                downloadService,
                failedJobPublisher,
                ocrService,
                resultStorageService,
                indexJobProducer
        );

        consumer.start();

        var handlerCaptor =
                ArgumentCaptor.forClass(Consumer.MessageHandler.class);

        verify(consumerBuilder).messageHandler(handlerCaptor.capture());
        handler = handlerCaptor.getValue();
    }

    @Test
    void acknowledgesOnlyAfterIndexingJobWasAccepted() throws IOException {
        Path pdf = prepareDownloadedPdf();
        CompletableFuture<Void> indexingConfirmation =
                prepareSuccessfulProcessing(pdf);

        deliver(validBody);
        verifyNoInteractions(context, failedJobPublisher);
        assertThat(pdf).doesNotExist();

        var order = inOrder(
                downloadService,
                ocrService,
                resultStorageService,
                indexJobProducer
        );

        order.verify(downloadService).download(PDF_KEY);
        order.verify(ocrService).extractText(pdf);
        order.verify(resultStorageService).store(7L, TEXT);
        order.verify(indexJobProducer).send(expectedIndexJob());

        indexingConfirmation.complete(null);

        verify(context).accept();
        verifyNoMoreInteractions(context);
        verifyNoInteractions(failedJobPublisher);
    }

    @Test
    void failedIndexingHandoverWaitsForFailureQueueConfirmation()
            throws IOException {

        Path pdf = prepareDownloadedPdf();
        CompletableFuture<Void> indexingConfirmation =
                prepareSuccessfulProcessing(pdf);

        prepareFailurePublisher();

        deliver(validBody);

        indexingConfirmation.completeExceptionally(
                new MessagingException("Indexing queue unavailable")
        );

        Publisher.Callback callback = captureFailureCallback(validBody);

        verifyNoInteractions(context);
        assertThat(pdf).doesNotExist();

        confirmFailureQueue(callback, Publisher.Status.ACCEPTED);

        verify(context).accept();
        verifyNoMoreInteractions(context);
    }

    @Test
    void ocrFailureForwardsOriginalMessageAndRemovesTemporaryPdf()
            throws IOException {

        Path pdf = prepareDownloadedPdf();

        when(ocrService.extractText(pdf))
                .thenThrow(new OcrException("Tesseract failed"));

        prepareFailurePublisher();

        deliver(validBody);

        captureFailureCallback(validBody);

        assertThat(pdf).doesNotExist();
        verifyNoInteractions(resultStorageService, indexJobProducer);
        verifyNoInteractions(context);
    }

    @Test
    void resultStorageFailurePreventsIndexing() throws IOException {
        Path pdf = prepareDownloadedPdf();

        when(ocrService.extractText(pdf)).thenReturn(TEXT);

        when(resultStorageService.store(7L, TEXT))
                .thenThrow(new StorageException(
                        "MinIO unavailable",
                        new IOException("Connection failed")
                ));

        prepareFailurePublisher();

        deliver(validBody);

        captureFailureCallback(validBody);

        verifyNoInteractions(indexJobProducer);
        verifyNoInteractions(context);
        assertThat(pdf).doesNotExist();
    }

    @Test
    void downloadFailurePreventsOcrAndIndexing() {
        when(downloadService.download(PDF_KEY))
                .thenThrow(new StorageException(
                        "Download failed",
                        new IOException("Connection failed")
                ));

        prepareFailurePublisher();

        deliver(validBody);

        captureFailureCallback(validBody);

        verifyNoInteractions(
                ocrService,
                resultStorageService,
                indexJobProducer
        );

        verifyNoInteractions(context);
    }

    @Test
    void invalidJobIsForwardedWithoutStartingProcessing() {
        byte[] invalidBody = "{}".getBytes(StandardCharsets.UTF_8);

        prepareFailurePublisher();

        deliver(invalidBody);

        Publisher.Callback callback = captureFailureCallback(invalidBody);

        verifyNoInteractions(
                downloadService,
                ocrService,
                resultStorageService,
                indexJobProducer
        );

        verifyNoInteractions(context);

        confirmFailureQueue(callback, Publisher.Status.ACCEPTED);

        verify(context).accept();
        verifyNoMoreInteractions(context);
    }

    @Test
    void rejectedFailureQueueMessageRequeuesOriginalMessage() {
        byte[] invalidBody = "{}".getBytes(StandardCharsets.UTF_8);

        prepareFailurePublisher();

        deliver(invalidBody);

        Publisher.Callback callback = captureFailureCallback(invalidBody);

        confirmFailureQueue(callback, Publisher.Status.REJECTED);

        verify(context).requeue();
        verifyNoMoreInteractions(context);
    }

    @Test
    void failurePublisherExceptionRequeuesOriginalMessage() {
        byte[] invalidBody = "{}".getBytes(StandardCharsets.UTF_8);

        prepareFailurePublisher();

        doThrow(new IllegalStateException("Publisher closed"))
                .when(failedJobPublisher)
                .publish(eq(failureMessage), any(Publisher.Callback.class));

        deliver(invalidBody);

        verify(context).requeue();
        verifyNoMoreInteractions(context);
    }

    private Path prepareDownloadedPdf() throws IOException {
        Path pdf = Files.writeString(
                temporaryDirectory.resolve("test.pdf"),
                "Simulated PDF data"
        );

        when(downloadService.download(PDF_KEY)).thenReturn(pdf);

        return pdf;
    }

    private CompletableFuture<Void> prepareSuccessfulProcessing(Path pdf) {
        when(ocrService.extractText(pdf)).thenReturn(TEXT);
        when(resultStorageService.store(7L, TEXT)).thenReturn(RESULT_KEY);

        CompletableFuture<Void> confirmation = new CompletableFuture<>();

        when(indexJobProducer.send(expectedIndexJob()))
                .thenReturn(confirmation);

        return confirmation;
    }

    private IndexDocumentMessage expectedIndexJob() {
        return new IndexDocumentMessage(7L, "test.pdf", RESULT_KEY);
    }

    private void deliver(byte[] body) {
        when(incomingMessage.body()).thenReturn(body);
        handler.handle(context, incomingMessage);
    }

    private void prepareFailurePublisher() {
        when(failedJobPublisher.message(any(byte[].class)))
                .thenReturn(failureMessage);
    }

    private Publisher.Callback captureFailureCallback(byte[] expectedBody) {
        var bodyCaptor = ArgumentCaptor.forClass(byte[].class);

        verify(failedJobPublisher).message(bodyCaptor.capture());

        assertThat(bodyCaptor.getValue()).containsExactly(expectedBody);

        var callbackCaptor =
                ArgumentCaptor.forClass(Publisher.Callback.class);

        verify(failedJobPublisher).publish(
                eq(failureMessage),
                callbackCaptor.capture()
        );

        return callbackCaptor.getValue();
    }

    private void confirmFailureQueue(
            Publisher.Callback callback,
            Publisher.Status status) {

        when(failureConfirmation.status()).thenReturn(status);
        callback.handle(failureConfirmation);
    }
}