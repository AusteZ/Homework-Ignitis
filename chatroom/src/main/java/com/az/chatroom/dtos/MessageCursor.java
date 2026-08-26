package com.az.chatroom.dtos;

import java.time.OffsetDateTime;
import java.util.UUID;

public record MessageCursor(OffsetDateTime createdAt, UUID messageId) {
}
