package com.az.chatroom.exceptions;

public class InvalidCursorException extends RuntimeException {
    public InvalidCursorException(Throwable cause) {
        super("Invalid cursor.", cause);
    }
}
