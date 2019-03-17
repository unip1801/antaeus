package io.pleo.antaeus.core.services

import io.pleo.antaeus.core.exceptions.CurrencyMismatchException
import io.pleo.antaeus.core.exceptions.CustomerNotFoundException
import io.pleo.antaeus.core.exceptions.NetworkException
import io.pleo.antaeus.core.external.PaymentProvider
import io.pleo.antaeus.models.Invoice
import io.pleo.antaeus.models.InvoiceStatus
import org.apache.logging.log4j.kotlin.Logging


class BillingService(
    private val paymentProvider: PaymentProvider, private val invoiceService: InvoiceService, private val customerService: CustomerService
) : Logging{

    fun handlePayments(){

        logger.info("Entering the HandlePayments function")

        //First we get all the pending invoices
        val pendingInvoices = invoiceService.fetchPending()
        logger.info{"Got the pending invoices. Count: ${pendingInvoices.count()}"}

        for(invoice in pendingInvoices){
            processInvoice(invoice)
        }

    }

    private fun processInvoice(invoice: Invoice): Boolean{

        logger.debug{"Processing invoice: Id  ${invoice.id} customerId ${invoice.customerId} "+
                "amount ${invoice.amount.value} currency ${invoice.amount.currency} Status ${invoice.status}"}

        //First we get the customer
        val customer = customerService.fetch(invoice.customerId)

        var localInvoice: Invoice

        //We handle the case where the customer's currency doesn't match the invoice
        if(customer.currency != invoice.amount.currency){
            logger.debug("*** Currency mismatch - Customer is in ${customer.currency} and the invoice in ${invoice.amount.currency}")
            localInvoice = invoice //TODO: helper to do a currency correction on the invoice
        } else{
            logger.debug("*** Currency validation successful")
            localInvoice = invoice
        }

        //Attribute to store the status to later update the invoice
        var status: InvoiceStatus

        //We request the payment provider to process the invoice
        try{
            logger.debug("--- Requesting payment to the provider")
            val transactionResult = paymentProvider.charge(localInvoice)
            logger.debug("--- Transaction completed, result: $transactionResult")

            //If we didn't had any exception, we confirm if the transaction succeeded
            when( paymentProvider.charge(localInvoice)){
                true-> status = InvoiceStatus.PAID
                false-> status = InvoiceStatus.MISSING_FUNDS
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

        //We return true unless there was a network error (in which case we should retry after processing other transactions
        return ( status != InvoiceStatus.NETWORK_ERROR)

    }
}