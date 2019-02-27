package org.tonycox.ktor.banking.account.repository

import org.jetbrains.exposed.dao.EntityID
import org.jetbrains.exposed.dao.LongEntity
import org.jetbrains.exposed.dao.LongEntityClass
import org.jetbrains.exposed.dao.LongIdTable
import org.tonycox.ktor.banking.account.service.EventType
import org.tonycox.ktor.banking.account.toJodaDate
import java.math.BigDecimal
import java.time.LocalDateTime

object AccountEventTable : LongIdTable("events") {
    val userId = long("user_id")
    val amount = decimal("amount", precision = 12, scale = 2)
    val eventType = enumeration("event_type", EventType::class)
    val date = datetime("created_date")
}

class AccountEventDao(id: EntityID<Long>) : LongEntity(id) {
    companion object : LongEntityClass<AccountEventDao>(AccountEventTable)

    var userId by AccountEventTable.userId
    var amount by AccountEventTable.amount
    var eventType by AccountEventTable.eventType
    var date by AccountEventTable.date
}

data class AccountEventDataKeeper(
    val userId: Long,
    val amount: BigDecimal,
    val eventType: EventType,
    val date: LocalDateTime
)
