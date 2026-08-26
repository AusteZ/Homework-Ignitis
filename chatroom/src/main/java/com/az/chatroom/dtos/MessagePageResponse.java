package com.az.chatroom.dtos;

import java.util.List;

public record MessagePageResponse(
        List<MessageResponse> messages,
        int size,
        String nextCursor
) {
}
