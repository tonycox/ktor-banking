package org.tonycox.ktor.banking.account.api

import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.databind.exc.UnrecognizedPropertyException
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import io.ktor.application.Application
import io.ktor.application.ApplicationCall
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
import io.ktor.routing.route
import io.ktor.routing.routing
import io.ktor.util.pipeline.PipelineContext
import org.koin.ktor.ext.inject
import org.tonycox.ktor.banking.account.service.*
import java.time.LocalDateTime

/**
 * Module for creating account related routes and application features in ktor environment.
 */
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
        exception<ValidationException> { exception ->
            call.respond(HttpStatusCode.BadRequest, exception.localizedMessage)
        }
        exception<UnrecognizedPropertyException> {
            call.respond(HttpStatusCode.NotAcceptable, "Input message cannot be parsed.")
        }
    }
    routing {
        val service: AccountService by inject()
        route("/{userId}") {
            get("/balance") {
                val projection = service.reduceEventsToBalance(extractUserId())
                call.respond(HttpStatusCode.OK, BalanceDto(projection.amount))
            }
            get("/statement") {
                val list = service.getAllEvents(extractUserId())
                    .map { event -> event.toDto() }
                call.respond(HttpStatusCode.OK, list)
            }
            post("/deposit") {
                val request = call.receive<DepositRequest>()
                service.handle(request.toEvent(extractUserId()))
                call.respond(HttpStatusCode.Accepted)
            }
            post("/withdraw") {
                val request = call.receive<WithdrawRequest>()
                service.handle(request.toEvent(extractUserId()))
                call.respond(HttpStatusCode.Accepted)
            }
            post("/transfer") {
                val request = call.receive<TransferRequest>()
                service.handle(request.toEvent(extractUserId()))
                call.respond(HttpStatusCode.Accepted)
            }
        }
    }
}

private fun PipelineContext<Unit, ApplicationCall>.extractUserId() =
    call.parameters["userId"]?.toLong() ?: throw NoSuchElementException("Parameter userId not found")

private fun AccountEvent.toDto() = StatementDto(this.amount, this.eventType, this.date)

private fun DepositRequest.toEvent(userId: Long): AccountEvent {
    return AccountEvent(userId, amount = this.amount, eventType = EventType.DEPOSIT, date = LocalDateTime.now())
}

private fun WithdrawRequest.toEvent(userId: Long): AccountEvent {
    return AccountEvent(userId, amount = this.amount, eventType = EventType.WITHDRAW, date = LocalDateTime.now())
}

private fun TransferRequest.toEvent(originUserId: Long): TransferAccountEvent {
    return TransferAccountEvent(originUserId, this.amount, EventType.TRANSFER_OUT, LocalDateTime.now(), this.userId)
}
