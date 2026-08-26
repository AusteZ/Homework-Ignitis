package com.az.chatroom.integration

import com.az.chatroom.enums.UserRole
import com.az.chatroom.utils.JwtCustomClaim
import com.az.generated.jooq.tables.records.AppUserRecord
import com.az.generated.jooq.tables.records.ChatMessageRecord
import org.jooq.DSLContext
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders
import org.springframework.transaction.annotation.Transactional
import spock.lang.Specification

import java.time.OffsetDateTime

import static com.az.chatroom.testutils.TestData.TEST_MESSAGE_CONTENT
import static com.az.chatroom.testutils.TestData.TEST_MESSAGE_ID
import static com.az.chatroom.testutils.TestData.TEST_PASSWORD_HASH
import static com.az.chatroom.testutils.TestData.TEST_ROLE
import static com.az.chatroom.testutils.TestData.TEST_USERNAME
import static com.az.chatroom.testutils.TestData.TEST_USER_ID
import static com.az.generated.jooq.Tables.APP_USER
import static com.az.generated.jooq.Tables.CHAT_MESSAGE
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status

@SpringBootTest(properties = [
        "spring.datasource.url=jdbc:h2:mem:chatdb-integration;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE"
])
@AutoConfigureMockMvc
@Transactional
class MessageIntegrationSpec extends Specification {

    @Autowired
    MockMvc mockMvc

    @Autowired
    DSLContext dsl

    def "delete user endpoint anonymizes that user's messages through service transaction"() {
        given:
        def otherUserId = UUID.randomUUID()
        def otherMessageId = UUID.randomUUID()
        insertUser(TEST_USER_ID, TEST_USERNAME, TEST_ROLE)
        insertUser(otherUserId, "other-username", TEST_ROLE)
        insertMessage(TEST_MESSAGE_ID, TEST_USER_ID, TEST_MESSAGE_CONTENT, OffsetDateTime.now())
        insertMessage(otherMessageId, otherUserId, "message to keep linked", OffsetDateTime.now())

        when:
        def result = mockMvc.perform(MockMvcRequestBuilders.delete("/api/admin/users/{userId}", TEST_USER_ID)
                .with(adminJwt()))

        then:
        result.andExpect(status().isNoContent())

        and:
        dsl.select(CHAT_MESSAGE.USER_ID)
                .from(CHAT_MESSAGE)
                .where(CHAT_MESSAGE.ID.eq(TEST_MESSAGE_ID))
                .fetchOne(CHAT_MESSAGE.USER_ID) == null

        dsl.select(CHAT_MESSAGE.USER_ID)
                .from(CHAT_MESSAGE)
                .where(CHAT_MESSAGE.ID.eq(otherMessageId))
                .fetchOne(CHAT_MESSAGE.USER_ID) == otherUserId

        !dsl.fetchExists(APP_USER, APP_USER.ID.eq(TEST_USER_ID))
    }

    private void insertUser(UUID userId, String username, UserRole role) {
        def user = new AppUserRecord()
        user.id = userId
        user.username = username
        user.role = role.name()
        user.createdAt = OffsetDateTime.now()
        user.passwordHash = TEST_PASSWORD_HASH

        dsl.insertInto(APP_USER)
                .set(user)
                .execute()
    }

    private void insertMessage(UUID messageId, UUID userId, String content, OffsetDateTime createdAt) {
        dsl.insertInto(CHAT_MESSAGE)
                .set(new ChatMessageRecord(messageId, userId, content, createdAt))
                .execute()
    }

    private static def adminJwt() {
        jwt()
                .jwt { token ->
                    token.claim(JwtCustomClaim.USER_ID, UUID.randomUUID().toString())
                            .claim(JwtCustomClaim.ROLE, UserRole.ADMIN.name())
                }
                .authorities(new SimpleGrantedAuthority(UserRole.ADMIN.authority()))
    }
}
