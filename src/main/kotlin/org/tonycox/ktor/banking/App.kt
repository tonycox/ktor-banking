package org.tonycox.ktor.banking

import io.ktor.server.engine.commandLineEnvironment
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty
import org.jetbrains.exposed.sql.Database
import org.koin.dsl.module.module
import org.koin.standalone.StandAloneContext.startKoin
import org.tonycox.ktor.banking.account.service.AccountService
import org.tonycox.ktor.banking.account.service.AccountServiceImpl

val dependencies = module {
    factory<AccountService> { AccountServiceImpl(get()) }
    single {
        Database.connect(
            "jdbc:h2:mem:dev;DB_CLOSE_DELAY=-1;",
            user = "sa",
            password = "",
            driver = "org.h2.Driver"
        )
    }
}

fun main(args: Array<String>) {
    startKoin(listOf(dependencies))
    embeddedServer(Netty, commandLineEnvironment(args)).start(wait = true)
}
