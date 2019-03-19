package io.pleo.antaeus.core.services

import io.pleo.antaeus.core.exceptions.CurrencyMismatchException
import io.pleo.antaeus.core.exceptions.CustomerNotFoundException
import io.pleo.antaeus.core.exceptions.NetworkException
import io.pleo.antaeus.core.external.PaymentProvider
import io.pleo.antaeus.models.Invoice
import io.pleo.antaeus.models.InvoiceStatus
import org.apache.logging.log4j.kotlin.Logging

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

    /**
     * This function handles the payment of the invoices.
     *
     * The function gets the invoices to handle (NETWORK_ERROR, ERROR, PENDING) and process each invoice by calling
     * processInvoice. This function also logs a resume of the actions that were done during the handling of the invoice.
     */
    fun handlePayments(){

        logger.info("Entering the HandlePayments function")

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

        var result : Invoice?

        //We process each individual invoice
        for(invoice in pendingInvoices){

            //We store the invoices to retry. Since we might have updated the invoice inside the function,
            // it returns null if the transaction was a success, or it returns the Invoice element that we
            // need to retry on
            result = processInvoice(invoice)
            if(result != null){
                invoicesToRetry.add(result)
            }
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
    private fun processInvoice(invoice: Invoice): Invoice?{

        logger.debug{"Processing invoice: Id  ${invoice.id} customerId ${invoice.customerId} "+
                "amount ${invoice.amount.value} currency ${invoice.amount.currency} Status ${invoice.status}"}

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

        //We want to retry those that had network errors, so we return the invoice if it needs to be retried, otherwise we return null
        return if ( status == InvoiceStatus.NETWORK_ERROR) updatedInvoice else null

    }
}