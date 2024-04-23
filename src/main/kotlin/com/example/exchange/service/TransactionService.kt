package com.example.exchange.service

import com.example.exchange.domain.*
import com.example.exchange.domain.TransactionType.*
import com.example.exchange.repository.TransactionRepository
import com.example.exchange.repository.WalletBalanceRepository
import kotlinx.coroutines.flow.Flow
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.math.BigDecimal
import java.time.LocalDateTime

@Service
class TransactionService(
    private val walletBalanceRepository: WalletBalanceRepository,
    private val transactionRepository: TransactionRepository,
    private val walletService: WalletService
) {
    @Transactional
    suspend fun processTransaction(transactionId: TransactionId): Transaction {
        val transaction = requireNotNull(transactionRepository.findById(transactionId.id)) { "Transaction not found" }
        if (transaction.status != TransactionStatus.PENDING)
            throw IllegalStateException("The transaction has already been processed")

        val walletBalance =
            walletService.findOrCreateBalance(WalletId(transaction.walletId), transaction.currency)

        when (transaction.type) {
            DEPOSIT, SPOT_WITHDRAWAL -> walletBalance.balance += transaction.amount
            WITHDRAWAL, SPOT_DEPOSIT -> walletBalance.balance -= transaction.amount
        }

        if (walletBalance.balance >= BigDecimal.ZERO) {
            transaction.status = TransactionStatus.SUCCESS
            walletBalanceRepository.save(walletBalance)
        } else {
            transaction.status = TransactionStatus.FAIL
        }
        return transactionRepository.save(transaction)
    }

    @Transactional
    suspend fun createTransaction(
        walletId: WalletId,
        currency: Currency,
        amount: BigDecimal,
        transactionType: TransactionType
    ): Transaction {
        if (amount <= BigDecimal.ZERO)
            throw IllegalArgumentException("The transaction amount must be positive")

        return transactionRepository.save(
            Transaction(
                walletId = walletId.id,
                type = transactionType,
                amount = amount,
                currency = currency,
                status = TransactionStatus.PENDING,
//                createdAt = LocalDateTime.now()
            )
        )
    }

    @Transactional
    suspend fun transactionHistory(walletId: WalletId): Flow<Transaction> =
        transactionRepository.findAllByWalletIdOrderByCreatedAtDesc(walletId.id)
}