package com.az.chatroom.rest;

import com.az.chatroom.exceptions.ResourceNotFoundException;
import com.az.chatroom.exceptions.UserAlreadyExistsException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

@RestControllerAdvice
public class GlobalExceptionHandler extends ResponseEntityExceptionHandler {
    private static final Logger LOGGER = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(UserAlreadyExistsException.class)
    public ProblemDetail handleUserAlreadyExistsException() {
        return ProblemDetail.forStatusAndDetail(
                HttpStatus.CONFLICT,
                "User already exists"
        );
    }

    @ExceptionHandler(ResourceNotFoundException.class)
    public ProblemDetail handleResourceNotFoundException(ResourceNotFoundException e) {
        LOGGER.error("Resource not found.", e);
        return ProblemDetail.forStatusAndDetail(
                HttpStatus.NOT_FOUND,
                "Resource not found."
        );
    }

    @ExceptionHandler(Exception.class)
    public ProblemDetail handleException(Exception e) {
        LOGGER.error("Unexpected error occurred. [message={}]", e.getMessage(), e);
        return ProblemDetail.forStatus(HttpStatus.INTERNAL_SERVER_ERROR);
    }
}
