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
    fun findAllByUserIdAndStatusOrderByCreatedAtDesc(userId: Int, orderStatus: OrderStatus): Flow<Order>

    @Lock(LockMode.PESSIMISTIC_WRITE)
    @Query(
        """ 
            WITH OrderedOrders AS (
                SELECT *,
                       SUM(amount) OVER (ORDER BY price ASC, created_at ASC) AS cumulative_amount,
                       LEAD(order_id) OVER (ORDER BY created_at ASC) AS next_order_id
                FROM orders
                WHERE base_currency = :baseCurrency
                  AND quote_currency = :quoteCurrency
                  AND type = :orderType
                  AND status = :status
            ),
                 FilteredOrders AS (
                     SELECT *
                     FROM OrderedOrders
                     WHERE cumulative_amount <= :amount
                 ),
                 NextOrder AS (
                     SELECT *
                     FROM OrderedOrders
                     WHERE order_id = (
                         SELECT next_order_id
                         FROM FilteredOrders
                         ORDER BY created_at DESC
                         LIMIT 1
                         )
                 )
            SELECT * FROM FilteredOrders
            UNION ALL
            SELECT * FROM NextOrder 
        """
    )
    suspend fun findMatchedOrders(
        baseCurrency: Currency,
        quoteCurrency: Currency,
        orderType: OrderType,
        orderStatus: OrderStatus,
        amount: BigDecimal
    ): Flow<Order>
}
