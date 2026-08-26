package com.az.chatroom.dtos;

import java.time.OffsetDateTime;

public record MessageResponse(
        String username,
        String content,
        OffsetDateTime createdAt
) {
}
