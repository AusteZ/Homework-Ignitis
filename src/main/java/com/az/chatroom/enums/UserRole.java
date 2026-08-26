package com.az.chatroom.enums;

public enum UserRole {
    ADMIN,
    USER;

    public String authority() {
        return name();
    }
}
