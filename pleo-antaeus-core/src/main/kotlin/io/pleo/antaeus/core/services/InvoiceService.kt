/*
    Implements endpoints related to invoices.
 */

package io.pleo.antaeus.core.services

import io.pleo.antaeus.core.exceptions.InvoiceNotFoundException
import io.pleo.antaeus.data.AntaeusDal
import io.pleo.antaeus.models.Invoice
import io.pleo.antaeus.models.InvoiceStatus

class InvoiceService(private val dal: AntaeusDal) {
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

}
