package org.tonycox.ktor.banking.account.service

/**
 * Service to process account events.
 */
interface AccountService {
    /**
     * Reduces all events of certain user in one with summing of amounts.
     * @return reduced amount.
     */
    fun reduceEventsToBalance(userId: Long): BalanceProjection

    /**
     * @return all events related to user id
     */
    fun getAllEvents(userId: Long): List<AccountEvent>

    /**
     * Processing input account event.
     * @exception ValidationException if input event cannot be applied to others.
     */
    fun handle(event: AccountEvent)
}
