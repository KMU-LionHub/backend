package com.contextstt.backend.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class SignupConcurrencyTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void concurrentSignupCreatesOnlyOneAccount() throws Exception {
        String payload = """
                {
                  "email": "concurrent@contextstt.com",
                  "password": "password1",
                  "nickname": "동시가입"
                }
                """;
        CountDownLatch ready = new CountDownLatch(2);
        CountDownLatch start = new CountDownLatch(1);
        ExecutorService executor = Executors.newFixedThreadPool(2);

        Callable<Integer> signup = () -> {
            ready.countDown();
            if (!start.await(5, TimeUnit.SECONDS)) {
                throw new IllegalStateException("회원가입 요청을 동시에 시작하지 못했습니다.");
            }
            return mockMvc.perform(post("/api/auth/signup")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(payload))
                    .andReturn()
                    .getResponse()
                    .getStatus();
        };

        try {
            Future<Integer> first = executor.submit(signup);
            Future<Integer> second = executor.submit(signup);

            assertThat(ready.await(5, TimeUnit.SECONDS)).isTrue();
            start.countDown();

            List<Integer> statuses = List.of(
                    first.get(10, TimeUnit.SECONDS),
                    second.get(10, TimeUnit.SECONDS)
            );
            assertThat(statuses).containsExactlyInAnyOrder(201, 409);
        } finally {
            start.countDown();
            executor.shutdownNow();
        }
    }
}