package com.example.exchange.wallet

import com.example.exchange.domain.*
import com.example.exchange.service.ExchangeService
import com.example.exchange.service.WalletService
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import org.springframework.web.bind.annotation.*
import java.math.BigDecimal
import java.time.LocalDateTime

@RestController
@RequestMapping("/v1/exchange/order")
class ExchangeController(
    private val exchangeService: ExchangeService,
    private val walletService: WalletService
) {

    @GetMapping
    suspend fun openOrders(): Flow<OrderResponse> {
        val userId = UserId(0)
        val wallet = walletService.findOrCreate(userId)
        return exchangeService.openOrders(WalletId(wallet.walletId!!)).map { it.toDto() }
    }

    @PostMapping
    suspend fun openOrder(@RequestBody request: OpenOrderRequest): OrderStatus {
        val userId = UserId(0)
        val wallet = walletService.findOrCreate(userId)

        return exchangeService.openOrder(
            walletId = WalletId(wallet.walletId!!),
            baseCurrency = request.baseCurrency,
            quoteCurrency = request.quoteCurrency,
            amount = request.amount,
            price = request.price,
            orderType = request.orderType
        )
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
    var type: OrderType,
    var baseCurrency: Currency,
    var quoteCurrency: Currency,
    var amount: BigDecimal,
    var price: BigDecimal,
    var status: OrderStatus,
    var createdAt: LocalDateTime
)

private fun Order.toDto() =
    OrderResponse(type, baseCurrency, quoteCurrency, amount, price, status, createdAt)