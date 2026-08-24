package com.az.chatroom.services

import com.az.chatroom.dtos.UserCreateRequest
import com.az.chatroom.exceptions.ResourceNotFoundException
import com.az.chatroom.exceptions.UserAlreadyExistsException
import com.az.chatroom.repositories.UserRepository
import com.az.generated.jooq.tables.records.AppUserRecord
import org.springframework.dao.DuplicateKeyException
import spock.lang.Specification

import static com.az.chatroom.testutils.TestData.TEST_DATE_TIME
import static com.az.chatroom.testutils.TestData.TEST_ROLE
import static com.az.chatroom.testutils.TestData.TEST_USERNAME
import static com.az.chatroom.testutils.TestData.TEST_USER_ID
import static com.az.chatroom.testutils.UserTestHelper.userRecord

class UserServiceSpec extends Specification {

    UserRepository userRepository = Mock()
    UserService userService = new UserService(userRepository)

    def "should store new user and return their generated id"() {
        given:
        def request = new UserCreateRequest(TEST_USERNAME, TEST_ROLE)
        AppUserRecord savedUser = null

        when:
        def userId = userService.createUser(request)

        then:
        1 * userRepository.create(_ as AppUserRecord) >> { AppUserRecord user ->
            savedUser = user
        }

        verifyAll(savedUser) {
            id == userId
            username == TEST_USERNAME
            role == TEST_ROLE.name()
            createdAt != null
        }

        0 * _
    }

    def "should translate duplicate username into domain exception"() {
        given:
        def request = new UserCreateRequest(TEST_USERNAME, TEST_ROLE)

        when:
        userService.createUser(request)

        then:
        1 * userRepository.create(_ as AppUserRecord) >> { throw new DuplicateKeyException("duplicate username") }
        def exception = thrown(UserAlreadyExistsException)
        exception.message == "Username %s already exists.".formatted(TEST_USERNAME)
        0 * _
    }

    def "should remove existing user"() {
        when:
        userService.deleteUser(TEST_USER_ID)

        then:
        1 * userRepository.delete(TEST_USER_ID) >> true
        noExceptionThrown()
        0 * _
    }

    def "should translate user not found on deletion into exception"() {
        when:
        userService.deleteUser(TEST_USER_ID)

        then:
        1 * userRepository.delete(TEST_USER_ID) >> false
        thrown(ResourceNotFoundException)
        0 * _
    }

    def "should find specific user"() {
        given:
        def record = userRecord()

        when:
        def response = userService.getUser(TEST_USER_ID)

        then:
        1 * userRepository.findById(TEST_USER_ID) >> Optional.of(record)
        verifyAll(response) {
            userId() == TEST_USER_ID
            username() == TEST_USERNAME
            role() == TEST_ROLE
            createdAt().toInstant() == TEST_DATE_TIME.toInstant()
        }
        0 * _
    }

    def "should throw when user is not found"() {
        when:
        userService.getUser(TEST_USER_ID)

        then:
        1 * userRepository.findById(TEST_USER_ID) >> Optional.empty()
        thrown(ResourceNotFoundException)
        0 * _
    }

    def "should return page of users and detect next page"() {
        given:
        def page = 0
        def size = 2
        def first = userRecord()
        def second = userRecord(UUID.randomUUID(), "second", TEST_DATE_TIME.minusMinutes(1))
        def extra = userRecord(UUID.randomUUID(), "extra", TEST_DATE_TIME.minusMinutes(2))

        when:
        def response = userService.getUserList(page, size)

        then:
        1 * userRepository.fetchUsers(page, size) >> [first, second, extra]
        response.page() == page
        response.size() == size
        response.hasNext()
        response.users()*.username() == [TEST_USERNAME, "second"]
        0 * _
    }
}
