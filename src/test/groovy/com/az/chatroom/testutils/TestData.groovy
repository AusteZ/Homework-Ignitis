package com.az.chatroom.testutils

import com.az.chatroom.enums.UserRole

import java.time.OffsetDateTime
import java.time.ZoneOffset

class TestData {
    public static final UUID TEST_USER_ID = UUID.fromString("11111111-1111-1111-1111-111111111111")
    public static final String TEST_USERNAME = "username"
    public static final UserRole TEST_ROLE = UserRole.USER
    public static final OffsetDateTime TEST_DATE_TIME = OffsetDateTime
            .of(2026, 8, 24, 18, 37, 0, 0,
                    ZoneOffset.of("+03:00"));
}
