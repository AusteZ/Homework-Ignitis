package com.az.chatroom.automated.config

import com.az.chatroom.automated.client.ChatroomApiClient
import org.springframework.boot.SpringBootConfiguration
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Bean

@SpringBootConfiguration
@EnableConfigurationProperties(AutomatedTestProperties)
class AutomatedTestConfiguration {

    @Bean
    ChatroomApiClient chatroomApiClient(AutomatedTestProperties properties) {
        new ChatroomApiClient(properties)
    }
}
