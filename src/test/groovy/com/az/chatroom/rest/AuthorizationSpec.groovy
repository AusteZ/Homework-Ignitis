package com.az.chatroom.rest

import com.az.chatroom.config.SecurityConfig
import com.az.chatroom.dtos.LoginRequest
import com.az.chatroom.dtos.LoginResponse
import com.az.chatroom.dtos.MessageCreateRequest
import com.az.chatroom.dtos.MessagePageResponse
import com.az.chatroom.dtos.StatisticsResponse
import com.az.chatroom.dtos.UserPageResponse
import com.az.chatroom.services.AuthService
import com.az.chatroom.services.MessageService
import com.az.chatroom.services.StatisticsService
import com.az.chatroom.services.UserService
import com.az.chatroom.utils.JwtCustomClaim
import org.spockframework.spring.SpringBean
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest
import org.springframework.context.annotation.Import
import org.springframework.http.MediaType
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders
import spock.lang.Specification
import tools.jackson.databind.ObjectMapper

import java.time.Instant

import static com.az.chatroom.enums.UserRole.ADMIN
import static com.az.chatroom.enums.UserRole.USER
import static com.az.chatroom.testutils.TestData.TEST_DATE_TIME
import static com.az.chatroom.testutils.TestData.TEST_MESSAGE_CONTENT
import static com.az.chatroom.testutils.TestData.TEST_MESSAGE_ID
import static com.az.chatroom.testutils.TestData.TEST_PASSWORD
import static com.az.chatroom.testutils.TestData.TEST_USERNAME
import static com.az.chatroom.testutils.TestData.TEST_USER_ID
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status

@WebMvcTest([AuthController, MessageController, UserController, StatisticsController])
@AutoConfigureMockMvc
@Import(SecurityConfig)
class AuthorizationSpec extends Specification {

    @Autowired
    MockMvc mockMvc

    @Autowired
    ObjectMapper objectMapper

    @SpringBean
    AuthService authService = Mock()

    @SpringBean
    MessageService messageService = Mock()

    @SpringBean
    UserService userService = Mock()

    @SpringBean
    StatisticsService statisticsService = Mock()

    def "should allow login without token"() {
        given:
        def requestJson = objectMapper.writeValueAsString(new LoginRequest(TEST_USERNAME, TEST_PASSWORD))

        when:
        def result = mockMvc.perform(MockMvcRequestBuilders.post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestJson))

        then:
        1 * authService.login(_) >> new LoginResponse(
                "token-value",
                Instant.now().plusSeconds(3600),
                TEST_USER_ID,
                TEST_USERNAME,
                USER
        )
        result.andExpect(status().isOk())
    }

    def "should reject anonymous access to protected endpoints"() {
        when:
        def result = mockMvc.perform(MockMvcRequestBuilders.get("/api/messages"))

        then:
        0 * messageService._
        result.andExpect(status().isUnauthorized())
    }

    def "should allow user access to non-admin api endpoints"() {
        given:
        def requestJson = objectMapper.writeValueAsString(new MessageCreateRequest(TEST_MESSAGE_CONTENT))

        when:
        def readResult = mockMvc.perform(MockMvcRequestBuilders.get("/api/messages")
                .with(userJwt()))
        def writeResult = mockMvc.perform(MockMvcRequestBuilders.post("/api/messages")
                .with(userJwt())
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestJson))

        then:
        1 * messageService.getMessageList(null, 100) >> new MessagePageResponse([], 0, null)
        1 * messageService.createMessage(TEST_USER_ID, _) >> TEST_MESSAGE_ID

        readResult.andExpect(status().isOk())
        writeResult.andExpect(status().isCreated())
    }

    def "should reject user access to admin api endpoints"() {
        when:
        def result = mockMvc.perform(MockMvcRequestBuilders.get("/api/admin/users")
                .with(userJwt()))

        then:
        0 * userService._
        result.andExpect(status().isForbidden())
    }

    def "should allow admin access to admin api endpoints"() {
        when:
        def usersResult = mockMvc.perform(MockMvcRequestBuilders.get("/api/admin/users")
                .with(adminJwt()))
        def statisticsResult = mockMvc.perform(MockMvcRequestBuilders.get("/api/admin/statistics/{userId}", TEST_USER_ID)
                .with(adminJwt()))

        then:
        1 * userService.getUserList(0, 50) >> new UserPageResponse([], 0, 0, false)
        1 * statisticsService.getUserMessageStats(TEST_USER_ID) >> new StatisticsResponse(
                TEST_USERNAME,
                0,
                TEST_DATE_TIME,
                TEST_DATE_TIME,
                0,
                ""
        )

        usersResult.andExpect(status().isOk())
        statisticsResult.andExpect(status().isOk())
    }

    private static def userJwt() {
        authenticatedJwt(USER)
    }

    private static def adminJwt() {
        authenticatedJwt(ADMIN)
    }

    private static def authenticatedJwt(role) {
        jwt()
                .jwt { token ->
                    token.claim(JwtCustomClaim.USER_ID, TEST_USER_ID.toString())
                            .claim(JwtCustomClaim.ROLE, role.name())
                }
                .authorities(new SimpleGrantedAuthority(role.authority()))
    }
}
