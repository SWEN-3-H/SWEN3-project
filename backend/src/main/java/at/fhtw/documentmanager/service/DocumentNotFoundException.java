package at.fhtw.documentmanager.service;

public class DocumentNotFoundException extends RuntimeException {
    public DocumentNotFoundException(Long id) {
        super("Document mit ID " + id + " nicht gefunden");
    }
}