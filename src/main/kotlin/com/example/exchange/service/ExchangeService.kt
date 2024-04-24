package com.example.exchange.service

import com.example.exchange.domain.*
import com.example.exchange.domain.OrderStatus.*
import com.example.exchange.domain.OrderType.*
import com.example.exchange.domain.TransactionType.SPOT_DEPOSIT
import com.example.exchange.domain.TransactionType.SPOT_WITHDRAWAL
import com.example.exchange.repository.OrderRepository
import com.example.exchange.repository.TradeRepository
import com.example.exchange.repository.TransactionRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.toList
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.math.BigDecimal
import java.math.RoundingMode

@Service
class ExchangeService(
    private val orderRepository: OrderRepository,
    private val transactionService: TransactionService,
    private val tradeRepository: TradeRepository,
    private val transactionRepository: TransactionRepository
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
        val spotDepositTransaction = createTransaction(orderType, walletId, baseCurrency, amount, price, quoteCurrency)
        val order = saveOrder(walletId, orderType, baseCurrency, quoteCurrency, amount, price, spotDepositTransaction)

        when (order.type) {
            SELL -> fulfillBuyOrders(order, baseCurrency, quoteCurrency, amount, price)
            BUY -> fulfillSellOrders(order, baseCurrency, quoteCurrency, amount, price)
        }
        return orderRepository.findById(order.orderId!!)!!.status
    }

    private suspend fun fulfillSellOrders(
        order: Order,
        baseCurrency: Currency,
        quoteCurrency: Currency,
        amount: BigDecimal,
        price: BigDecimal
    ) {
        val matchedOrders = orderRepository.findMatchedSellOrders(baseCurrency, quoteCurrency, amount, price).toList()
        matchedOrders.forEach { matched ->
            val fulfilled = setOf(order, matched).minBy { it.amount }

            val savedOrder = orderRepository.save(order - fulfilled)
            val savedMatched = orderRepository.save(matched - fulfilled)

            val sellTransaction = createAndProcessTransaction(order, savedOrder, fulfilled)
            val buyTransaction = createAndProcessTransaction(matched, savedMatched, fulfilled)

            tradeRepository.save(
                Trade(
                    buyOrderId = order.orderId!!,
                    sellOrderId = matched.orderId!!,
                    buyTransactionId = buyTransaction.transactionId!!,
                    sellTransactionId = sellTransaction.transactionId!!
                )
            )

            order.amount = savedOrder.amount
            if (savedOrder.status == FULFILLED) return
        }
    }

    private suspend fun fulfillBuyOrders(
        order: Order,
        baseCurrency: Currency,
        quoteCurrency: Currency,
        amount: BigDecimal,
        price: BigDecimal
    ) {
        val matchedOrders = orderRepository.findMatchedBuyOrders(baseCurrency, quoteCurrency, amount, price).toList()
        matchedOrders.forEach { matched ->
            val fulfilled = setOf(order, matched).minBy { it.amount }

            val savedOrder = orderRepository.save(order - fulfilled)
            val savedMatched = orderRepository.save(matched - fulfilled)

            val buyTransaction = createAndProcessTransaction(order, savedOrder, fulfilled)
            val sellTransaction = createAndProcessTransaction(matched, savedMatched, fulfilled)

            tradeRepository.save(
                Trade(
                    buyOrderId = order.orderId!!,
                    sellOrderId = matched.orderId!!,
                    buyTransactionId = buyTransaction.transactionId!!,
                    sellTransactionId = sellTransaction.transactionId!!
                )
            )
            order.amount = savedOrder.amount
            if (savedOrder.status == FULFILLED) return
        }
    }

    private suspend fun createAndProcessTransaction(order: Order, updated: Order, fulfilled: Order): Transaction {
        val (currency, amount) = when (order.type) {
            BUY -> order.baseCurrency to (order.amount - updated.amount)
            SELL -> order.quoteCurrency to (order.amount - updated.amount) * fulfilled.price
        }
        return transactionService.createAndProcess(WalletId(order.walletId), currency, amount, SPOT_WITHDRAWAL)
    }

    private suspend fun saveOrder(
        walletId: WalletId,
        orderType: OrderType,
        baseCurrency: Currency,
        quoteCurrency: Currency,
        amount: BigDecimal,
        price: BigDecimal,
        spotDepositTransaction: Transaction
    ): Order =
        orderRepository.save(
            Order(
                walletId = walletId.id,
                type = orderType,
                baseCurrency = baseCurrency,
                quoteCurrency = quoteCurrency,
                originalAmount = amount,
                amount = amount,
                price = price,
                status = OPEN,
                spotDepositTransactionId = spotDepositTransaction.transactionId!!
            )
        )

    private suspend fun createTransaction(
        orderType: OrderType,
        walletId: WalletId,
        baseCurrency: Currency,
        amount: BigDecimal,
        price: BigDecimal,
        quoteCurrency: Currency
    ): Transaction {
        check(amount > BigDecimal.ZERO) { "The amount must be positive." }
        check(baseCurrency != quoteCurrency) { "The base and quote currencies must not be the same." }

        val transaction = when (orderType) {
            BUY -> transactionService.create(walletId, baseCurrency, amount * price, SPOT_DEPOSIT)
            SELL -> transactionService.create(walletId, quoteCurrency, amount, SPOT_DEPOSIT)
        }
        val spotDepositTransaction = transactionService.process(TransactionId(transaction.transactionId!!))
        return spotDepositTransaction
    }

    @Transactional
    suspend fun openOrders(walletId: WalletId): Flow<Order> =
        orderRepository.findAllByWalletIdAndStatusInOrderByCreatedAtDesc(walletId.id, setOf(OPEN, PARTIALLY_FULFILLED))

    @Transactional
    suspend fun cancelOrder(orderId: OrderId): OrderStatus {
        val order = checkNotNull(orderRepository.findById(orderId.id)) { "Order not found" }
        val depositTransaction = checkNotNull(transactionRepository.findById(order.spotDepositTransactionId))
        transactionService.createAndProcess(
            WalletId(depositTransaction.walletId),
            depositTransaction.currency,
            depositTransaction.amount,
            SPOT_WITHDRAWAL
        )
        order.status = CANCELLED
        orderRepository.save(order)
        return order.status
    }
}

private operator fun Order.minus(order: Order): Order {
    val result = copy(amount = (amount - order.amount).setScale(8, RoundingMode.HALF_UP))
    result.status = if (result.amount.compareTo(BigDecimal.ZERO) == 0) FULFILLED else PARTIALLY_FULFILLED
    return result
}