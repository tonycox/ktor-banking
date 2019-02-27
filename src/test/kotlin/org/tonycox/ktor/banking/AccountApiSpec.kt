package org.tonycox.ktor.banking

import ch.vorburger.mariadb4j.DB
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty
import io.restassured.RestAssured
import io.restassured.http.ContentType
import io.restassured.response.Response
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.transactions.transaction
import org.koin.standalone.StandAloneContext.startKoin
import org.spekframework.spek2.Spek
import org.spekframework.spek2.style.specification.describe
import org.tonycox.ktor.banking.account.api.*
import org.tonycox.ktor.banking.account.repository.AccountEventDao
import org.tonycox.ktor.banking.account.repository.AccountEventTable
import org.tonycox.ktor.banking.account.service.*
import org.tonycox.ktor.banking.account.toJodaDate
import java.math.BigDecimal
import java.time.LocalDateTime
import kotlin.test.assertEquals
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

private fun init(database: Database) {
    transaction(db = database) {
        SchemaUtils.drop(AccountEventTable)
        SchemaUtils.create(AccountEventTable)
    }
}

object AccountApiSpec : Spek({
    describe("an account service") {
        val koinContext = startKoin(listOf(testDependencies)).koinContext
        val database = koinContext.get<Database>(clazz = Database::class)
        val embeddedServer = embeddedServer(Netty, port) { accountModule() }
        embeddedServer.start()
        RestAssured.port = port

        val userId = 1L
        val destinationUserId = 2L

        afterGroup {
            embeddedServer.stop(0L, 0L, TimeUnit.SECONDS)
            koinContext.get<DB>(clazz = DB::class).stop()
        }

        context("with zero balance") {
            beforeEach {
                init(database)
            }
            val service by memoized { koinContext.get<AccountService>(clazz = AccountService::class) }
            it("doesn't handle withdraw and transfer events") {
                val withdrawResponse = withdraw(userId, BigDecimal.TEN).statusCode
                val transferResponse = transfer(destinationUserId, userId, BigDecimal.TEN).statusCode

                val events = service.getAllEvents(userId)

                assertEquals(0, events.size)
                assertEquals(400, withdrawResponse)
                assertEquals(400, transferResponse)
            }
            it("return statements after applying common flow") {
                deposit(userId, BigDecimal.TEN)
                withdraw(userId, BigDecimal.valueOf(3))
                deposit(userId, BigDecimal.valueOf(0.02))
                transfer(destinationUserId, userId, BigDecimal.valueOf(2.2))
                val statements: List<*> = RestAssured.get("$userId/statement")
                    .body.`as`<List<*>>(List::class.java) as List<*>
                assertEquals(4, statements.size)
            }
            it("doesn't handle any event when event has nothing") {
                val depositResponse = deposit(userId, BigDecimal.ZERO).statusCode
                val events = service.getAllEvents(userId)
                assertEquals(400, depositResponse)
                assertEquals(0, events.size)
            }
            it("doesn't handle any event with too big scale") {
                val depositResponse = deposit(userId, BigDecimal.valueOf(0.000001)).statusCode
                val events = service.getAllEvents(userId)
                assertEquals(400, depositResponse)
                assertEquals(0, events.size)
            }
            it("doesn't handle any event with wrong input json") {
                val depositResponse = RestAssured.given()
                    .contentType(ContentType.JSON)
                    .body(TransferRequest(destinationUserId, BigDecimal.TEN))
                    .`when`()
                    .post("/$userId/deposit")
                val events = service.getAllEvents(userId)
                assertEquals(406, depositResponse.statusCode)
                assertEquals(0, events.size)
            }
        }

        context("with preset deposit event") {
            beforeEach {
                init(database)
                transaction(db = database) {
                    AccountEventDao.new {
                        this.userId = userId
                        this.amount = BigDecimal.valueOf(100L)
                        this.eventType = EventType.DEPOSIT
                        this.date = LocalDateTime.now().toJodaDate()
                    }
                }
            }
            val service by memoized { koinContext.get<AccountService>(clazz = AccountService::class) }
            it("creates another event for destination users when handle transfer event") {
                val transferResponse = transfer(destinationUserId, userId, BigDecimal.TEN).statusCode
                val events = service.getAllEvents(userId)
                assertEquals(202, transferResponse)
                assertEquals(2, events.size)
                val destinationUserEvents = service.getAllEvents(destinationUserId)
                assertEquals(1, destinationUserEvents.size)
            }
            it("has proper balance with concurrency withdraw") {
                val latch = CountDownLatch(10)
                val amount = BigDecimal.valueOf(20L)

                (0..9).map {
                    GlobalScope.launch {
                        withdraw(userId, amount)
                        latch.countDown()
                    }
                }
                latch.await()

                val balance = RestAssured.get("$userId/balance")
                    .body.jsonPath().get<Number>("amount").toDouble()
                assertEquals(0.toDouble(), balance)
            }
        }
    }
})

private fun withdraw(userId: Long, amount: BigDecimal): Response {
    return RestAssured.given()
        .contentType(ContentType.JSON)
        .body(WithdrawRequest(amount))
        .`when`()
        .post("/$userId/withdraw")
}

private fun transfer(destinationUserId: Long, userId: Long, amount: BigDecimal): Response {
    return RestAssured.given()
        .contentType(ContentType.JSON)
        .body(TransferRequest(destinationUserId, amount))
        .`when`()
        .post("/$userId/transfer")
}

private fun deposit(id: Long, amount: BigDecimal): Response {
    return RestAssured.given()
        .contentType(ContentType.JSON)
        .body(DepositRequest(amount))
        .`when`()
        .post("/$id/deposit")
}
