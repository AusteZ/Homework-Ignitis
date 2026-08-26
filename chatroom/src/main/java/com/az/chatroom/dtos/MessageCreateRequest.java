package com.az.chatroom.dtos;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record MessageCreateRequest(
        @NotBlank @Size(max = 1000) String content
) {
}
