package com.az.chatroom.repositories.projections;

import java.time.OffsetDateTime;

public record StatisticsRow(
        String username,
        int messageCount,
        OffsetDateTime firstMessageAt,
        OffsetDateTime lastMessageAt,
        double averageMessageLength,
        String lastMessageText
) {
}
