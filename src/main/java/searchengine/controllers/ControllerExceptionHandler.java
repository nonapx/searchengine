package searchengine.controllers;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import searchengine.dto.ShortErrorResponse;
import searchengine.exceptions.CustomBadRequestException;
import searchengine.exceptions.CustomBadUrlException;

import java.io.PrintWriter;
import java.io.StringWriter;

@RestControllerAdvice
public class ControllerExceptionHandler {
    @ExceptionHandler(CustomBadRequestException.class)
    public ResponseEntity<ShortErrorResponse> handleBadRequestException(Exception e) {
        return new ResponseEntity<>(
                new ShortErrorResponse(false, e.getMessage()),
                HttpStatus.BAD_REQUEST
        );
    }

    @ExceptionHandler(CustomBadUrlException.class)
    public ResponseEntity<ShortErrorResponse> handleBadUrlException(Exception e) {
        return new ResponseEntity<>(
                new ShortErrorResponse(false, e.getMessage()),
                HttpStatus.BAD_REQUEST
        );
    }

    // На всякий случай для остальных ошибок, но в таком же сокращенном формате - А.П.
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ShortErrorResponse> handleException(Exception e) {
        // Идея из https://auth0.com/blog/get-started-with-custom-error-handling-in-spring-boot-java/
        StringWriter stringWriter = new StringWriter();
        PrintWriter printWriter = new PrintWriter(stringWriter);
        e.printStackTrace(printWriter);
        String stackTrace = stringWriter.toString();
        return new ResponseEntity<>(
                new ShortErrorResponse(false, stackTrace),
                HttpStatus.INTERNAL_SERVER_ERROR
        );
    }
}