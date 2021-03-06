/*
    Defines the main() entry point of the app.
    Configures the database and sets up the REST web service.
 */

@file:JvmName("AntaeusApp")

package io.pleo.antaeus.app

import getPaymentProvider
import io.pleo.antaeus.core.services.*
import io.pleo.antaeus.data.AntaeusDal
import io.pleo.antaeus.data.CustomerTable
import io.pleo.antaeus.data.InvoiceTable
import io.pleo.antaeus.rest.AntaeusRest
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.StdOutSqlLogger
import org.jetbrains.exposed.sql.addLogger
import org.jetbrains.exposed.sql.transactions.TransactionManager
import org.jetbrains.exposed.sql.transactions.transaction
import setupInitialData
import java.sql.Connection

fun main() {
    // The tables to create in the database.
    val tables = arrayOf(InvoiceTable, CustomerTable)

    // Connect to the database and create the needed tables. Drop any existing data.
    val db = Database
        .connect("jdbc:sqlite:/tmp/data.db", "org.sqlite.JDBC")
        .also {
            TransactionManager.manager.defaultIsolationLevel = Connection.TRANSACTION_SERIALIZABLE
            transaction(it) {
                addLogger(StdOutSqlLogger)
                // Drop all existing tables to ensure a clean slate on each run
                SchemaUtils.drop(*tables)
                // Create all tables
                SchemaUtils.create(*tables)
            }
        }

    // Set up data access layer.
    val dal = AntaeusDal(db = db)

    // Insert example data in the database.
    setupInitialData(dal = dal)


    // Create core services
    val invoiceService = InvoiceService(dal = dal)
    val customerService = CustomerService(dal = dal)
    val currencyService = CurrencyService()
    val reportService = ReportService()


    // Get third parties
    // Note: We pass the customerService to the external as I want the mock to throw the CurrencyMismatchException
    //      when the currencies don't match
    val paymentProvider = getPaymentProvider(customerService)



    // This is _your_ billing service to be included where you see fit
    val billingService = BillingService(paymentProvider = paymentProvider,
            customerService = customerService,invoiceService = invoiceService, currencyService = currencyService,
            reportService = reportService)

    val schedulingService = SchedulingService(billingService)

    //Start the scheduling service
    schedulingService.start()

    // Create REST web service
    AntaeusRest(
        invoiceService = invoiceService,
        customerService = customerService,
        billingService = billingService,
        schedulingService = schedulingService
    ).run()
}

