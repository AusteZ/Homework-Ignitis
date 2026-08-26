package com.az.chatroom.dtos;

import com.az.chatroom.enums.UserRole;

import java.time.Instant;
import java.util.UUID;

public record LoginResponse(
        String token,
        Instant expiresAt,
        UUID userId,
        String username,
        UserRole role
) {
}
