package org.tonycox.ktor.banking.account.service

import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.transactions.transaction
import org.tonycox.ktor.banking.account.toJavaDateTime
import org.tonycox.ktor.banking.account.toJodaDate
import java.math.BigDecimal

class AccountServiceImpl(private val database: Database) : AccountService {

    init {
        transaction(db = database) {
            SchemaUtils.create(AccountEventTable)
        }
    }

    override fun reduceEventsToBalance(userId: Long): BalanceProjection {
        return transaction(db = database) {
            val list = getAllEvents(userId)
                .map {
                    when (it.eventType) {
                        EventType.DEPOSIT, EventType.TRANSFER_IN -> BalanceProjection(it.amount)
                        EventType.WITHDRAW, EventType.TRANSFER_OUT -> BalanceProjection(it.amount.negate())
                    }
                }
            return@transaction if (list.isEmpty()) {
                BalanceProjection(BigDecimal.ZERO)
            } else {
                list.reduceRight { left, right -> BalanceProjection(left.amount.add(right.amount)) }
            }
        }
    }

    override fun getAllEvents(userId: Long): List<AccountEvent> {
        return transaction(db = database) {
            AccountEventDao.find { AccountEventTable.userId.eq(userId) }
                .map {
                    AccountEvent(
                        it.userId,
                        it.destinationUserId,
                        it.amount,
                        it.eventType,
                        it.date.toJavaDateTime()
                    )
                }
        }
    }

    override fun handle(event: AccountEvent) {
        transaction(db = database) {
            validate(event)
            AccountEventDao.new {
                userId = event.userId
                destinationUserId = event.destinationUserId
                amount = event.amount
                eventType = event.eventType
                date = event.date.toJodaDate()
            }
            if (event.eventType == EventType.TRANSFER_OUT) {
                AccountEventDao.new {
                    userId = event.destinationUserId
                    destinationUserId = event.userId
                    amount = event.amount
                    eventType = EventType.TRANSFER_IN
                    date = event.date.toJodaDate()
                }
            }
        }
    }

    private fun validate(event: AccountEvent) {
        if (event.amount == BigDecimal.ZERO) {
            throw ValidationException("requested amount is zero")
        }
        if (event.eventType == EventType.TRANSFER_OUT || event.eventType == EventType.WITHDRAW) {
            val balance = reduceEventsToBalance(event.userId)
            if (balance.amount < event.amount) {
                throw ValidationException("requested amount is less then balance")
            }
        }
    }
}

