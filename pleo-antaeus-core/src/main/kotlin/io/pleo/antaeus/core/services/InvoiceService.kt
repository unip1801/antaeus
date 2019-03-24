/*
    Implements endpoints related to invoices.
 */

package io.pleo.antaeus.core.services

import io.pleo.antaeus.core.exceptions.InvoiceNotFoundException
import io.pleo.antaeus.data.AntaeusDal
import io.pleo.antaeus.models.Invoice
import io.pleo.antaeus.models.InvoiceStatus
import org.apache.logging.log4j.kotlin.Logging
import java.lang.Exception

class InvoiceService(private val dal: AntaeusDal)  : Logging {
    fun fetchAll(): List<Invoice> {
       return dal.fetchInvoices()
    }

    fun fetch(id: Int): Invoice {
        return dal.fetchInvoice(id) ?: throw InvoiceNotFoundException(id)
    }

    fun fetchPending():List<Invoice>{
        return dal.fetchInvoices(InvoiceStatus.PENDING)
    }

    fun fetchInvoicesToHandle():List<Invoice>{
        return dal.fetchInvoicesToHandle()
    }

    fun updateInvoice(invoice: Invoice){
        dal.updateInvoice(invoice)
    }

    fun getAllInvoicesCount(): Int{
        return dal.fetchInvoiceCount()
    }

    fun fetchInvoicesByStatus(status : InvoiceStatus): List<Invoice>{
        return dal.fetchInvoices(status)
    }

    /**
     * Function to get invoices by their status - Using a string as an input
     *
     * The function will try to get the InvoiceStatus that corresponds to the string, then fetch the invoices
     * with that status. An empty list will be returned if the InvoiceStatus is incorrect
     *
     * @return List of invoices fetched by filtering by status
     */
    fun fetchInvoicesByStatusStr(status: String): List<Invoice>{

        var invoices : List<Invoice>

        try {
            val st = InvoiceStatus.valueOf(status)
            invoices =  fetchInvoicesByStatus(st)
        }catch(e: Exception){

            logger.error("Tried to fetch invoices by non existent status: $status")

            invoices = ArrayList<Invoice>()
        }

        return invoices

    }

    fun getPendingInvoicesCount(): Int{
        return dal.fetchInvoiceCount(InvoiceStatus.PENDING)
    }

    fun getErrorInvoicesCount(): Int{
        return dal.fetchInvoiceCount(InvoiceStatus.ERROR)
    }

    fun getNetworkErrorInvoicesCount(): Int{
        return dal.fetchInvoiceCount(InvoiceStatus.NETWORK_ERROR)
    }

    fun getMissingFundsInvoicesCount(): Int{
        return dal.fetchInvoiceCount(InvoiceStatus.MISSING_FUNDS)
    }

    fun getPaidInvoicesCount(): Int{
        return dal.fetchInvoiceCount(InvoiceStatus.PAID)
    }

    /**
     * Function to change the status of all invoices in error to pending
     */
    fun resetAllErrorsToPending(){
        dal.resetAllInvoiceErrorsToPending()
    }

}
