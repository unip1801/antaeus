package io.pleo.antaeus.core.services

import io.pleo.antaeus.core.exceptions.CurrencyMismatchException
import io.pleo.antaeus.core.exceptions.CustomerNotFoundException
import io.pleo.antaeus.core.exceptions.NetworkException
import io.pleo.antaeus.core.external.PaymentProvider
import io.pleo.antaeus.core.helpers.DateHelper
import io.pleo.antaeus.models.Invoice
import io.pleo.antaeus.models.InvoiceStatus
import kotlinx.coroutines.*
import org.apache.logging.log4j.kotlin.Logging
import java.util.concurrent.locks.ReentrantLock

/**
 * This service is in charge of handling the billing, so processing the invoices
 *
 * In short, when the function handlePayments gets called, the class pulls all the invoices that should be
 *  handled (it has one of the following statuses: NETWORK_ERROR, ERROR, PENDING). Once we have the list of invoices
 *  to handle, we iterate through them and call the function processInvoice with each invoice.
 *
 *  We then inspect each invoice to ensure it's on the right currency after which we call the external provider to do the payment.
 *
 *  Depending on the external provider result (success, failure or exception), we update the invoice.
 *
 *  For the invoices that had a network error, we retry them once after the first pass.
 *
 *
 *
 *  Note: We don't try to process again the invoices with status "Missing funds" as this represents a charge to the customer.
 *  This case should be manually handled.
 *
 */
