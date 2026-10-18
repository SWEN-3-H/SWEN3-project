package at.fhtw.documentmanager.service;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.CONFLICT)
public class DuplicateLabelException extends RuntimeException {

    public DuplicateLabelException(String name) {
        super("Label with name '" + name + "' already exists");
    }
}