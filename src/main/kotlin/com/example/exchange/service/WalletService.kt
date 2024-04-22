package com.example.exchange.service

import com.example.exchange.domain.*
import com.example.exchange.repository.WalletBalanceRepository
import com.example.exchange.repository.WalletRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class WalletService(
    private val walletRepository: WalletRepository,
    private val walletBalanceRepository: WalletBalanceRepository
) {
    suspend fun findOrCreate(userId: UserId): Wallet =
        walletRepository.findByUserId(userId.id) ?: walletRepository.save(Wallet(userId = userId.id))

    @Transactional
    suspend fun findOrCreateBalance(userId: UserId, currency: Currency): WalletBalance {
        val walletId = findOrCreate(userId).walletId!!
        return findOrCreateBalance(WalletId(walletId), currency)
    }

    @Transactional
    suspend fun findOrCreateBalance(walletId: WalletId, currency: Currency): WalletBalance =
        walletBalanceRepository.findByWalletIdAndCurrency(walletId.id, currency)
            ?: walletBalanceRepository.save(WalletBalance(walletId = walletId.id, currency = currency))
}