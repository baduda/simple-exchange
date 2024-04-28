package com.example.exchange.repository

import com.example.exchange.domain.*
import kotlinx.coroutines.flow.Flow
import org.springframework.data.r2dbc.repository.Query
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
    fun findAllByWalletIdAndStatusInOrderByCreatedAtDesc(walletId: Int, orderStatuses: Set<OrderStatus>): Flow<Order>

    @Lock(LockMode.PESSIMISTIC_WRITE)
    @Query(
        """ 
        WITH MatchedOrders AS (SELECT *, SUM(amount) OVER (ORDER BY price DESC , created_at ASC) AS cumulative_amount
                   FROM orders
                   WHERE base_currency = :baseCurrency
                     AND quote_currency = :quoteCurrency
                     AND type = 'BUY'
                     AND status in ('OPEN', 'PARTIALLY_FULFILLED')
                     AND price >= :price
                   ORDER BY price DESC, created_at ASC)
                   
        SELECT *
        FROM MatchedOrders
        LIMIT (SELECT count(*) + 1
               FROM MatchedOrders
               WHERE cumulative_amount <= :amount)
        """
    )
    fun findMatchedBuyOrders(
        baseCurrency: Currency,
        quoteCurrency: Currency,
        amount: BigDecimal,
        price: BigDecimal
    ): Flow<Order>

    @Lock(LockMode.PESSIMISTIC_WRITE)
    @Query(
        """ 
        WITH MatchedOrders AS (SELECT *, SUM(amount) OVER (ORDER BY price ASC , created_at ASC) AS cumulative_amount
                   FROM orders
                   WHERE base_currency = :baseCurrency
                     AND quote_currency = :quoteCurrency
                     AND type = 'SELL'
                     AND status in ('OPEN', 'PARTIALLY_FULFILLED')
                     AND price <= :price
                   ORDER BY price ASC, created_at ASC)
                   
        SELECT *
        FROM MatchedOrders
        LIMIT (SELECT count(*) + 1
               FROM MatchedOrders
               WHERE cumulative_amount <= :amount)
        """
    )
    fun findMatchedSellOrders(
        baseCurrency: Currency,
        quoteCurrency: Currency,
        amount: BigDecimal,
        price: BigDecimal
    ): Flow<Order>
}

interface TradeRepository : CoroutineCrudRepository<Trade, Int>
