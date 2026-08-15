package com.contextstt.backend.dto.auth;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

@Schema(description = "로그인 요청")
public record LoginRequest(

        @Schema(description = "이메일", example = "user@example.com")
        @NotBlank(message = "이메일은 필수입니다.")
        @Email(message = "이메일 형식이 올바르지 않습니다.")
        @Size(max = 100, message = "이메일은 100자 이하여야 합니다.")
        String email,

        @Schema(description = "비밀번호(영문, 숫자, 특수문자만 허용)", example = "password1")
        @NotBlank(message = "비밀번호는 필수입니다.")
        @Size(max = 64, message = "비밀번호는 64자 이하여야 합니다.")
        @Pattern(
                regexp = "^[\\x21-\\x7E]+$",
                message = "비밀번호는 공백 없이 영문, 숫자, 특수문자만 사용할 수 있습니다."
        )
        String password
) {
}