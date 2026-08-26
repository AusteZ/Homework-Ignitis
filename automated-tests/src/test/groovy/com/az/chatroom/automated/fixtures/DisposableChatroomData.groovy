package com.az.chatroom.automated.fixtures

import com.az.chatroom.automated.client.ChatroomApiClient

class DisposableChatroomData {
    final String password = "password123"
    final String userRole = "USER"

    private final ChatroomApiClient client
    private final List<UUID> messageIds = []
    private final List<UUID> userIds = []

    DisposableChatroomData(ChatroomApiClient client) {
        this.client = client
    }

    UUID createUser(String adminToken, String username) {
        def userId = client.createUser(adminToken, username, password, userRole)
        userIds << userId
        userId
    }

    UUID createMessage(String userToken, String content) {
        def messageId = client.createMessage(userToken, content)
        messageIds << messageId
        messageId
    }

    void cleanup(String adminToken) {
        messageIds.reverseEach { messageId ->
            client.deleteMessageIfPossible(adminToken, messageId)
        }
        userIds.reverseEach { userId ->
            client.deleteUserIfPossible(adminToken, userId)
        }
        messageIds.clear()
        userIds.clear()
    }

    String uniqueValue(String prefix) {
        "${prefix}-${UUID.randomUUID()}"
    }
}
