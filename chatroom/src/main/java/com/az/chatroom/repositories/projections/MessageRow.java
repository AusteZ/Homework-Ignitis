package com.az.chatroom.repositories.projections;

import java.time.OffsetDateTime;
import java.util.UUID;

public record MessageRow(
        UUID id,
        String username,
        String content,
        OffsetDateTime createdAt
) {
}
