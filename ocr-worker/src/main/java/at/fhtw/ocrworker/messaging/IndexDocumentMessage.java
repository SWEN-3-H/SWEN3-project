package at.fhtw.ocrworker.messaging;

public record IndexDocumentMessage(
        Long documentId,
        String fileName,
        String textObjectKey
) {
}