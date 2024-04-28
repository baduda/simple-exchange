package com.example.exchange.service

import com.example.exchange.domain.*
import com.example.exchange.domain.TransactionType.*
import com.example.exchange.repository.TransactionRepository
import com.example.exchange.repository.WalletBalanceRepository
import kotlinx.coroutines.flow.toList
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.math.BigDecimal

@Service
class TransactionService(
    private val walletBalanceRepository: WalletBalanceRepository,
    private val transactionRepository: TransactionRepository,
    private val walletService: WalletService
) {
    @Transactional
    suspend fun process(transactionId: TransactionId): Transaction {
        val transaction = requireNotNull(transactionRepository.findById(transactionId.id)) { "Transaction not found" }
        check(transaction.status == TransactionStatus.PENDING) { "The transaction has already been processed" }

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
    suspend fun create(
        walletId: WalletId,
        currency: Currency,
        amount: BigDecimal,
        transactionType: TransactionType
    ): Transaction {
        check(amount > BigDecimal.ZERO) { "The transaction amount must be positive" }

        return transactionRepository.save(
            Transaction(
                walletId = walletId.id,
                type = transactionType,
                amount = amount,
                currency = currency,
                status = TransactionStatus.PENDING
            )
        )
    }

    @Transactional
    suspend fun createAndProcess(
        walletId: WalletId,
        currency: Currency,
        amount: BigDecimal,
        transactionType: TransactionType
    ): Transaction {
        val transaction = create(walletId, currency, amount, transactionType)
        return process(TransactionId(transaction.transactionId!!))
    }

    @Transactional
    suspend fun history(walletId: WalletId): List<Transaction> =
        transactionRepository.findAllByWalletIdOrderByCreatedAtDesc(walletId.id).toList()
}