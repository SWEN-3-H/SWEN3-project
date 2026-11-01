package at.fhtw.documentmanager.messaging;

public record DocumentUploadedMessage(Long documentId, String fileName) {
}
