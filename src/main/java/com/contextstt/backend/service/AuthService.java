package com.contextstt.backend.service;

import com.contextstt.backend.domain.user.User;
import com.contextstt.backend.domain.user.UserRepository;
import com.contextstt.backend.dto.auth.LoginRequest;
import com.contextstt.backend.dto.auth.LoginResponse;
import com.contextstt.backend.dto.auth.SignupRequest;
import com.contextstt.backend.dto.auth.TokenResponse;
import com.contextstt.backend.dto.user.UserResponse;
import com.contextstt.backend.exception.DuplicateEmailException;
import com.contextstt.backend.exception.InvalidCredentialsException;
import com.contextstt.backend.security.JwtTokenProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;

    @Transactional
    public UserResponse signup(SignupRequest request) {
        if (userRepository.existsByEmail(request.email())) {
            throw new DuplicateEmailException(request.email());
        }

        User user = User.builder()
                .email(request.email())
                .password(passwordEncoder.encode(request.password()))
                .nickname(request.nickname())
                .build();

        return UserResponse.from(userRepository.save(user));
    }

    public LoginResponse login(LoginRequest request) {
        User user = userRepository.findByEmail(request.email())
                .orElseThrow(InvalidCredentialsException::new);

        if (!passwordEncoder.matches(request.password(), user.getPassword())) {
            throw new InvalidCredentialsException();
        }

        String accessToken = jwtTokenProvider.createAccessToken(user.getId(), user.getEmail());

        return LoginResponse.builder()
                .token(TokenResponse.builder()
                        .accessToken(accessToken)
                        .tokenType("Bearer")
                        .expiresInSeconds(jwtTokenProvider.getAccessTokenValiditySeconds())
                        .build())
                .user(UserResponse.from(user))
                .build();
    }
}
