package com.az.chatroom.testutils

import com.az.generated.jooq.tables.records.AppUserRecord

import java.time.OffsetDateTime

import static com.az.chatroom.testutils.TestData.TEST_DATE_TIME
import static com.az.chatroom.testutils.TestData.TEST_PASSWORD_HASH
import static com.az.chatroom.testutils.TestData.TEST_ROLE
import static com.az.chatroom.testutils.TestData.TEST_USERNAME
import static com.az.chatroom.testutils.TestData.TEST_USER_ID

class UserTestHelper {
    static AppUserRecord userRecord(UUID id = TEST_USER_ID, String username = TEST_USERNAME, OffsetDateTime createdAt = TEST_DATE_TIME) {
        def user = new AppUserRecord()
        user.id = id
        user.username = username
        user.role = TEST_ROLE.name()
        user.createdAt = createdAt
        user.passwordHash = TEST_PASSWORD_HASH
        user
    }
}
