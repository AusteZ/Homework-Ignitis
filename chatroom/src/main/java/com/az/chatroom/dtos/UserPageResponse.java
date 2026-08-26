package com.az.chatroom.dtos;

import java.util.List;

public record UserPageResponse(
        List<UserResponse> users,
        int page,
        int size,
        boolean hasNext
) {
}
