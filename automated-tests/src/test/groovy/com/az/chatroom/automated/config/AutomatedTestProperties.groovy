package com.az.chatroom.automated.config

import jakarta.validation.constraints.NotBlank
import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.validation.annotation.Validated

@Validated
@ConfigurationProperties(prefix = "automated-test")
class AutomatedTestProperties {
    @NotBlank(message = "automated-test.base-url must point to the deployed chatroom service")
    String baseUrl = ""

    @NotBlank(message = "automated-test.admin-username must contain an admin user for automated API tests")
    String adminUsername = ""

    @NotBlank(message = "automated-test.admin-password must contain the admin password for automated API tests")
    String adminPassword = ""
}
