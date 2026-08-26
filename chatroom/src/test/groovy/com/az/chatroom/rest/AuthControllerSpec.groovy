package com.az.chatroom.rest

import com.az.chatroom.dtos.LoginRequest
import com.az.chatroom.dtos.LoginResponse
import com.az.chatroom.enums.UserRole
import com.az.chatroom.services.AuthService
import org.spockframework.spring.SpringBean
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders
import spock.lang.Specification
import tools.jackson.databind.ObjectMapper

import java.time.Instant

import static com.az.chatroom.testutils.TestData.TEST_PASSWORD
import static com.az.chatroom.testutils.TestData.TEST_USERNAME
import static com.az.chatroom.testutils.TestData.TEST_USER_ID
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status

@WebMvcTest(AuthController)
@AutoConfigureMockMvc(addFilters = false)
class AuthControllerSpec extends Specification {

    @Autowired
    MockMvc mockMvc

    @Autowired
    ObjectMapper objectMapper

    @SpringBean
    AuthService authService = Mock()

    def "should return token response for valid credentials"() {
        given:
        def requestJson = objectMapper.writeValueAsString(new LoginRequest(TEST_USERNAME, TEST_PASSWORD))

        when:
        def result = mockMvc.perform(MockMvcRequestBuilders.post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestJson))

        then:
        1 * authService.login({ request ->
            request.username() == TEST_USERNAME && request.password() == TEST_PASSWORD
        }) >> new LoginResponse("token-value", Instant.now().plusSeconds(3600), TEST_USER_ID, TEST_USERNAME, UserRole.USER)

        result.andExpect(status().isOk())
                .andExpect(jsonPath('$.token').value("token-value"))
                .andExpect(jsonPath('$.userId').value(TEST_USER_ID.toString()))
                .andExpect(jsonPath('$.username').value(TEST_USERNAME))
                .andExpect(jsonPath('$.role').value(UserRole.USER.name()))
    }

}
