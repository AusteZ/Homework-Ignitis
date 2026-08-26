package com.az.chatroom.utils;

import com.az.chatroom.dtos.MessageCursor;
import com.az.chatroom.exceptions.InvalidCursorException;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

public final class CursorCoderUtil {
    private static final ObjectMapper objectMapper = new ObjectMapper();

    private CursorCoderUtil() {
        throw new UnsupportedOperationException("Utility class");
    }

    public static MessageCursor decodeCursor(String cursor) {
        if (cursor == null || cursor.isBlank()) {
            return null;
        }

        try {
            String decoded = new String(Base64.getUrlDecoder().decode(cursor), StandardCharsets.UTF_8);
            return objectMapper.readValue(decoded, MessageCursor.class);
        } catch (IllegalArgumentException | JacksonException e) {
            throw new InvalidCursorException(e);
        }
    }

    public static String encodeCursor(MessageCursor cursor) {
        String cursorJson = objectMapper.writeValueAsString(cursor);
        return Base64.getUrlEncoder()
                .withoutPadding()
                .encodeToString(cursorJson.getBytes(StandardCharsets.UTF_8));
    }
}
