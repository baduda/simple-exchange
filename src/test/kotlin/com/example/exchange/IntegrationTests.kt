package com.example.exchange

import com.example.exchange.domain.Currency
import com.example.exchange.domain.Currency.*
import com.example.exchange.domain.OrderStatus
import com.example.exchange.domain.OrderStatus.FULFILLED
import com.example.exchange.domain.OrderStatus.PARTIALLY_FULFILLED
import com.example.exchange.domain.OrderType.BUY
import com.example.exchange.domain.OrderType.SELL
import com.example.exchange.wallet.*
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.core.ParameterizedTypeReference
import org.springframework.security.test.context.support.WithMockUser
import org.springframework.test.web.reactive.server.WebTestClient
import java.math.BigDecimal


@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@WithMockUser(username = "007")
class IntegrationTests(
    @Autowired private val web: WebTestClient
) {

    @BeforeEach
    fun cancelOpenOrders() {
        web.openOrders()!!.forEach { order -> web.cancelOrder(order.id) }
    }

    @Test
    fun `should do deposit`() {
        val result = web.deposit(DepositRequest(BigDecimal(100), USD))

        assertThat(result!!.amount).isGreaterThanOrEqualTo(BigDecimal(100))
    }

    @Test
    fun `should do withdrawal`() {
        val balance = web.deposit(DepositRequest(BigDecimal(100), USD))
        val result = web.withdrawal(WithdrawalRequest(BigDecimal(50), USD))

        assertThat(result!!.amount).isEqualTo(balance!!.amount - BigDecimal(50))
    }

    @Test
    fun `should return balances`() {
        val result = web.balances()

        assertThat(result!!.balances).size().isEqualTo(Currency.entries.size)
    }

    @Test
    fun `should return history`() {
        val initialHistory = web.history()
        assertThat(initialHistory).isNotNull
        requireNotNull(initialHistory)

        web.deposit(DepositRequest(BigDecimal(100), USD))
        web.withdrawal(WithdrawalRequest(BigDecimal(50), USD))

        val history = web.history()

        assertThat(history).hasSizeGreaterThanOrEqualTo(2)
        requireNotNull(history)

        assertThat(history - initialHistory).hasSize(2)
    }

    @Test
    fun `should open order`() {
        web.deposit(DepositRequest(BigDecimal(100), USD))

        val result = web.openOrder(
            OpenOrderRequest(
                baseCurrency = USD,
                quoteCurrency = BTC,
                amount = BigDecimal(10),
                price = BigDecimal(5.123124),
                orderType = BUY
            )
        )

        assertThat(result).isEqualTo(OrderStatus.OPEN)
    }

    @Test
    fun `should return open orders`() {
        val initialOrders = web.openOrders()

        web.deposit(DepositRequest(BigDecimal(100), USD))
        web.openOrder(
            OpenOrderRequest(USD, BTC, BigDecimal(10), BigDecimal(5.123124), BUY)
        )

        val result = web.openOrders()

        assertThat(result).size().isEqualTo(initialOrders!!.size + 1)
    }

    @Test
    fun `should not match single order`() {
        web.deposit(DepositRequest(BigDecimal(100), USD))
        web.deposit(DepositRequest(BigDecimal(100), EUR))

        web.openOrder(OpenOrderRequest(USD, EUR, BigDecimal(10), BigDecimal(1.10), SELL))
        val status = web.openOrder(OpenOrderRequest(USD, EUR, BigDecimal(10), BigDecimal(1.09), BUY))

        assertThat(status).isEqualTo(OrderStatus.OPEN)
    }

    @Test
    fun `should fulfill buy and sell orders`() {
        web.deposit(DepositRequest(BigDecimal(100), USD))
        web.deposit(DepositRequest(BigDecimal(100), EUR))

        web.openOrder(OpenOrderRequest(USD, EUR, BigDecimal(10), BigDecimal(1.10), SELL))
        val status = web.openOrder(OpenOrderRequest(USD, EUR, BigDecimal(10), BigDecimal(1.11), BUY))

        assertThat(status).isEqualTo(FULFILLED)
    }

    @Test
    fun `should partially fulfill buy order and fulfill sell order`() {
        web.deposit(DepositRequest(BigDecimal(100), USD))
        web.deposit(DepositRequest(BigDecimal(100), EUR))

        web.openOrder(OpenOrderRequest(USD, EUR, BigDecimal(11), BigDecimal(1.10), SELL))
        val status = web.openOrder(OpenOrderRequest(USD, EUR, BigDecimal(10), BigDecimal(1.11), BUY))
        val openOrders = web.openOrders()

        assertThat(status).isEqualTo(FULFILLED)
        assertThat(openOrders).size().isEqualTo(1)
        assertThat(openOrders!!.first().status).isEqualTo(PARTIALLY_FULFILLED)
        assertThat(openOrders.first().amount).isEqualByComparingTo(BigDecimal(1))
    }

    @Test
    fun `should fulfill buy order and partially fulfill sell order`() {
        web.deposit(DepositRequest(BigDecimal(100), USD))
        web.deposit(DepositRequest(BigDecimal(100), EUR))

        web.openOrder(OpenOrderRequest(USD, EUR, BigDecimal(10), BigDecimal(1.10), SELL))
        val status = web.openOrder(OpenOrderRequest(USD, EUR, BigDecimal(11), BigDecimal(1.11), BUY))
        val openOrders = web.openOrders()

        assertThat(status).isEqualTo(PARTIALLY_FULFILLED)
        assertThat(openOrders).size().isEqualTo(1)
        assertThat(openOrders!!.first().status).isEqualTo(PARTIALLY_FULFILLED)
        assertThat(openOrders.first().amount).isEqualByComparingTo(BigDecimal(1))
    }

    @Test
    fun `should partially fulfill few order`() {
        web.deposit(DepositRequest(BigDecimal(100), USD))
        web.deposit(DepositRequest(BigDecimal(100), EUR))

        web.openOrder(OpenOrderRequest(USD, EUR, BigDecimal(10), BigDecimal(1.10), SELL))
        web.openOrder(OpenOrderRequest(USD, EUR, BigDecimal(10), BigDecimal(1.11), SELL))
        web.openOrder(OpenOrderRequest(USD, EUR, BigDecimal(10), BigDecimal(1.12), SELL))
        web.openOrder(OpenOrderRequest(USD, EUR, BigDecimal(10), BigDecimal(1.13), SELL))
        web.openOrder(OpenOrderRequest(USD, EUR, BigDecimal(10), BigDecimal(1.14), SELL))
        web.openOrder(OpenOrderRequest(USD, EUR, BigDecimal(10), BigDecimal(1.15), SELL))

        val status = web.openOrder(OpenOrderRequest(USD, EUR, BigDecimal(25), BigDecimal(1.13), BUY))
        val openOrders = web.openOrders()!!.sortedBy { it.price }

        assertThat(status).isEqualTo(FULFILLED)
        assertThat(openOrders).size().isEqualTo(4)

        assertThat(openOrders.first().status).isEqualTo(PARTIALLY_FULFILLED)
        assertThat(openOrders.first().amount).isEqualByComparingTo(BigDecimal(5))
    }

}

