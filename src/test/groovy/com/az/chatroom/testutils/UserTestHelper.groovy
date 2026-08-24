package com.az.chatroom.testutils

import com.az.generated.jooq.tables.records.AppUserRecord

import java.time.OffsetDateTime

import static com.az.chatroom.testutils.TestData.TEST_DATE_TIME
import static com.az.chatroom.testutils.TestData.TEST_ROLE
import static com.az.chatroom.testutils.TestData.TEST_USERNAME
import static com.az.chatroom.testutils.TestData.TEST_USER_ID

class UserTestHelper {
    static AppUserRecord userRecord(UUID id = TEST_USER_ID, String username = TEST_USERNAME, OffsetDateTime createdAt = TEST_DATE_TIME) {
        new AppUserRecord(id, username, TEST_ROLE.name(), createdAt)
    }
}
