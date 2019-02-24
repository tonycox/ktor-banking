package org.tonycox.ktor.banking.account.api

import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import io.ktor.application.Application
import io.ktor.application.call
import io.ktor.application.install
import io.ktor.features.CallLogging
import io.ktor.features.ContentNegotiation
import io.ktor.features.DefaultHeaders
import io.ktor.features.StatusPages
import io.ktor.http.HttpStatusCode
import io.ktor.jackson.jackson
import io.ktor.request.receive
import io.ktor.response.respond
import io.ktor.routing.get
import io.ktor.routing.post
import io.ktor.routing.routing
import org.koin.ktor.ext.inject
import org.tonycox.ktor.banking.account.service.AccountEvent
import org.tonycox.ktor.banking.account.service.AccountService
import org.tonycox.ktor.banking.account.service.EventType
import org.tonycox.ktor.banking.account.service.ValidationException
import java.time.LocalDateTime

fun Application.accountModule() {
    install(DefaultHeaders)
    install(CallLogging)
    install(ContentNegotiation) {
        jackson {
            enable(SerializationFeature.INDENT_OUTPUT)
            disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
            registerModule(JavaTimeModule())
        }
    }
    install(StatusPages) {
        exception<ValidationException> {
            call.respond(HttpStatusCode.BadRequest, "Not valid operation")
        }
    }
    routing {
        val service: AccountService by inject()
        get("/{userId}/balance") {
            val userId = call.parameters["userId"] ?: throw NoSuchElementException("Parameter userId not found")
            val projection = service.reduceEventsToBalance(userId.toLong())
            call.respond(HttpStatusCode.OK, BalanceDto(projection.amount))
        }
        get("/{userId}/statement") {
            val userId = call.parameters["userId"] ?: throw NoSuchElementException("Parameter userId not found")
            val list = service.getAllEvents(userId.toLong())
                .map { event -> StatementDto(event.amount, event.date, event.eventType) }
            call.respond(HttpStatusCode.OK, list)
        }
        post("/{userId}/deposit") {
            val request = call.receive<DepositRequest>()
            val userId = call.parameters["userId"] ?: throw NoSuchElementException("Parameter userId not found")
            service.handle(request.toEvent(userId.toLong()))
            call.respond(HttpStatusCode.Accepted)
        }
        post("/{userId}/withdraw") {
            val request = call.receive<WithdrawRequest>()
            val userId = call.parameters["userId"] ?: throw NoSuchElementException("Parameter userId not found")
            service.handle(request.toEvent(userId.toLong()))
            call.respond(HttpStatusCode.Accepted)
        }
        post("/{userId}/transfer") {
            val request = call.receive<TransferRequest>()
            val userId = call.parameters["userId"] ?: throw NoSuchElementException("Parameter userId not found")
            service.handle(request.toEvent(userId.toLong()))
            call.respond(HttpStatusCode.Accepted)
        }
    }
}

private fun DepositRequest.toEvent(userId: Long): AccountEvent {
    return AccountEvent(userId, amount = this.amount, eventType = EventType.DEPOSIT, date = LocalDateTime.now())
}

private fun WithdrawRequest.toEvent(userId: Long): AccountEvent {
    return AccountEvent(userId, amount = this.amount, eventType = EventType.WITHDRAW, date = LocalDateTime.now())
}

private fun TransferRequest.toEvent(originUserId: Long): AccountEvent {
    return AccountEvent(originUserId, this.userId, this.amount, EventType.TRANSFER_OUT, LocalDateTime.now())
}
