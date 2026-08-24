package com.az.chatroom.repositories

import com.az.chatroom.testutils.UserTestHelper
import com.az.generated.jooq.tables.records.AppUserRecord
import org.jooq.DSLContext
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.transaction.annotation.Transactional
import spock.lang.Specification

import java.time.OffsetDateTime

import static com.az.chatroom.testutils.TestData.TEST_DATE_TIME
import static com.az.chatroom.testutils.TestData.TEST_ROLE
import static com.az.chatroom.testutils.TestData.TEST_USERNAME
import static com.az.chatroom.testutils.TestData.TEST_USER_ID
import static com.az.generated.jooq.Tables.APP_USER

@SpringBootTest
@Transactional
class UserRepositorySpec extends Specification {

    private static final String TEST_ROLE_STRING = TEST_ROLE.name()

    @Autowired
    UserRepository userRepository

    @Autowired
    DSLContext dsl

    def "should create new user"() {
        given:
        def user = new AppUserRecord(TEST_USER_ID, TEST_USERNAME, TEST_ROLE_STRING, TEST_DATE_TIME)

        when:
        userRepository.create(user)

        then:
        def savedUser = dsl.selectFrom(APP_USER)
                .where(APP_USER.ID.eq(TEST_USER_ID))
                .fetchOne()
        verifyAll(savedUser) {
            id == TEST_USER_ID
            username == TEST_USERNAME
            role == TEST_ROLE_STRING
            createdAt.toInstant() == TEST_DATE_TIME.toInstant()
        }
    }

    def "should delete existing user"() {
        given:
        insertUser()

        when:
        def deleted = userRepository.delete(TEST_USER_ID)

        then:
        deleted
        !dsl.fetchExists(
                APP_USER,
                APP_USER.ID.eq(TEST_USER_ID)
        )
    }

    def "should return false when deleting non-existing user"() {
        expect:
        !userRepository.delete(TEST_USER_ID)
    }

    def "should find specific user"() {
        given:
        insertUser()

        when:
        def result = userRepository.findById(TEST_USER_ID)

        then:
        result.present

        verifyAll(result.get()) {
            id == TEST_USER_ID
            username == TEST_USERNAME
            role == TEST_ROLE_STRING
            createdAt.toInstant() == TEST_DATE_TIME.toInstant()
        }
    }

    def "should fetch one extra user in expected order for next page detection"() {
        given:
        def page = 0
        def size = 2
        insertUser()
        def secondUserId = UUID.randomUUID()
        insertUser(secondUserId, "second", TEST_DATE_TIME.minusMinutes(1))
        def thirdUserId = UUID.randomUUID()
        insertUser(thirdUserId, "third", TEST_DATE_TIME.minusMinutes(2))
        insertUser(UUID.randomUUID(), "notFetched", TEST_DATE_TIME.minusMinutes(3))

        when:
        def users = userRepository.fetchUsers(page, size)

        then:
        users.size() == size + 1
        users*.id == [TEST_USER_ID, secondUserId, thirdUserId]
    }

    private void insertUser(UUID userId = TEST_USER_ID,
                            String username = TEST_USERNAME,
                            OffsetDateTime createdAt = TEST_DATE_TIME) {
        dsl.insertInto(APP_USER)
                .set(UserTestHelper.userRecord(userId, username, createdAt))
                .execute()
    }
}
