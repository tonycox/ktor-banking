package org.tonycox.ktor.banking.account.api

import org.tonycox.ktor.banking.account.service.EventType
import java.math.BigDecimal
import java.time.LocalDateTime

data class StatementDto(
    val amount: BigDecimal,
    val operationType: EventType,
    val date: LocalDateTime
)

data class BalanceDto(val amount: BigDecimal)

data class WithdrawRequest(
    val amount: BigDecimal
)

data class DepositRequest(
    val amount: BigDecimal
)

data class TransferRequest(
    val userId: Long,
    val amount: BigDecimal
)
