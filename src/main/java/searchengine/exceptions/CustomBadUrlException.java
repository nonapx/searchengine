package searchengine.exceptions;

public class CustomBadUrlException extends RuntimeException {
    public CustomBadUrlException(String message) {
        super(message);
    }
}
