package org.tonycox.ktor.banking

import io.ktor.server.engine.commandLineEnvironment
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty
import org.koin.standalone.StandAloneContext.startKoin

/**
 * Entry point of the application.
 * It initializes dependencies and starts embedded netty server.
 */
fun main(args: Array<String>) {
    startKoin(listOf(dependencies))
    embeddedServer(Netty, commandLineEnvironment(args)).start(wait = true)
}
