package com.az.chatroom.dtos;

import com.az.chatroom.enums.UserRole;

import java.time.OffsetDateTime;
import java.util.UUID;

public record UserResponse(
        UUID userId,
        String username,
        UserRole role,
        OffsetDateTime createdAt
) {
}
