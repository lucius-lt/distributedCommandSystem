package com.c2;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest(properties = "agent.server.enabled=false")
class C2ApplicationTests {

    @Test
    void contextLoads() {
    }

}
