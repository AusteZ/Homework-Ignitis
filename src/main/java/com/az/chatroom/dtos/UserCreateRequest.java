package com.az.chatroom.dtos;

import com.az.chatroom.enums.UserRole;
import jakarta.validation.constraints.NotBlank;

public record UserCreateRequest(
        @NotBlank String username,
        UserRole role
) {
}
