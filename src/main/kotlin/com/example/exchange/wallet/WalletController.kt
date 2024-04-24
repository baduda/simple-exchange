package com.example.exchange.wallet

import com.example.exchange.domain.*
import com.example.exchange.domain.TransactionType.DEPOSIT
import com.example.exchange.domain.TransactionType.WITHDRAWAL
import com.example.exchange.service.TransactionService
import com.example.exchange.service.WalletService
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.reactor.awaitSingle
import org.springframework.security.core.context.ReactiveSecurityContextHolder
import org.springframework.web.bind.annotation.*
import java.math.BigDecimal
import java.math.RoundingMode
import java.time.LocalDateTime

@RestController
@RequestMapping("/v1/wallet")
class WalletController(
    private val transactionService: TransactionService,
    private val walletService: WalletService
) {

    @PostMapping("/deposit")
    suspend fun deposit(@RequestBody request: DepositRequest): WalletBalanceResponse {
        val userId = userId()
        val wallet = walletService.findOrCreate(userId)
        val transaction =
            transactionService.create(
                WalletId(wallet.walletId!!),
                request.currency,
                request.amount.setScale(8, RoundingMode.HALF_UP),
                DEPOSIT
            )
        transactionService.process(TransactionId(transaction.transactionId!!))
        val walletBalance = walletService.findOrCreateBalance(WalletId(transaction.walletId), transaction.currency)

        return walletBalance.toDto()
    }

    @PostMapping("/withdrawal")
    suspend fun withdrawal(@RequestBody request: WithdrawalRequest): WalletBalanceResponse {
        val wallet = walletService.findOrCreate(userId())
        val transaction = transactionService.create(
            WalletId(wallet.walletId!!),
            request.currency,
            request.amount.setScale(8, RoundingMode.HALF_UP),
            WITHDRAWAL
        )
        transactionService.process(TransactionId(transaction.transactionId!!))
        val walletBalance = walletService.findOrCreateBalance(WalletId(transaction.walletId), transaction.currency)

        return walletBalance.toDto()
    }

    @GetMapping
    suspend fun walletBalances(): WalletBalancesResponse {
        val balances = Currency.entries.map { currency -> walletService.findOrCreateBalance(userId(), currency).toDto() }
        return WalletBalancesResponse(balances)
    }

    @GetMapping("/history")
    suspend fun walletHistory(): Flow<TransactionResponse> {
        val wallet = walletService.findOrCreate(userId())
        return transactionService.history(WalletId(wallet.walletId!!)).map { it.toDto() }
    }
}

data class DepositRequest(val amount: BigDecimal, val currency: Currency)
data class WithdrawalRequest(val amount: BigDecimal, val currency: Currency)
data class WalletBalanceResponse(val amount: BigDecimal, val currency: Currency)
data class WalletBalancesResponse(val balances: List<WalletBalanceResponse>)
data class TransactionResponse(
    var type: TransactionType,
    var amount: BigDecimal,
    var currency: Currency,
    var createdAt: LocalDateTime,
    var status: TransactionStatus
)

private fun WalletBalance.toDto() = WalletBalanceResponse(balance, currency)
private fun Transaction.toDto() = TransactionResponse(type, amount, currency, createdAt, status)
internal suspend fun userId() =
    ReactiveSecurityContextHolder.getContext().map { UserId(it.authentication.name.toInt()) }.awaitSingle()