package org.tonycox.ktor.banking.account.repository

import org.tonycox.ktor.banking.account.toJavaDateTime
import org.tonycox.ktor.banking.account.toJodaDate

class AccountEventRepositoryImpl : AccountEventRepository {

    override fun getAllEvents(userId: Long): List<AccountEventDataKeeper> {
        return AccountEventDao.find { AccountEventTable.userId.eq(userId) }
            .forUpdate()
            .map { it.toKeeper() }
    }

    override fun save(event: AccountEventDataKeeper): AccountEventDataKeeper {
        return AccountEventDao.new(event.id) {
            userId = event.userId
            amount = event.amount
            eventType = event.eventType
            date = event.date.toJodaDate()
        }.toKeeper()
    }

    private fun AccountEventDao.toKeeper(): AccountEventDataKeeper {
        return AccountEventDataKeeper(
            this.id.value,
            this.userId,
            this.amount,
            this.eventType,
            this.date.toJavaDateTime()
        )
    }
}
