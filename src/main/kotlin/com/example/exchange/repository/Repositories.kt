package com.example.exchange.repository

import com.example.exchange.domain.*
import kotlinx.coroutines.flow.Flow
import org.springframework.data.relational.core.sql.LockMode
import org.springframework.data.relational.repository.Lock
import org.springframework.data.repository.kotlin.CoroutineCrudRepository
import java.math.BigDecimal

interface TransactionRepository : CoroutineCrudRepository<Transaction, Int> {
    fun findAllByWalletIdOrderByCreatedAtDesc(walletId: Int): Flow<Transaction>
}

interface WalletRepository : CoroutineCrudRepository<Wallet, Int> {
    suspend fun findByUserId(userId: Int): Wallet?
}

interface WalletBalanceRepository : CoroutineCrudRepository<WalletBalance, Int> {
    @Lock(LockMode.PESSIMISTIC_WRITE)
    suspend fun findByWalletIdAndCurrency(walletId: Int, currency: Currency): WalletBalance?
}

interface OrderRepository : CoroutineCrudRepository<Order, Int> {
    fun findAllByUserIdAndStatusOrderByCreatedAtDesc(userId: Int, orderStatus: OrderStatus): Flow<Order>

    @Lock(LockMode.PESSIMISTIC_WRITE)
    suspend fun findAllByBaseCurrencyAndQuoteCurrencyAndTypeAndPriceLessThanEqualAndStatusOrderByCreatedAt(
        baseCurrency: Currency,
        quoteCurrency: Currency,
        type: OrderType,
        price: BigDecimal,
        orderStatus: OrderStatus
    ): Flow<Order>
}
