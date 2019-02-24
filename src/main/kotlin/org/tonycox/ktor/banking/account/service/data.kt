package org.tonycox.ktor.banking.account.service

import org.jetbrains.exposed.dao.EntityID
import org.jetbrains.exposed.dao.LongEntity
import org.jetbrains.exposed.dao.LongEntityClass
import org.jetbrains.exposed.dao.LongIdTable
import java.math.BigDecimal
import java.time.LocalDateTime

enum class EventType { WITHDRAW, DEPOSIT, TRANSFER_IN, TRANSFER_OUT }

data class AccountEvent(
    val userId: Long,
    val destinationUserId: Long = userId,
    val amount: BigDecimal,
    val eventType: EventType,
    val date: LocalDateTime
)

object AccountEventTable : LongIdTable("events") {
    val userId = long("user_id")
    val destinationUserId = long("dest_user_id")
    val amount = decimal("amount", precision = 12, scale = 2)
    val eventType = enumeration("event_type", EventType::class)
    val date = datetime("date")
}

class AccountEventDao(id: EntityID<Long>) : LongEntity(id) {
    companion object : LongEntityClass<AccountEventDao>(AccountEventTable)

    var userId by AccountEventTable.userId
    var destinationUserId by AccountEventTable.destinationUserId
    var amount by AccountEventTable.amount
    var eventType by AccountEventTable.eventType
    var date by AccountEventTable.date
}

class BalanceProjection(val amount: BigDecimal)