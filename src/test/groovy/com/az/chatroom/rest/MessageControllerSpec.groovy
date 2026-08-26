package com.az.chatroom.rest

import com.az.chatroom.dtos.MessageCreateRequest
import com.az.chatroom.dtos.MessagePageResponse
import com.az.chatroom.dtos.MessageResponse
import com.az.chatroom.exceptions.InvalidCursorException
import com.az.chatroom.services.MessageService
import org.spockframework.spring.SpringBean
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders
import spock.lang.Specification
import tools.jackson.databind.ObjectMapper

import static com.az.chatroom.testutils.TestData.TEST_DATE_TIME
import static com.az.chatroom.testutils.TestData.TEST_MESSAGE_CONTENT
import static com.az.chatroom.testutils.TestData.TEST_MESSAGE_ID
import static com.az.chatroom.testutils.TestData.TEST_USERNAME
import static com.az.chatroom.testutils.TestData.TEST_USER_ID
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status

@WebMvcTest(MessageController)
@AutoConfigureMockMvc(addFilters = false)
class MessageControllerSpec extends Specification {
    private static String API_PATH = "/api/messages"

    @Autowired
    MockMvc mockMvc

    @Autowired
    ObjectMapper objectMapper

    @SpringBean
    MessageService messageService = Mock()

    def "should return CREATED with created message id"() {
        given:
        def messageRequest = new MessageCreateRequest(TEST_USER_ID, TEST_MESSAGE_CONTENT)
        def messageRequestJson = objectMapper.writeValueAsString(messageRequest)

        when:
        def result = mockMvc.perform(MockMvcRequestBuilders.post(API_PATH)
                .contentType(MediaType.APPLICATION_JSON)
                .content(messageRequestJson))

        then:
        1 * messageService.createMessage({ request ->
            request.userId() == TEST_USER_ID && request.content() == TEST_MESSAGE_CONTENT
        }) >> TEST_MESSAGE_ID

        result.andExpect(status().isCreated())
                .andExpect(content().string("\"${TEST_MESSAGE_ID}\""))
    }

    def "should return BAD REQUEST when message content is blank"() {
        given:
        def messageRequestJson = objectMapper.writeValueAsString(new MessageCreateRequest(TEST_USER_ID, " "))

        when:
        def result = mockMvc.perform(MockMvcRequestBuilders.post(API_PATH)
                .contentType(MediaType.APPLICATION_JSON)
                .content(messageRequestJson))

        then:
        0 * messageService._

        result.andExpect(status().isBadRequest())
    }

    def "should return OK and a page of messages with requested size"() {
        given:
        def cursor = "next-cursor"
        def requestedSize = 25
        def response = new MessagePageResponse(
                [new MessageResponse(TEST_USERNAME, TEST_MESSAGE_CONTENT, TEST_DATE_TIME)],
                1,
                cursor
        )

        when:
        def result = mockMvc.perform(MockMvcRequestBuilders.get(API_PATH)
                .param("size", requestedSize.toString()))

        then:
        1 * messageService.getMessageList(null, requestedSize) >> response

        result.andExpect(status().isOk())
                .andExpect(jsonPath('$.size').value(1))
                .andExpect(jsonPath('$.nextCursor').value(cursor))
                .andExpect(jsonPath('$.messages[0].messageId').doesNotExist())
                .andExpect(jsonPath('$.messages[0].userId').doesNotExist())
                .andExpect(jsonPath('$.messages[0].username').value(TEST_USERNAME))
                .andExpect(jsonPath('$.messages[0].content').value(TEST_MESSAGE_CONTENT))
    }

    def "should return BAD REQUEST when requested size is above maximum"() {
        when:
        def result = mockMvc.perform(MockMvcRequestBuilders.get(API_PATH)
                .param("size", "501"))

        then:
        0 * messageService._

        result.andExpect(status().isBadRequest())
    }

    def "should return BAD REQUEST when cursor is invalid"() {
        given:
        def cursor = "invalid-cursor"

        when:
        def result = mockMvc.perform(MockMvcRequestBuilders.get(API_PATH)
                .param("cursor", cursor))

        then:
        1 * messageService.getMessageList(cursor, 100) >> {
            throw new InvalidCursorException("Invalid message cursor.", new RuntimeException("decode failed"))
        }

        result.andExpect(status().isBadRequest())
                .andExpect(jsonPath('$.detail').value("Invalid cursor."))
    }

    def "should return INTERNAL SERVER ERROR when unexpected exception occurs"() {
        when:
        def result = mockMvc.perform(MockMvcRequestBuilders.get(API_PATH))

        then:
        1 * messageService.getMessageList(null, 100) >> {
            throw new RuntimeException("boom")
        }

        result.andExpect(status().isInternalServerError())
    }
}
