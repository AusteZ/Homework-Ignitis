package com.az.chatroom.services

import com.az.chatroom.dtos.MessageCreateRequest
import com.az.chatroom.dtos.MessageCursor
import com.az.chatroom.exceptions.InvalidCursorException
import com.az.chatroom.exceptions.ResourceNotFoundException
import com.az.chatroom.repositories.MessageRepository
import com.az.chatroom.repositories.projections.MessageRow
import com.az.chatroom.utils.CursorCoderUtil
import com.az.generated.jooq.tables.records.ChatMessageRecord
import org.springframework.dao.DataIntegrityViolationException
import spock.lang.Specification

import java.time.OffsetDateTime

import static com.az.chatroom.testutils.TestData.TEST_DATE_TIME
import static com.az.chatroom.testutils.TestData.TEST_MESSAGE_CONTENT
import static com.az.chatroom.testutils.TestData.TEST_MESSAGE_ID
import static com.az.chatroom.testutils.TestData.TEST_USERNAME
import static com.az.chatroom.testutils.TestData.TEST_USER_ID

class MessageServiceSpec extends Specification {

    MessageRepository messageRepository = Mock()
    MessageService messageService = new MessageService(messageRepository)

    def "should store new message and return generated id"() {
        given:
        def request = new MessageCreateRequest(TEST_MESSAGE_CONTENT)
        ChatMessageRecord savedMessage = null

        when:
        def messageId = messageService.createMessage(TEST_USER_ID, request)

        then:
        1 * messageRepository.create(_ as ChatMessageRecord) >> { ChatMessageRecord message ->
            savedMessage = message
        }

        verifyAll(savedMessage) {
            id == messageId
            userId == TEST_USER_ID
            content == TEST_MESSAGE_CONTENT
            createdAt != null
        }

        0 * _
    }

    def "should reject message from missing user"() {
        given:
        def request = new MessageCreateRequest(TEST_MESSAGE_CONTENT)

        when:
        messageService.createMessage(TEST_USER_ID, request)

        then:
        1 * messageRepository.create(_ as ChatMessageRecord) >> { throw new DataIntegrityViolationException("missing user") }
        thrown(ResourceNotFoundException)
        0 * _
    }

    def "should return messages newest first and mark deleted users anonymous"() {
        given:
        def requestedSize = 2
        def latest = messageRow(TEST_MESSAGE_ID, null, "some text from before", TEST_DATE_TIME)
        def older = messageRow(UUID.randomUUID(), TEST_USERNAME, TEST_MESSAGE_CONTENT, TEST_DATE_TIME.minusMinutes(1))
        def extra = messageRow(UUID.randomUUID(), TEST_USERNAME, "extra", TEST_DATE_TIME.minusMinutes(2))

        when:
        def response = messageService.getMessageList(null, requestedSize)

        then:
        1 * messageRepository.fetchMessages(null, null, requestedSize) >> [latest, older, extra]
        verifyAll(response) {
            size() == requestedSize
            nextCursor() != null
            messages()*.username() == ["anonymous", TEST_USERNAME]
            messages()*.content() == ["some text from before", TEST_MESSAGE_CONTENT]
        }
        0 * _
    }

    def "should request next message page from decoded cursor"() {
        given:
        def size = 50
        def cursor = CursorCoderUtil.encodeCursor(new MessageCursor(TEST_DATE_TIME, TEST_MESSAGE_ID))

        when:
        def response = messageService.getMessageList(cursor, size)

        then:
        1 * messageRepository.fetchMessages(
                { OffsetDateTime createdAt -> createdAt.toInstant() == TEST_DATE_TIME.toInstant() },
                TEST_MESSAGE_ID,
                size
        ) >> []
        response.nextCursor() == null
        0 * _
    }

    def "should reject invalid message cursor"() {
        when:
        messageService.getMessageList("not-a-valid-cursor", 50)

        then:
        thrown(InvalidCursorException)
        0 * _
    }

    private static MessageRow messageRow(UUID messageId,
                                         String username,
                                         String content,
                                         OffsetDateTime createdAt) {
        new MessageRow(messageId, username, content, createdAt)
    }
}
