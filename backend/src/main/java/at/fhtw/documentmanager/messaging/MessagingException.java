package at.fhtw.documentmanager.messaging;

public class MessagingException extends RuntimeException{
    public MessagingException(String msg){
        super(msg);
    }

    public MessagingException(String message, Throwable cause) {
        super(message, cause);
    }
}
