package com.contextstt.backend.dto.user;

import com.contextstt.backend.domain.user.User;
import lombok.Builder;

@Builder
public record UserResponse(
        Long id,
        String email,
        String nickname
) {
    public static UserResponse from(User user) {
        return UserResponse.builder()
                .id(user.getId())
                .email(user.getEmail())
                .nickname(user.getNickname())
                .build();
    }
}
