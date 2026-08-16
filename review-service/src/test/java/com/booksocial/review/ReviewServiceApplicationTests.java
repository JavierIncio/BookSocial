package com.booksocial.review;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest(properties = {"app.seed.books=false", "spring.rabbitmq.listener.simple.auto-startup=false"})
class ReviewServiceApplicationTests {

	@Test
	void contextLoads() {
	}

}
