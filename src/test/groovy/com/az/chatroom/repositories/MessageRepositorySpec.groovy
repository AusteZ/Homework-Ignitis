package com.az.chatroom.repositories

import com.az.chatroom.testutils.UserTestHelper
import com.az.generated.jooq.tables.records.ChatMessageRecord
import org.jooq.DSLContext
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.jooq.test.autoconfigure.JooqTest
import org.springframework.context.annotation.Import
import org.springframework.transaction.annotation.Transactional
import spock.lang.Specification

import java.time.OffsetDateTime

import static com.az.chatroom.testutils.TestData.TEST_DATE_TIME
import static com.az.chatroom.testutils.TestData.TEST_MESSAGE_CONTENT
import static com.az.chatroom.testutils.TestData.TEST_MESSAGE_ID
import static com.az.chatroom.testutils.TestData.TEST_USERNAME
import static com.az.chatroom.testutils.TestData.TEST_USER_ID
import static com.az.generated.jooq.Tables.APP_USER
import static com.az.generated.jooq.Tables.CHAT_MESSAGE

@JooqTest
@Import(MessageRepository)
@Transactional
class MessageRepositorySpec extends Specification {

    @Autowired
    MessageRepository messageRepository

    @Autowired
    DSLContext dsl

    def "should create message"() {
        given:
        insertUser()
        def message = new ChatMessageRecord(TEST_MESSAGE_ID, TEST_USER_ID, TEST_MESSAGE_CONTENT, TEST_DATE_TIME)

        when:
        messageRepository.create(message)

        then:
        def savedMessage = dsl.selectFrom(CHAT_MESSAGE)
                .where(CHAT_MESSAGE.ID.eq(TEST_MESSAGE_ID))
                .fetchOne()

        verifyAll(savedMessage) {
            id == TEST_MESSAGE_ID
            userId == TEST_USER_ID
            content == TEST_MESSAGE_CONTENT
            createdAt == TEST_DATE_TIME
        }
    }

    def "should anonymize messages by user id"() {
        given:
        insertUser()
        def otherUserId = UUID.randomUUID()
        insertUser(otherUserId, "other")
        def firstMessageId = UUID.randomUUID()
        def secondMessageId = UUID.randomUUID()
        def otherMessageId = UUID.randomUUID()
        insertMessage(firstMessageId, TEST_USER_ID, "first", TEST_DATE_TIME)
        insertMessage(secondMessageId, TEST_USER_ID, "second", TEST_DATE_TIME.minusMinutes(1))
        insertMessage(otherMessageId, otherUserId, "other", TEST_DATE_TIME.minusMinutes(2))

        when:
        messageRepository.anonymizeByUserId(TEST_USER_ID)

        then:
        dsl.select(CHAT_MESSAGE.USER_ID)
                .from(CHAT_MESSAGE)
                .where(CHAT_MESSAGE.ID.in(firstMessageId, secondMessageId))
                .fetch(CHAT_MESSAGE.USER_ID)
                .every { it == null }

        dsl.select(CHAT_MESSAGE.USER_ID)
                .from(CHAT_MESSAGE)
                .where(CHAT_MESSAGE.ID.eq(otherMessageId))
                .fetchOne(CHAT_MESSAGE.USER_ID) == otherUserId
    }

    def "should fetch messages newest first with one extra row for next page detection"() {
        given:
        def size = 2
        insertUser()
        def latestMessageId = UUID.randomUUID()
        def secondMessageId = UUID.randomUUID()
        def thirdMessageId = UUID.randomUUID()
        insertMessage(thirdMessageId, TEST_USER_ID, "third", TEST_DATE_TIME.minusMinutes(2))
        insertMessage(secondMessageId, TEST_USER_ID, "second", TEST_DATE_TIME.minusMinutes(1))
        insertMessage(latestMessageId, TEST_USER_ID, "latest", TEST_DATE_TIME)
        insertMessage(UUID.randomUUID(), TEST_USER_ID, "not fetched", TEST_DATE_TIME.minusMinutes(3))

        when:
        def messages = messageRepository.fetchMessages(null, null, size)

        then:
        verifyAll {
            messages.size() == size + 1
            messages*.id() == [latestMessageId, secondMessageId, thirdMessageId]
            messages*.username() == [TEST_USERNAME, TEST_USERNAME, TEST_USERNAME]
        }
    }

