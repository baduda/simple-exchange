package com.example.exchange

import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.web.reactive.server.WebTestClient


@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class SimpleExchangeApplicationTests(
    @Autowired private val web: WebTestClient
) {


    @Test
    fun `should do a rest call`() {
        web.get().uri("/v1/wallet/history")
            .exchange()
            .expectStatus()
            .isOk
    }

    @Test
    fun contextLoads() {
    }
}
