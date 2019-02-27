package org.tonycox.ktor.banking.account.service

import java.math.BigDecimal
import java.time.LocalDateTime

enum class EventType { WITHDRAW, DEPOSIT, TRANSFER_IN, TRANSFER_OUT }

open class AccountEvent(
    open val userId: Long,
    open val amount: BigDecimal,
    open val eventType: EventType,
    open val date: LocalDateTime
)

data class TransferAccountEvent(
    override val userId: Long,
    override val amount: BigDecimal,
    override val eventType: EventType,
    override val date: LocalDateTime,
    val destinationUserId: Long = userId
) : AccountEvent(userId, amount, eventType, date)

class BalanceProjection(val amount: BigDecimal)
