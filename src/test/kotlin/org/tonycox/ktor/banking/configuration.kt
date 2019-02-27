package org.tonycox.ktor.banking

import ch.vorburger.mariadb4j.DB
import ch.vorburger.mariadb4j.DBConfigurationBuilder
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.transactions.transaction
import org.koin.dsl.module.module
import org.tonycox.ktor.banking.account.repository.AccountEventRepository
import org.tonycox.ktor.banking.account.repository.AccountEventRepositoryImpl
import org.tonycox.ktor.banking.account.repository.AccountEventTable
import org.tonycox.ktor.banking.account.service.AccountService
import org.tonycox.ktor.banking.account.service.AccountServiceImpl

internal const val port = 15808
internal val testDependencies = module {
    single<AccountEventRepository> { AccountEventRepositoryImpl() }
    single<AccountService> { AccountServiceImpl(get(), get()) }
    single {
        val configBuilder = DBConfigurationBuilder.newBuilder()
        val db = DB.newEmbeddedDB(configBuilder.build())
        db.start()
        db.createDB("test")
        db
    }
    single(createOnStart = true) {
        val db: DB = get()
        val database = Database.connect(
            db.configuration.getURL("test"),
            user = "root",
            password = "",
            driver = "org.mariadb.jdbc.Driver"
        )
        transaction(db = database) {
            SchemaUtils.create(AccountEventTable)
        }
        database
    }
}
