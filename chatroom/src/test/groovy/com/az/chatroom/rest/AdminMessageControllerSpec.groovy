package com.az.chatroom.rest

import com.az.chatroom.config.SecurityConfig
import com.az.chatroom.exceptions.ResourceNotFoundException
import com.az.chatroom.services.MessageService
import com.az.chatroom.utils.JwtCustomClaim
import org.spockframework.spring.SpringBean
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest
import org.springframework.context.annotation.Import
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders
import spock.lang.Specification

import static com.az.chatroom.enums.UserRole.ADMIN
import static com.az.chatroom.testutils.TestData.TEST_MESSAGE_ID
import static com.az.chatroom.testutils.TestData.TEST_USER_ID
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status

@WebMvcTest(AdminMessageController)
@AutoConfigureMockMvc
@Import(SecurityConfig)
class AdminMessageControllerSpec extends Specification {
    private static final String API_PATH = "/api/admin/messages"

    @Autowired
    MockMvc mockMvc

    @SpringBean
    MessageService messageService = Mock()

    def "should return NO CONTENT when admin deletes message"() {
        when:
        def result = mockMvc.perform(MockMvcRequestBuilders.delete("${API_PATH}/{messageId}", TEST_MESSAGE_ID)
                .with(adminJwt()))

        then:
        1 * messageService.deleteMessage(TEST_MESSAGE_ID)

        result.andExpect(status().isNoContent())
    }

    def "should return NOT FOUND when message does not exist"() {
        when:
        def result = mockMvc.perform(MockMvcRequestBuilders.delete("${API_PATH}/{messageId}", TEST_MESSAGE_ID)
                .with(adminJwt()))

        then:
        1 * messageService.deleteMessage(TEST_MESSAGE_ID) >> {
            throw new ResourceNotFoundException("Message not found.")
        }

        result.andExpect(status().isNotFound())
                .andExpect(jsonPath('$.detail').value("Message not found."))
    }

    private static def adminJwt() {
        jwt()
                .jwt { token ->
                    token.claim(JwtCustomClaim.USER_ID, TEST_USER_ID.toString())
                            .claim(JwtCustomClaim.ROLE, ADMIN.name())
                }
                .authorities(new SimpleGrantedAuthority(ADMIN.authority()))
    }
}