    def "should fetch messages after cursor"() {
        given:
        def size = 2
        insertUser()
        def latestMessageId = UUID.randomUUID()
        def cursorMessageId = UUID.randomUUID()
        def olderMessageId = UUID.randomUUID()
        insertMessage(olderMessageId, TEST_USER_ID, "older", TEST_DATE_TIME.minusMinutes(2))
        insertMessage(cursorMessageId, TEST_USER_ID, "cursor", TEST_DATE_TIME.minusMinutes(1))
        insertMessage(latestMessageId, TEST_USER_ID, "latest", TEST_DATE_TIME)

        when:
        def messages = messageRepository.fetchMessages(TEST_DATE_TIME.minusMinutes(1), cursorMessageId, size)

        then:
        messages*.id() == [olderMessageId]
    }

    def "should fetch anonymized message with null username"() {
        given:
        def messageId = UUID.randomUUID()
        insertMessage(messageId, null, TEST_MESSAGE_CONTENT, TEST_DATE_TIME)

        when:
        def messages = messageRepository.fetchMessages(null, null, 10)

        then:
        messages.size() == 1
        verifyAll(messages.first()) {
            id() == messageId
            username() == null
            content() == TEST_MESSAGE_CONTENT
            createdAt() == TEST_DATE_TIME
        }
    }

    def "should aggregate statistics for requested user and exclude other messages before joining"() {
        given:
        insertUser()
        def otherUserId = UUID.randomUUID()
        insertUser(otherUserId, "other")
        insertMessage(UUID.randomUUID(), TEST_USER_ID, "hey", TEST_DATE_TIME.minusMinutes(2))
        insertMessage(TEST_MESSAGE_ID, TEST_USER_ID, "hello", TEST_DATE_TIME)
        insertMessage(UUID.randomUUID(), otherUserId, "other later message", TEST_DATE_TIME.plusMinutes(2))
        insertMessage(UUID.randomUUID(), null, "anonymous first", TEST_DATE_TIME.minusMinutes(1))
        insertMessage(UUID.randomUUID(), null, "anonymous last", TEST_DATE_TIME.plusMinutes(1))

        when:
        def stats = messageRepository.fetchUserMessageStats(TEST_USER_ID)

        then:
        stats.present

        verifyAll(stats.get()) {
            username() == TEST_USERNAME
            messageCount() == 2
            firstMessageAt() == TEST_DATE_TIME.minusMinutes(2)
            lastMessageAt() == TEST_DATE_TIME
            averageMessageLength() == 4
            lastMessageText() == "hello"
        }
    }

    def "should return empty statistics for user with no messages"() {
        given:
        insertUser()

        expect:
        messageRepository.fetchUserMessageStats(TEST_USER_ID).empty
    }

    def "should return empty statistics for deleted user with anonymized messages"() {
        given:
        insertUser()
        insertMessage(UUID.randomUUID(), TEST_USER_ID, TEST_MESSAGE_CONTENT, TEST_DATE_TIME)
        messageRepository.anonymizeByUserId(TEST_USER_ID)
        dsl.deleteFrom(APP_USER)
                .where(APP_USER.ID.eq(TEST_USER_ID))
                .execute()

        expect:
        messageRepository.fetchUserMessageStats(TEST_USER_ID).empty
    }

    private void insertUser(UUID userId = TEST_USER_ID, String username = TEST_USERNAME) {
        dsl.insertInto(APP_USER)
                .set(UserTestHelper.userRecord(userId, username))
                .execute()
    }

    private void insertMessage(UUID messageId,
                               UUID userId,
                               String content,
                               OffsetDateTime createdAt) {
        dsl.insertInto(CHAT_MESSAGE)
                .set(new ChatMessageRecord(messageId, userId, content, createdAt))
                .execute()
    }
}
