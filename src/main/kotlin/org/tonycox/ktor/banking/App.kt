package org.tonycox.ktor.banking

import io.ktor.server.engine.commandLineEnvironment
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty
import org.koin.standalone.StandAloneContext.startKoin

fun main(args: Array<String>) {
    startKoin(listOf(dependencies))
    embeddedServer(Netty, commandLineEnvironment(args)).start(wait = true)
}
