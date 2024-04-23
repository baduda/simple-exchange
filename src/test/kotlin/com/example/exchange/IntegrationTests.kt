package com.example.exchange

import com.example.exchange.domain.Currency
import com.example.exchange.domain.OrderStatus
import com.example.exchange.domain.OrderType
import com.example.exchange.wallet.*
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.core.ParameterizedTypeReference
import org.springframework.test.web.reactive.server.WebTestClient
import java.math.BigDecimal


@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class IntegrationTests(
    @Autowired private val web: WebTestClient
) {

    @Test
    fun `should do deposit`() {
        val result = web.deposit(DepositRequest(BigDecimal(100), Currency.USD))

        assertThat(result).isNotNull
    }

    @Test
    fun `should do withdrawal`() {
        val balance = web.deposit(DepositRequest(BigDecimal(100), Currency.USD))
        val result = web.withdrawal(WithdrawalRequest(BigDecimal(50), Currency.USD))

        assertThat(result).isNotNull
        assertThat(result!!.amount).isEqualTo(balance!!.amount - BigDecimal(50))
    }

    @Test
    fun `should return history`() {
        val initialHistory = web.history()
        assertThat(initialHistory).isNotNull
        requireNotNull(initialHistory)

        val balance = web.deposit(DepositRequest(BigDecimal(100), Currency.USD))
        val result = web.withdrawal(WithdrawalRequest(BigDecimal(50), Currency.USD))

        val history = web.history()
        assertThat(history)
            .isNotNull
            .hasSizeGreaterThanOrEqualTo(2)
        requireNotNull(history)

        assertThat(history - initialHistory)
            .hasSize(2)
        //todo: improve test
    }

    @Test
    fun `should open order`() {
        web.deposit(DepositRequest(BigDecimal(100), Currency.USD))

        val result = web.openOrder(
            OpenOrderRequest(
                baseCurrency = Currency.USD,
                quoteCurrency = Currency.BTC,
                amount = BigDecimal(10),
                price = BigDecimal(5.123124),
                orderType = OrderType.BUY
            )
        )

        assertThat(result).isNotNull
        assertThat(result!!).isEqualTo(OrderStatus.OPEN)
    }
}

private fun WebTestClient.deposit(depositRequest: DepositRequest): WalletBalanceResponse? =
    post()
        .uri("/v1/wallet/deposit")
        .bodyValue(depositRequest)
        .exchange()
        .expectStatus()
        .isOk
        .expectBody(WalletBalanceResponse::class.java)
        .returnResult()
        .responseBody

private fun WebTestClient.withdrawal(withdrawalRequest: WithdrawalRequest): WalletBalanceResponse? =
    post()
        .uri("/v1/wallet/withdrawal")
        .bodyValue(withdrawalRequest)
        .exchange()
        .expectStatus()
        .isOk
        .expectBody(WalletBalanceResponse::class.java)
        .returnResult()
        .responseBody

private fun WebTestClient.history(): List<TransactionResponse>? =
    get()
        .uri("/v1/wallet/history")
        .exchange()
        .expectStatus()
        .isOk
        .expectBody(typeReference<List<TransactionResponse>>())
        .returnResult()
        .responseBody

private fun WebTestClient.openOrder(request: OpenOrderRequest): OrderStatus? =
    post()
        .uri("/v1/exchange/order")
        .bodyValue(request)
        .exchange()
        .expectStatus()
        .isOk
        .expectBody(OrderStatus::class.java)
        .returnResult()
        .responseBody


private fun WebTestClient.openOrders(): List<OrderResponse>? =
    get()
        .uri("/v1/exchange/order")
        .exchange()
        .expectStatus()
        .isOk
        .expectBody(typeReference<List<OrderResponse>>())
        .returnResult()
        .responseBody

internal inline fun <reified T> typeReference() = object : ParameterizedTypeReference<T>() {}
