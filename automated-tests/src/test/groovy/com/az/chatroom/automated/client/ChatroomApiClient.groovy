package com.az.chatroom.automated.client

import com.az.chatroom.automated.config.AutomatedTestProperties
import io.restassured.RestAssured
import io.restassured.builder.RequestSpecBuilder
import io.restassured.http.ContentType
import io.restassured.response.Response
import io.restassured.specification.RequestSpecification
import org.slf4j.Logger
import org.slf4j.LoggerFactory

class ChatroomApiClient {
    private static final Logger LOGGER = LoggerFactory.getLogger(ChatroomApiClient)

    private final String loginPath = "/api/auth/login"
    private final String messagesPath = "/api/messages"
    private final String adminUsersPath = "/api/admin/users"
    private final String adminMessagesPath = "/api/admin/messages"

    private final RequestSpecification baseRequest

    ChatroomApiClient(AutomatedTestProperties properties) {
        baseRequest = new RequestSpecBuilder()
                .setBaseUri(properties.baseUrl.replaceAll('/+$', ''))
                .setAccept(ContentType.JSON)
                .setContentType(ContentType.JSON)
                .build()
    }

    String login(String username, String password) {
        def response = anonymousJsonRequest()
                .body([
                        username: username,
                        password: password
                ])
                .when()
                .post(loginPath)

        expectStatus(response, 200, "Log in as '${username}'")
        response.path("token")
    }

    UUID createUser(String adminToken, String username, String password, String role) {
        def response = authenticatedJsonRequest(adminToken)
                .body([
                        username: username,
                        password: password,
                        role    : role
                ])
                .when()
                .post(adminUsersPath)

        expectStatus(response, 201, "Create disposable user '${username}'")
        UUID.fromString(unquote(response.asString()))
    }

    Response deleteUser(String adminToken, UUID userId) {
        authenticatedJsonRequest(adminToken)
                .when()
                .delete("${adminUsersPath}/${userId}")
    }

    UUID createMessage(String userToken, String content) {
        def response = authenticatedJsonRequest(userToken)
                .body([content: content])
                .when()
                .post(messagesPath)

        expectStatus(response, 201, "Create disposable message")
        UUID.fromString(unquote(response.asString()))
    }

    Map getMessages(String token, int size) {
        def response = authenticatedJsonRequest(token)
                .queryParam("size", size)
                .when()
                .get(messagesPath)

        expectStatus(response, 200, "Fetch latest messages page with size ${size}")
        response.as(Map)
    }

    void deleteMessageIfPossible(String adminToken, UUID messageId) {
        deleteIfPossible(adminToken, "${adminMessagesPath}/${messageId}")
    }

    void deleteUserIfPossible(String adminToken, UUID userId) {
        deleteIfPossible(adminToken, "${adminUsersPath}/${userId}")
    }

    private RequestSpecification anonymousJsonRequest() {
        RestAssured.given(baseRequest)
    }

    private RequestSpecification authenticatedJsonRequest(String token) {
        anonymousJsonRequest()
                .auth()
                .oauth2(token)
    }

    private void deleteIfPossible(String adminToken, String path) {
        if (!adminToken) {
            return
        }

        try {
            def response = authenticatedJsonRequest(adminToken)
                    .when()
                    .delete(path)
            if (!(response.statusCode() in [204, 404])) {
                LOGGER.warn(
                        "Cleanup request did not complete cleanly. [path={}, status={}, body={}]",
                        path,
                        response.statusCode(),
                        response.asString()
                )
            }
        } catch (RuntimeException ignored) {
            LOGGER.warn("Cleanup request failed. [path={}]", path, ignored)
        }
    }

    private String unquote(String value) {
        value.replace('"', '')
    }

    private void expectStatus(Response response, int expectedStatus, String operation) {
        assert response.statusCode() == expectedStatus:
                "${operation} failed. Expected HTTP ${expectedStatus}, got HTTP ${response.statusCode()}. Response body: ${response.asString()}"
    }
}