class BillingService(
    private val paymentProvider: PaymentProvider, private val invoiceService: InvoiceService,
    private val customerService: CustomerService, private val currencyService: CurrencyService) : Logging{


    private var invoiceCurrencyUpdatedCount = 0
    private var thread : Job? = null
    private var threadShouldRun : Boolean = false

    //Since the BillingService is in a thread separate to the rest of the program, technically someone
    // could try to update an invoice through the REST API at the same time that the BillingService is working
    // On the invoices, so we could end-up requesting two payments for the same invoice. This lock will
    // help us ensure that only the REST API or the BillingService thread is accessing the invoices to prevent
    // concurrent access errors.
    private val invoicesLock = ReentrantLock()


    /**
     * Function to start the BillingService thread.
     *
     * @return true if the thread was started, false if it was already started
     */
    fun start(): Boolean{

        //We start the thread only if it's not already running
        if(thread == null){
            logger.info("Starting the Billing Service")

            threadShouldRun = true

            thread = GlobalScope.launch {
                main()
            }

            return true

        }else{
            logger.info("Cannot start the billing service - It is already running")
            return false
        }

    }

    /**
     * Function to stop the processing of the current thread
     *
     * @return true if we stopped the thread, false if the thread was already stopped
     */
    fun stop() : Boolean {

        if(thread == null){
            return false
        }


        logger.info("About to stop the thread")


        threadShouldRun = false

        //Cancel the thread and join it with the main thread
        thread?.cancel()
        runBlocking {
            thread?.join()
        }

        thread = null

        logger.info("Billing service has been stopped")

        return true
    }

    /**
     * Returns true if the thread is running - Return false if not.
     */
    fun status() = (thread != null)


    /**
     * This is the function being run by the thread, so it must never end unless we want to stop the thread
     *
     *
     */
    private suspend fun main(){

        var delayToFirstOfNextMonth: Long

        //We continue to execute the function unless we want to stop the thread
        while(threadShouldRun()){
            logger.info("Loop - BillingService.Main")

            //We handle the invoices only if we're on the first of the month
            if(DateHelper.isFirstOfTheMonth()){
                logger.info("We are the first of the month - Processing invoices")
                handlePayments()
            }

            //How much seconds we need to sleep until first day of next month
            delayToFirstOfNextMonth = DateHelper.msUntilFirstOfNextMonth()

            logger.info("Next payment date: ${DateHelper.firstOfNextMonth()}")
            logger.info("Milliseconds to sleep until next first of the month: $delayToFirstOfNextMonth")
            logger.info("Putting thread to sleep - Good night!")

            //Put the thread to sleep
            delay(delayToFirstOfNextMonth)

        }

    }


    /**
     * Returns true if the thread should continue running or false if it should end its infinite loop
     *
     * Note: We use a function here instead of directly accessing the attribute threadShouldRun as
     * the logic for this could potentially grow into something more complex than a simple attribute
     */
    private fun threadShouldRun(): Boolean{
        return threadShouldRun
    }

    /**
     * This function handles the payment of the invoices.
     *
     * The function gets the invoices to handle (NETWORK_ERROR, ERROR, PENDING) and process each invoice by calling
     * processInvoice. This function also logs a resume of the actions that were done during the handling of the invoice.
     */
    fun handlePayments() : List<Invoice>{

        logger.info("Entering the HandlePayments function")

        //Getting the lock for the invoices
        invoicesLock.lock()
        logger.info("Got the invoice lock")

        //Get the invoice counts before, so we can do the diff at the end
        val allInvoicesCount = invoiceService.getAllInvoicesCount()
        val paidInvoiceCount = invoiceService.getPaidInvoicesCount()
        val missingFundsInvoiceCount = invoiceService.getMissingFundsInvoicesCount()

        logger.info("There's a total of $allInvoicesCount invoices on the system")


        //First we get all the pending invoices
        val pendingInvoices = invoiceService.fetchInvoicesToHandle()
        val newPendingInvoices = pendingInvoices.count()
        logger.info{"Got the pending invoices. Count: $newPendingInvoices"}

        //We create an empty list to store the invoices that weren't paid because of an issue and must be retried
        val invoicesToRetry = ArrayList<Invoice>()

        //Another empty list, this time to contain all the invoices once modified. We will be returning this
        val updatedInvoices = ArrayList<Invoice>()

        var result : Invoice?

        //We process each individual invoice
        for(invoice in pendingInvoices){

            //We store the invoices to retry. Since we might have updated the invoice inside the function,
            // it returns null if the transaction was a success, or it returns the Invoice element that we
            // need to retry on
            result = processInvoice(invoice)
            if(result.status == InvoiceStatus.NETWORK_ERROR){
                invoicesToRetry.add(result)
            }
            updatedInvoices.add(result)

        }

        logger.info ("**********************************************************************************************************")
        logger.info("Finished first run of invoice handling. There are ${invoicesToRetry.count()} invoices to retry. Starting...")


        //We Retry those invoice transactions that failed before.
        for(invoice in invoicesToRetry) {
            processInvoice(invoice)
        }

        //Get counts again, calculate diff
        val paidInvoiceCountDiff = invoiceService.getPaidInvoicesCount() - paidInvoiceCount
        val networkErrorInvoiceCount = invoicesToRetry.count()
        val networkErrorInvoiceCountFinal =  invoiceService.getNetworkErrorInvoicesCount()
        val errorInvoiceCount = invoiceService.getErrorInvoicesCount()
        val missingFundsInvoiceCountDiff = invoiceService.getMissingFundsInvoicesCount() - missingFundsInvoiceCount

        logger.info ("**********************************************************************************************************")
        logger.info("Finished 2nd run of invoice handling. Here are the final numbers:")
        logger.info("We handled a total of $newPendingInvoices invoices.")
        logger.info("A total of $paidInvoiceCountDiff were paid")
        logger.info("A total of $networkErrorInvoiceCount invoices had network errors while processing and were retried."
                        +" ($networkErrorInvoiceCountFinal of them had network issues again during retry)")
        logger.info("A total of $errorInvoiceCount invoices had consumer or currency errors while processing.")
        logger.info("A total of $missingFundsInvoiceCountDiff invoices weren't paid because of low costumer balance.")
        logger.info("A total of $invoiceCurrencyUpdatedCount invoices had their currency updated to match the customer's currency")


        invoicesLock.unlock()
        logger.debug("Unlocked the invoice lock")

        return updatedInvoices

    }

    /**
     * Public function to handle an individual invoice
     *
     * This will be used by the REST Api to handle invoices
     *
     */
    fun handleInvoice(invoiceId: Int): Invoice
    {
        //Getting the lock for the invoices
        invoicesLock.lock()
        logger.info("Handle individual invoice - got the invoice lock. Id: $invoiceId\"")

        val invoice = processInvoice(invoiceService.fetch(invoiceId))

        //Giving the lock back
        invoicesLock.unlock()
        logger.debug("Unlocked the invoice lock")

        return invoice

    }

    /**
     *Process an individual invoice
     *
     * This function will ensure the invoice is on the right currency (same as costumer), call the payment provider and
     * finally update the invoice depending on the result.
     *
     * @param invoice the invoice to handle
     * @return returns the invoice if we received a network error while processing it
     */
    private fun processInvoice(invoice: Invoice): Invoice{

        logger.debug{"Processing invoice: Id  ${invoice.id} customerId ${invoice.customerId} "+
                "amount ${invoice.amount.value} currency ${invoice.amount.currency} Status ${invoice.status}"}

        //If the invoice is already paid, we don't process it
        if(invoice.status == InvoiceStatus.PAID) return invoice

        //First we get the customer
        val customer = customerService.fetch(invoice.customerId)

        val localInvoice: Invoice

        //We handle the case where the customer's currency doesn't match the invoice
        if(customer.currency != invoice.amount.currency){
            logger.debug("*** Currency mismatch - Customer is in ${customer.currency} and the invoice in ${invoice.amount.currency}")
            localInvoice = currencyService.invoiceCurrencyConversion(invoice,customer.currency)
            invoiceCurrencyUpdatedCount++
        } else{
            logger.debug("*** Currency validation successful")
            localInvoice = invoice
        }

        //Attribute to store the status to later update the invoice
        var status: InvoiceStatus

        //We request the payment provider to process the invoice
        try{
            val transactionResult = paymentProvider.charge(localInvoice)
            logger.debug("--- Transaction completed, result: $transactionResult")

            //If we didn't had any exception, we confirm if the transaction succeeded
            status = when( paymentProvider.charge(localInvoice)){
                true->InvoiceStatus.PAID
                false->InvoiceStatus.MISSING_FUNDS
            }

        }
        //If we get a CurrencyMismatchException (shouldn't really happen since we made the verification before
        //  processing the transaction, however we catch the error to retry again.
        // NOTE: I was hesitant between setting it to pending in order to retry again (could turn into infinite loop)
        // or set it to error so "it get's handled manually". This would be answered by having more details on why this
        // could happen in reality.
        catch (e: CurrencyMismatchException){
            status = InvoiceStatus.ERROR
            logger.debug("--- Got currency mismatch error - To handle manually")
        }
        //This should not happen either as we have also retrieved the customer ourselves
        //We set the transaction to error so it can be handled manually
        catch (e:CustomerNotFoundException){
            status = InvoiceStatus.ERROR
            logger.debug("--- Got customer not found error - To handle manually")
        }
        // If there was a network error we capture it so we can retry again later
        catch (e:NetworkException){
            status = InvoiceStatus.NETWORK_ERROR
            logger.debug("--- Network error - To be retried")
        }


        logger.debug("~~ Updating the invoice, new status: $status")
        //We create a new invoice to call the update
        val updatedInvoice = Invoice(localInvoice.id,localInvoice.customerId,localInvoice.amount,status)

        //Save the updates to the DB
        invoiceService.updateInvoice(updatedInvoice)

        logger.debug("---------------------------------------------------------")

        //Return the latest version of the invoice.
        return updatedInvoice

    }
}