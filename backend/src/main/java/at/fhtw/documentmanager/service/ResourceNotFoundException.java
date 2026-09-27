package at.fhtw.documentmanager.service;

public class ResourceNotFoundException extends RuntimeException {
    public ResourceNotFoundException(String resourceName, Long id) {
        super(resourceName + " with ID " + id + " not found");
    }
}
