package com.contextstt.backend.service;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import com.contextstt.backend.domain.user.User;
import com.contextstt.backend.domain.user.UserRepository;
import com.contextstt.backend.dto.auth.SignupRequest;
import com.contextstt.backend.exception.DuplicateEmailException;
import com.contextstt.backend.security.JwtTokenProvider;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtTokenProvider jwtTokenProvider;

    @InjectMocks
    private AuthService authService;

    @Test
    void signupMapsDatabaseUniqueConstraintViolationToDuplicateEmail() {
        SignupRequest request = new SignupRequest(
                "duplicate@contextstt.com",
                "password1",
                "테스터"
        );
        DataIntegrityViolationException databaseException =
                new DataIntegrityViolationException("unique email constraint");

        when(passwordEncoder.encode(request.password())).thenReturn("encoded-password");
        when(userRepository.saveAndFlush(any(User.class))).thenThrow(databaseException);

        assertThatThrownBy(() -> authService.signup(request))
                .isInstanceOf(DuplicateEmailException.class)
                .hasMessage("이미 가입된 이메일입니다: duplicate@contextstt.com")
                .hasCause(databaseException);
    }
}