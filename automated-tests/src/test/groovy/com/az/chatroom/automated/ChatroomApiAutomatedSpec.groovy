package com.az.chatroom.automated

import com.az.chatroom.automated.client.ChatroomApiClient
import com.az.chatroom.automated.config.AutomatedTestConfiguration
import com.az.chatroom.automated.config.AutomatedTestProperties
import com.az.chatroom.automated.fixtures.DisposableChatroomData
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import spock.lang.Specification

@SpringBootTest(classes = AutomatedTestConfiguration)
class ChatroomApiAutomatedSpec extends Specification {
    private final int singleMessagePageSize = 1
    private final int anonymizationLookupPageSize = 100
    private final String anonymousUsername = "anonymous"
    private final String authTestUsernamePrefix = "automated-auth"
    private final String anonymizationTestUsernamePrefix = "automated-anonymization"
    private final String authTestMessagePrefix = "automated auth message"
    private final String anonymizationTestMessagePrefix = "automated anonymization message"

    @Autowired
    AutomatedTestProperties properties

    @Autowired
    ChatroomApiClient api

    private String adminToken
    private DisposableChatroomData disposableData

    def setup() {
        adminToken = api.login(properties.adminUsername, properties.adminPassword)
        disposableData = new DisposableChatroomData(api)
    }

    def cleanup() {
        disposableData?.cleanup(adminToken)
    }

    def "user should login, post a message and then read latest page of messages"() {
        given:
        def username = disposableData.uniqueValue(authTestUsernamePrefix)
        disposableData.createUser(adminToken, username)
        def message = disposableData.uniqueValue(authTestMessagePrefix)

        when:
        def userToken = api.login(username, disposableData.password)
        def messageId = disposableData.createMessage(userToken, message)
        def messagesPage = api.getMessages(userToken, singleMessagePageSize)

        then:
        assert messageId != null: "Expected message creation to return a message id."
        assert messagesPage.size > 0: "Expected messages page size to be positive after creating a message."
        assert !messagesPage.messages.empty: "Expected at least one message after creating a disposable message."
    }

    def "admin should delete a user and anonymize that user's messages"() {
        given:
        def username = disposableData.uniqueValue(anonymizationTestUsernamePrefix)
        def userId = disposableData.createUser(adminToken, username)
        def userToken = api.login(username, disposableData.password)
        def message = disposableData.uniqueValue(anonymizationTestMessagePrefix)

        and:
        disposableData.createMessage(userToken, message)

        when:
        def deleteResponse = api.deleteUser(adminToken, userId)
        def messagesPage = api.getMessages(adminToken, anonymizationLookupPageSize)
        def anonymizedMessage = messagesPage.messages.find { it.content == message }

        then:
        assert deleteResponse.statusCode() == 204:
                "Expected deleting disposable user '${username}' to return HTTP 204, got HTTP ${deleteResponse.statusCode()}."
        assert anonymizedMessage != null:
                "Expected to find the disposable user's message after deleting the user."
        assert anonymizedMessage.username == anonymousUsername:
                "Expected deleted user's message to be shown as '${anonymousUsername}', got '${anonymizedMessage.username}'."
    }
}
