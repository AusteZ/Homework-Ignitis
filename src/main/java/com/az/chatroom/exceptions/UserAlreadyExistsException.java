package com.az.chatroom.exceptions;

public class UserAlreadyExistsException extends RuntimeException {

    public UserAlreadyExistsException(String username) {
        super("Username %s already exists.".formatted(username));
    }
}
