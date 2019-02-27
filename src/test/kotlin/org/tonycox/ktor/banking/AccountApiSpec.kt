package org.tonycox.ktor.banking

import ch.vorburger.mariadb4j.DB
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty
import io.restassured.RestAssured
import io.restassured.http.ContentType
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
import org.tonycox.ktor.banking.account.repository.AccountEventRepositoryImpl
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
                val withdrawPost = RestAssured.given()
                    .contentType(ContentType.JSON)
                    .body(WithdrawRequest(BigDecimal.TEN))
                    .`when`()
                    .post("/$userId/withdraw")
                    .statusCode
                val transferPost = RestAssured.given()
                    .contentType(ContentType.JSON)
                    .body(TransferRequest(destinationUserId, BigDecimal.TEN))
                    .`when`()
                    .post("/$userId/transfer")
                    .statusCode

                val events = service.getAllEvents(userId)

                assertEquals(0, events.size)
                assertEquals(400, withdrawPost)
                assertEquals(400, transferPost)
            }
        }

        context("when event has nothing") {
            beforeEach {
                init(database)
            }
            val service by memoized { koinContext.get<AccountService>(clazz = AccountService::class) }
            it("doesn't handle any event") {
                val depositPost = RestAssured.given()
                    .contentType(ContentType.JSON)
                    .body(DepositRequest(BigDecimal.ZERO))
                    .`when`()
                    .post("/$userId/deposit")
                    .statusCode
                val events = service.getAllEvents(userId)
                assertEquals(400, depositPost)
                assertEquals(0, events.size)
            }
        }

        context("when handle transfer event") {
            beforeEach {
                init(database)
                transaction(db = database) {
                    AccountEventDao.new {
                        this.userId = userId
                        this.amount = BigDecimal.TEN
                        this.eventType = EventType.DEPOSIT
                        this.date = LocalDateTime.now().toJodaDate()
                    }
                }
            }
            val service by memoized { koinContext.get<AccountService>(clazz = AccountService::class) }
            it("creates another event for destination users") {
                val transferPost = RestAssured.given()
                    .contentType(ContentType.JSON)
                    .body(TransferRequest(destinationUserId, BigDecimal.TEN))
                    .`when`()
                    .post("/$userId/transfer")
                    .statusCode

                val events = service.getAllEvents(userId)
                assertEquals(202, transferPost)
                assertEquals(2, events.size)
                val destinationUserEvents = service.getAllEvents(destinationUserId)
                assertEquals(1, destinationUserEvents.size)
            }
        }

        context("with concurrency withdraw") {
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
            it("has proper balance") {
                val latch = CountDownLatch(10)
                val request = WithdrawRequest(BigDecimal.valueOf(20L))

                (0..9).map {
                    GlobalScope.launch {
                        RestAssured.given()
                            .contentType(ContentType.JSON)
                            .body(request)
                            .`when`()
                            .post("/$userId/withdraw")
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
