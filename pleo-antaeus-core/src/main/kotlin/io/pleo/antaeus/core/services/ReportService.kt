package io.pleo.antaeus.core.services

import io.pleo.antaeus.models.Invoice
import io.pleo.antaeus.models.InvoiceStatus.*
import org.apache.logging.log4j.kotlin.Logging

/**
 * This service is responsible for keeping track of the results from invoice processing
 *
 * The idea would be to extend this class in order to send notifications, either by email or SMS when there are errors.
 */
class ReportService : Logging {

    private val pendingInvoices: MutableList<Invoice> = ArrayList()
    private val paidInvoices : MutableList<Invoice> = ArrayList()
    private val networkErrorInvoices: MutableList<Invoice> = ArrayList()
    private val missingFundsInvoices: MutableList<Invoice> = ArrayList()
    private val errorInvoices: MutableList<Invoice> = ArrayList()
    private var networkErrorCount: Int = 0
    private var currencyUpdateCount: Int = 0

    /**
     * Initialize the service - We clear all the information
     */
    fun initialize(){
        pendingInvoices.clear()
        paidInvoices.clear()
        networkErrorInvoices.clear()
        missingFundsInvoices.clear()
        errorInvoices.clear()
        networkErrorCount = 0
        currencyUpdateCount = 0
    }

    /**
     * Add an invoice to the corresponding list depending on the status
     */
    fun addInvoice(invoice: Invoice){

        when(invoice.status){
            PENDING -> pendingInvoices.add(invoice)
            PAID -> paidInvoices.add(invoice)
            NETWORK_ERROR -> networkErrorInvoices.add(invoice)
            MISSING_FUNDS -> missingFundsInvoices.add(invoice)
            ERROR -> errorInvoices.add(invoice)
        }
    }

    /**
     * Increase the value of networkErrorCount
     */
    fun increaseNetworkErrorCount(){
        networkErrorCount++
    }

    /**
     * Increase the value of currencyUpdateCount
     */
    fun increaseCurrencyUpdateCount(){
        currencyUpdateCount++
    }

    /**
     *
     */
    fun debugReport(){

        val pendingInvoiceCount = pendingInvoices.count()
        val paidInvoiceCount = paidInvoices.count()
        val networkErrorFinalInvoiceCount = networkErrorInvoices.count()
        val missingFundsInvoiceCount = missingFundsInvoices.count()
        val errorInvoiceCount = errorInvoices.count()
        val totalCount = pendingInvoiceCount+paidInvoiceCount+networkErrorFinalInvoiceCount+missingFundsInvoiceCount+errorInvoiceCount

        logger.info ("**********************************************************************************************************")
        logger.info("Finished invoice handling. Here are the final numbers:")
        logger.info("We handled a total of $totalCount invoices.")
        logger.info("A total of $paidInvoiceCount were paid")
        logger.info("A total of $networkErrorCount invoices had network errors while processing and were retried."
                +" ($networkErrorFinalInvoiceCount of them had network issues again during retry)")
        logger.info("A total of $errorInvoiceCount invoices had consumer or currency errors while processing.")
        logger.info("A total of $missingFundsInvoiceCount invoices weren't paid because of low costumer balance.")
        logger.info("A total of $currencyUpdateCount invoices had their currency updated to match the customer's currency")
    }



}