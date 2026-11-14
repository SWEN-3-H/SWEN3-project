package at.fhtw.ocrworker.messaging;

public record DocumentUploadedMessage(
        Long documentId,
        String fileName,
        String objectKey
) {
}