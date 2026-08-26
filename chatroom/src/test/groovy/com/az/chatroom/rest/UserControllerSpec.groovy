package com.az.chatroom.rest

import com.az.chatroom.dtos.UserCreateRequest
import com.az.chatroom.dtos.UserPageResponse
import com.az.chatroom.dtos.UserResponse
import com.az.chatroom.exceptions.ResourceAlreadyExistsException
import com.az.chatroom.exceptions.ResourceNotFoundException
import com.az.chatroom.services.UserService
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
import static com.az.chatroom.testutils.TestData.TEST_PASSWORD
import static com.az.chatroom.testutils.TestData.TEST_ROLE
import static com.az.chatroom.testutils.TestData.TEST_USERNAME
import static com.az.chatroom.testutils.TestData.TEST_USER_ID
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status

@WebMvcTest(UserController)
@AutoConfigureMockMvc(addFilters = false)
class UserControllerSpec extends Specification {
    private static final String API_PATH = "/api/admin/users"

    @Autowired
    MockMvc mockMvc

    @Autowired
    ObjectMapper objectMapper

    @SpringBean
    UserService userService = Mock()

    def "should return CREATED with created user id"() {
        given:
        def userRequest = new UserCreateRequest(TEST_USERNAME, TEST_PASSWORD, TEST_ROLE)
        def userRequestJson = objectMapper.writeValueAsString(userRequest)

        when:
        def result = mockMvc.perform(MockMvcRequestBuilders.post(API_PATH)
                .contentType(MediaType.APPLICATION_JSON)
                .content(userRequestJson))

        then:
        1 * userService.createUser({ request ->
            request.username() == TEST_USERNAME && request.password() == TEST_PASSWORD && request.role() == TEST_ROLE
        }) >> TEST_USER_ID

        and:
        result.andExpect(status().isCreated())
                .andExpect(content().string("\"${TEST_USER_ID}\""))
    }

    def "should return CONFLICT when username already exists"() {
        given:
        def userRequest = new UserCreateRequest(TEST_USERNAME, TEST_PASSWORD, TEST_ROLE)
        def userRequestJson = objectMapper.writeValueAsString(userRequest)

        when:
        def result = mockMvc.perform(MockMvcRequestBuilders.post(API_PATH)
                .contentType(MediaType.APPLICATION_JSON)
                .content(userRequestJson))

        then:
        1 * userService.createUser(_) >> { throw new ResourceAlreadyExistsException("User already exists.") }

        and:
        result.andExpect(status().isConflict())
                .andExpect(jsonPath('$.detail').value("User already exists."))
    }

    def "should return NO CONTENT after deleting user"() {
        when:
        def result = mockMvc.perform(MockMvcRequestBuilders.delete("${API_PATH}/{userId}", TEST_USER_ID))

        then:
        1 * userService.deleteUser(TEST_USER_ID)

        and:
        result.andExpect(status().isNoContent())
    }

    def "should return OK and found user"() {
        when:
        def result = mockMvc.perform(MockMvcRequestBuilders.get("${API_PATH}/{userId}", TEST_USER_ID))

        then:
        1 * userService.getUser(TEST_USER_ID) >> new UserResponse(TEST_USER_ID, TEST_USERNAME, TEST_ROLE, TEST_DATE_TIME)

        and:
        result.andExpect(status().isOk())
                .andExpect(jsonPath('$.userId').value(TEST_USER_ID.toString()))
                .andExpect(jsonPath('$.username').value(TEST_USERNAME))
                .andExpect(jsonPath('$.role').value(TEST_ROLE.name()))
    }

    def "should return NOT FOUND when user not found"() {
        when:
        def result = mockMvc.perform(MockMvcRequestBuilders.get("${API_PATH}/{userId}", TEST_USER_ID))

        then:
        1 * userService.getUser(TEST_USER_ID) >> { throw new ResourceNotFoundException("User not found.") }

        and:
        result.andExpect(status().isNotFound())
                .andExpect(jsonPath('$.detail').value("User not found."))
    }

    def "should return OK and a page of users"() {
        given:
        def page = 1
        def givenSize = 25
        def response = [new UserResponse(TEST_USER_ID, TEST_USERNAME, TEST_ROLE, TEST_DATE_TIME)]

        when:
        def result = mockMvc.perform(MockMvcRequestBuilders.get(API_PATH)
                .param("page", page.toString())
                .param("size", givenSize.toString()))

        then:

        1 * userService.getUserList(page, givenSize) >> new UserPageResponse(
                response,
                page,
                response.size(),
                false
        )

        and:
        result.andExpect(status().isOk())
                .andExpect(jsonPath('$.page').value(page))
                .andExpect(jsonPath('$.size').value(response.size()))
                .andExpect(jsonPath('$.hasNext').value(false))
                .andExpect(jsonPath('$.users[0].username').value(TEST_USERNAME))
    }
}