private fun WebTestClient.balances(): WalletBalancesResponse? =
    get().uri("/v1/wallet").exchange().expectStatus().isOk
        .expectBody(WalletBalancesResponse::class.java).returnResult().responseBody


private fun WebTestClient.deposit(depositRequest: DepositRequest): WalletBalanceResponse? =
    post().uri("/v1/wallet/deposit").bodyValue(depositRequest).exchange().expectStatus().isOk
        .expectBody(WalletBalanceResponse::class.java).returnResult().responseBody

private fun WebTestClient.withdrawal(withdrawalRequest: WithdrawalRequest): WalletBalanceResponse? =
    post().uri("/v1/wallet/withdrawal").bodyValue(withdrawalRequest).exchange().expectStatus().isOk
        .expectBody(WalletBalanceResponse::class.java).returnResult().responseBody

private fun WebTestClient.history(): List<TransactionResponse>? =
    get().uri("/v1/wallet/history").exchange()
        .expectStatus().isOk.expectBody(typeReference<List<TransactionResponse>>()).returnResult().responseBody

private fun WebTestClient.openOrder(request: OpenOrderRequest): OrderStatus? =
    post().uri("/v1/exchange/order").bodyValue(request).exchange()
        .expectStatus().isOk.expectBody(OrderStatus::class.java).returnResult().responseBody


private fun WebTestClient.openOrders(): List<OrderResponse>? =
    get().uri("/v1/exchange/order").exchange().expectStatus().isOk.expectBody(typeReference<List<OrderResponse>>())
        .returnResult().responseBody

private fun WebTestClient.cancelOrder(orderId: Int) {
    delete().uri("/v1/exchange/order/$orderId").exchange().expectStatus().isOk
}

internal inline fun <reified T> typeReference() = object : ParameterizedTypeReference<T>() {}