package com.az.chatroom.rest

import com.az.chatroom.dtos.StatisticsResponse
import com.az.chatroom.exceptions.ResourceNotFoundException
import com.az.chatroom.services.StatisticsService
import org.spockframework.spring.SpringBean
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders
import spock.lang.Specification

import static com.az.chatroom.testutils.TestData.TEST_DATE_TIME
import static com.az.chatroom.testutils.TestData.TEST_USER_ID
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status

@WebMvcTest(StatisticsController)
@AutoConfigureMockMvc(addFilters = false)
class StatisticsControllerSpec extends Specification {

    @Autowired
    MockMvc mockMvc

    @SpringBean
    StatisticsService statisticsService = Mock()

    def "should return OK and user message statistics"() {
        when:
        def result = mockMvc.perform(MockMvcRequestBuilders.get("/api/statistics/{userId}", TEST_USER_ID))

        then:
        1 * statisticsService.getUserMessageStats(TEST_USER_ID) >>
                new StatisticsResponse(
                        "username",
                        2,
                        TEST_DATE_TIME.minusMinutes(1),
                        TEST_DATE_TIME,
                        15,
                        "last"
                )

        and:
        result.andExpect(status().isOk())
                .andExpect(jsonPath('$.username').value("username"))
                .andExpect(jsonPath('$.messageCount').value(2))
                .andExpect(jsonPath('$.lastMessageText').value("last"))
    }

    def "should return NOT FOUND when user statistics are not found"() {
        when:
        def result = mockMvc.perform(MockMvcRequestBuilders.get("/api/statistics/{userId}", TEST_USER_ID))

        then:
        1 * statisticsService.getUserMessageStats(TEST_USER_ID) >> {
            throw new ResourceNotFoundException("userId not found.")
        }

        and:
        result.andExpect(status().isNotFound())
                .andExpect(jsonPath('$.detail').value("Resource not found."))
    }
}
