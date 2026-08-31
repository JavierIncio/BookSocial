package com.booksocial.identity;

import com.booksocial.identity.security.RateLimitService;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

@SpringBootTest
class IdentityServiceApplicationTests {

    @MockitoBean
    RateLimitService rateLimitService;

    @Test
    void contextLoads() {
    }

}
