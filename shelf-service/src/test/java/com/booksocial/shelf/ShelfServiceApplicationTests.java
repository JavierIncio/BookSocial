package com.booksocial.shelf;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest(properties = {
        "spring.rabbitmq.listener.simple.auto-startup=false",
        "app.seed.shelves=false"
})
class ShelfServiceApplicationTests {

    @Test
    void contextLoads() {
    }

}
