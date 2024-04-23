package com.example.exchange.service

import com.example.exchange.domain.*
import com.example.exchange.domain.OrderType.*
import com.example.exchange.domain.TransactionType.SPOT_DEPOSIT
import com.example.exchange.repository.OrderRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.toList
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.math.BigDecimal

@Service
class ExchangeService(
    private val orderRepository: OrderRepository,
    private val transactionService: TransactionService
) {

    @Transactional
    suspend fun openOrder(
        walletId: WalletId,
        baseCurrency: Currency,
        quoteCurrency: Currency,
        amount: BigDecimal,
        price: BigDecimal,
        orderType: OrderType
    ): OrderStatus {
        //todo: amount > 0, fromCurrencyCode!=toCurrencyCode
        val transaction = when (orderType) {
            BUY -> transactionService.createTransaction(walletId, baseCurrency, amount * price, SPOT_DEPOSIT)
            SELL -> transactionService.createTransaction(walletId, quoteCurrency, amount, SPOT_DEPOSIT)
        }
        val spotDepositTransaction = transactionService.processTransaction(TransactionId(transaction.transactionId!!))

        val order = orderRepository.save(
            Order(
                userId = walletId.id,
                type = orderType,
                baseCurrency = baseCurrency,
                quoteCurrency = quoteCurrency,
                amount = amount,
                price = price,
                status = OrderStatus.OPEN,
                spotDepositTransactionId = spotDepositTransaction.transactionId!!
            )
        )

        val matchedOrders = orderRepository
            .findMatchedOrders(
                baseCurrency,
                quoteCurrency,
                if (orderType == BUY) SELL else BUY,
                OrderStatus.OPEN,
                price
            ).toList()

        println("matchedOrders = ${matchedOrders.size}")

        return order.status
    }

    @Transactional
    suspend fun cancelOrder(orderId: OrderId): Order {
        val order = requireNotNull(orderRepository.findById(orderId.id)) { "Order not found" }
        return orderRepository.save(order.copy(status = OrderStatus.CANCELLED))
    }

    @Transactional
    fun openOrders(userId: UserId): Flow<Order> =
        orderRepository.findAllByUserIdAndStatusOrderByCreatedAtDesc(userId.id, OrderStatus.OPEN)
}