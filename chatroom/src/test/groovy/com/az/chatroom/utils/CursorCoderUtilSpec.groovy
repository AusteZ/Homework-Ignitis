package com.az.chatroom.utils

import com.az.chatroom.dtos.MessageCursor
import com.az.chatroom.exceptions.InvalidCursorException
import spock.lang.Specification

import static com.az.chatroom.testutils.TestData.TEST_DATE_TIME
import static com.az.chatroom.testutils.TestData.TEST_MESSAGE_ID

class CursorCoderUtilSpec extends Specification {
    private static final String TEST_CURSOR = "eyJjcmVhdGVkQXQiOiIyMDI2LTA4LTI0VDE4OjM3OjAwKzAzOjAwIiwibWVzc2FnZUlkIjoiMjIyMjIyMjItMjIyMi0yMjIyLTIyMjItMjIyMjIyMjIyMjIyIn0"

    def "should encode message cursor"() {
        given:
        def cursor = new MessageCursor(TEST_DATE_TIME, TEST_MESSAGE_ID)

        expect:
        CursorCoderUtil.encodeCursor(cursor) == TEST_CURSOR
    }

    def "should decode message cursor"() {
        when:
        def decoded = CursorCoderUtil.decodeCursor(TEST_CURSOR)
 
        then:
        decoded.messageId() == TEST_MESSAGE_ID
        decoded.createdAt().toInstant() == TEST_DATE_TIME.toInstant()
    }

    def "should return null cursor when encoded cursor is null or blank"() {
        expect:
        CursorCoderUtil.decodeCursor(encodedCursor) == null

        where:
        encodedCursor << [null, "", " "]
    }

    def "should reject invalid cursor"() {
        when:
        CursorCoderUtil.decodeCursor("not-a-valid-cursor")

        then:
        thrown(InvalidCursorException)
    }

    def "should reject cursor that decodes to invalid json"() {
        given:
        def encoded = Base64.urlEncoder.withoutPadding().encodeToString("not json".bytes)

        when:
        CursorCoderUtil.decodeCursor(encoded)

        then:
        thrown(InvalidCursorException)
    }
}
