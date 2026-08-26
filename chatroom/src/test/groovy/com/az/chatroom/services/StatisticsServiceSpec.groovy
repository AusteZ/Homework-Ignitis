package com.az.chatroom.services

import com.az.chatroom.exceptions.ResourceNotFoundException
import com.az.chatroom.repositories.MessageRepository
import com.az.chatroom.repositories.projections.StatisticsRow
import spock.lang.Specification

import static com.az.chatroom.testutils.TestData.TEST_DATE_TIME
import static com.az.chatroom.testutils.TestData.TEST_USERNAME
import static com.az.chatroom.testutils.TestData.TEST_USER_ID

class StatisticsServiceSpec extends Specification {

    MessageRepository messageRepository = Mock()
    StatisticsService statisticsService = new StatisticsService(messageRepository)

    def "should return user message statistics from repository aggregate"() {
        given:
        def row = new StatisticsRow(
                TEST_USERNAME,
                2,
                TEST_DATE_TIME.minusMinutes(2),
                TEST_DATE_TIME,
                4,
                "hello"
        )

        when:
        def stats = statisticsService.getUserMessageStats(TEST_USER_ID)

        then:
        1 * messageRepository.fetchUserMessageStats(TEST_USER_ID) >> Optional.of(row)
        verifyAll(stats) {
            username() == TEST_USERNAME
            messageCount() == 2
            firstMessageAt() == row.firstMessageAt()
            lastMessageAt() == row.lastMessageAt()
            averageMessageLength() == 4
            lastMessageText() == "hello"
        }
        0 * _
    }

    def "should throw when user message statistics are not found"() {
        when:
        statisticsService.getUserMessageStats(TEST_USER_ID)

        then:
        1 * messageRepository.fetchUserMessageStats(TEST_USER_ID) >> Optional.empty()
        def exception = thrown(ResourceNotFoundException)
        exception.message == "User statistics not found."
        0 * _
    }
}
