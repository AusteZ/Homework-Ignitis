package com.az.chatroom.dtos;

import java.time.OffsetDateTime;

public record StatisticsResponse(
        String username,
        int messageCount,
        OffsetDateTime firstMessageAt,
        OffsetDateTime lastMessageAt,
        double averageMessageLength,
        String lastMessageText
) {
}
