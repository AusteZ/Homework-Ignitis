package com.az.chatroom.dtos;

import com.az.chatroom.enums.UserRole;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record UserCreateRequest(
        @NotBlank String username,
        @NotBlank @Size(min = 8, max = 72) String password,
        @NotNull UserRole role
) {
}
