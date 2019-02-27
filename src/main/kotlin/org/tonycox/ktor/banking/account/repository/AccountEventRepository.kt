package org.tonycox.ktor.banking.account.repository

/**
 * Repository to AccountEvent storage.
 */
interface AccountEventRepository {
    /**
     * @return all events related to user id
     */
    fun getAllEvents(userId: Long): List<AccountEventDataKeeper>

    /**
     * @return saved AccountEventDataKeeper with generated id from storage.
     */
    fun save(event: AccountEventDataKeeper): AccountEventDataKeeper
}
