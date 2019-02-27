package org.tonycox.ktor.banking.account.repository

interface AccountEventRepository {
    fun getAllEvents(userId: Long): List<AccountEventDataKeeper>
    fun save(event: AccountEventDataKeeper): AccountEventDataKeeper
}