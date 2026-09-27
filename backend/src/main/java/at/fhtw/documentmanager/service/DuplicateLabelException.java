package at.fhtw.documentmanager.service;

public class DuplicateLabelException extends RuntimeException {
    public DuplicateLabelException(String name) {
        super("Label with name '" + name + "' already exits");
    }
}