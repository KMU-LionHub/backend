package com.contextstt.backend.dto.auth;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

@Schema(description = "회원가입 요청")
public record SignupRequest(

        @Schema(description = "이메일", example = "user@example.com")
        @NotBlank(message = "이메일은 필수입니다.")
        @Email(message = "이메일 형식이 올바르지 않습니다.")
        @Size(max = 100, message = "이메일은 100자 이하여야 합니다.")
        String email,

        @Schema(description = "비밀번호(8~64자, 영문·숫자 필수, 특수문자 허용)", example = "password1")
        @NotBlank(message = "비밀번호는 필수입니다.")
        @Size(min = 8, max = 64, message = "비밀번호는 8자 이상 64자 이하이어야 합니다.")
        @Pattern.List({
                @Pattern(
                        regexp = "^[\\x21-\\x7E]+$",
                        message = "비밀번호는 공백 없이 영문, 숫자, 특수문자만 사용할 수 있습니다."
                ),
                @Pattern(
                        regexp = "^(?=.*[A-Za-z])(?=.*\\d).+$",
                        message = "비밀번호는 영문과 숫자를 포함해야 합니다."
                )
        })
        String password,

        @Schema(description = "닉네임", example = "사용자")
        @NotBlank(message = "닉네임은 필수입니다.")
        @Size(min = 2, max = 20, message = "닉네임은 2자 이상 20자 이하이어야 합니다.")
        String nickname
) {
}