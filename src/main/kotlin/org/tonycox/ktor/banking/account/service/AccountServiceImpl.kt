package org.tonycox.ktor.banking.account.service

import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.transactions.transaction
import org.tonycox.ktor.banking.account.repository.AccountEventDataKeeper
import org.tonycox.ktor.banking.account.repository.AccountEventRepository
import java.math.BigDecimal

class AccountServiceImpl(
    private val database: Database,
    private val repository: AccountEventRepository
) : AccountService {

    override fun reduceEventsToBalance(userId: Long): BalanceProjection {
        return transaction(db = database) {
            val list = repository.getAllEvents(userId)
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
            repository.getAllEvents(userId)
                .map {
                    AccountEvent(it.userId, it.amount, it.eventType, it.date)
                }
        }
    }

    override fun handle(event: AccountEvent) {
        transaction(db = database) {
            validate(event)
            val keeper = AccountEventDataKeeper(
                event.userId, event.amount, event.eventType, event.date
            )
            repository.save(keeper)
            if (event is TransferAccountEvent) {
                val transKeeper = AccountEventDataKeeper(
                    event.destinationUserId, event.amount, EventType.TRANSFER_IN, event.date
                )
                repository.save(transKeeper)
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
