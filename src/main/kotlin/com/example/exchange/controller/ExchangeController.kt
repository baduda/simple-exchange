package com.example.exchange.controller

import com.example.exchange.domain.*
import com.example.exchange.service.ExchangeService
import com.example.exchange.service.WalletService
import org.springframework.web.bind.annotation.*
import java.math.BigDecimal
import java.math.RoundingMode
import java.time.LocalDateTime

@RestController
@RequestMapping("/v1/exchange/order")
class ExchangeController(
    private val exchangeService: ExchangeService,
    private val walletService: WalletService
) {

    @GetMapping
    suspend fun openOrders(): List<OrderResponse> {
        val wallet = walletService.findOrCreate(userId())
        return exchangeService.openOrders(WalletId(wallet.walletId!!)).map { it.toDto() }
    }

    @PostMapping
    suspend fun openOrder(@RequestBody request: OpenOrderRequest): OrderStatus {
        val wallet = walletService.findOrCreate(userId())

        return exchangeService.openOrder(
            walletId = WalletId(wallet.walletId!!),
            baseCurrency = request.baseCurrency,
            quoteCurrency = request.quoteCurrency,
            amount = request.amount.setScale(8, RoundingMode.HALF_UP),
            price = request.price.setScale(8, RoundingMode.HALF_UP),
            orderType = request.orderType
        )
    }

    @DeleteMapping("/{orderId}")
    suspend fun cancelOrder(@PathVariable orderId: Int): OrderStatus {
        return exchangeService.cancelOrder(OrderId(orderId))
    }
}

data class OpenOrderRequest(
    val baseCurrency: Currency,
    val quoteCurrency: Currency,
    val amount: BigDecimal,
    val price: BigDecimal,
    val orderType: OrderType
)

data class OrderResponse(
    var id: Int,
    var type: OrderType,
    var baseCurrency: Currency,
    var quoteCurrency: Currency,
    var amount: BigDecimal,
    var price: BigDecimal,
    var status: OrderStatus,
    var createdAt: LocalDateTime
)

private fun Order.toDto() =
    OrderResponse(orderId!!, type, baseCurrency, quoteCurrency, amount, price, status, createdAt)