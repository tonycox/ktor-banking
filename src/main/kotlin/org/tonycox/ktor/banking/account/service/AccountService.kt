package org.tonycox.ktor.banking.account.service

interface AccountService {
    fun reduceEventsToBalance(userId: Long): BalanceProjection

    fun getAllEvents(userId: Long): List<AccountEvent>

    fun handle(event: AccountEvent)
}
