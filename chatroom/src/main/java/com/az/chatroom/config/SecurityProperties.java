package com.az.chatroom.config;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import java.time.Duration;

@Validated
@ConfigurationProperties(prefix = "app.security")
public record SecurityProperties(
        @Valid @NotNull Jwt jwt,
        @Valid @NotNull BootstrapAdmin bootstrapAdmin
) {
    public record Jwt(
            @NotBlank @Size(min = 32) String secret,
            @NotNull Duration expiresIn
    ) {
    }

    public record BootstrapAdmin(
            @NotBlank String username,
            @NotBlank @Size(min = 8, max = 72) String password
    ) {
    }
}
