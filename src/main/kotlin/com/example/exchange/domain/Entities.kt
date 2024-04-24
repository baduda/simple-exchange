package com.example.exchange.domain

import org.springframework.data.annotation.Id
import org.springframework.data.relational.core.mapping.Table
import java.math.BigDecimal
import java.time.LocalDateTime


@Table("wallets")
data class Wallet(
    @Id val walletId: Int? = null,
    var userId: Int
)

@Table("wallet_balances")
data class WalletBalance(
    @Id val walletBalanceId: Int? = null,
    var walletId: Int,
    var currency: Currency,
    var balance: BigDecimal = BigDecimal.ZERO
)

@Table("transactions")
data class Transaction(
    @Id val transactionId: Int? = null,
    var walletId: Int,
    var type: TransactionType,
    var amount: BigDecimal,
    var currency: Currency,
    var createdAt: LocalDateTime = LocalDateTime.now(),
    var status: TransactionStatus
)

@Table("orders")
data class Order(
    @Id val orderId: Int? = null,
    var walletId: Int,
    var type: OrderType,
    var baseCurrency: Currency,
    var quoteCurrency: Currency,
    var originalAmount: BigDecimal,
    var amount: BigDecimal,
    var price: BigDecimal,
    var status: OrderStatus,
    var createdAt: LocalDateTime = LocalDateTime.now(),
    var spotDepositTransactionId: Int
)

@Table("trades")
data class Trade(
    @Id val tradeId: Int? = null,
    var buyOrderId: Int,
    var sellOrderId: Int,
    var buyTransactionId: Int,
    var sellTransactionId: Int,
    var createdAt: LocalDateTime = LocalDateTime.now()
)

enum class TransactionType {
    DEPOSIT, WITHDRAWAL, SPOT_DEPOSIT, SPOT_WITHDRAWAL
}

enum class TransactionStatus {
    PENDING, SUCCESS, FAIL
}

enum class OrderType {
    BUY, SELL
}

enum class OrderStatus {
    OPEN, PARTIALLY_FULFILLED, FULFILLED, CANCELLED
}

enum class Currency {
    USD, EUR, BTC, ETH, USDT
}

data class TransactionId(val id: Int)
data class UserId(val id: Int)
data class WalletId(val id: Int)
data class WalletBalanceId(val id: Int)
data class OrderId(val id: Int)