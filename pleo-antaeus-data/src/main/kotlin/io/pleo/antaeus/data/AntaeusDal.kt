/*
    Implements the data access layer (DAL).
    This file implements the database queries used to fetch and insert rows in our database tables.

    See the `mappings` module for the conversions between database rows and Kotlin objects.
 */

package io.pleo.antaeus.data

import io.pleo.antaeus.models.Currency
import io.pleo.antaeus.models.Customer
import io.pleo.antaeus.models.Invoice
import io.pleo.antaeus.models.InvoiceStatus
import io.pleo.antaeus.models.Money
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.transactions.transaction

class AntaeusDal(private val db: Database) {
    fun fetchInvoice(id: Int): Invoice? {
        // transaction(db) runs the internal query as a new database transaction.
        return transaction(db) {
            // Returns the first invoice with matching id.
            InvoiceTable
                .select { InvoiceTable.id.eq(id) }
                .firstOrNull()
                ?.toInvoice()
        }
    }

    fun fetchInvoices(): List<Invoice> {
        return transaction(db) {
            InvoiceTable
                .selectAll()
                .map { it.toInvoice() }
        }
    }

    fun fetchInvoices(status: InvoiceStatus): List<Invoice>{
        return transaction(db) {
            InvoiceTable
                    .select{InvoiceTable.status eq status.toString()}
                    .map { it.toInvoice() }
        }
    }

    fun fetchInvoicesToHandle():List<Invoice>{
        return transaction(db) {
            InvoiceTable
                    .select{InvoiceTable.status.eq(InvoiceStatus.NETWORK_ERROR.toString()) or
                            InvoiceTable.status.eq(InvoiceStatus.PENDING.toString()) or
                            InvoiceTable.status.eq(InvoiceStatus.ERROR.toString())
                    }
                    .map { it.toInvoice() }
        }
    }

    fun fetchInvoiceCount(status: InvoiceStatus): Int{
        return transaction(db){
            InvoiceTable
                    .select{InvoiceTable.status eq status.toString()}
                    .count()
        }
    }
    fun fetchInvoiceCount(): Int{
        return transaction(db){
            InvoiceTable.
                    selectAll()
                    .count()
        }
    }



    fun createInvoice(amount: Money, customer: Customer, status: InvoiceStatus = InvoiceStatus.PENDING): Invoice? {
        val id = transaction(db) {
            // Insert the invoice and returns its new id.
            InvoiceTable
                .insert {
                    it[this.value] = amount.value
                    it[this.currency] = amount.currency.toString()
                    it[this.status] = status.toString()
                    it[this.customerId] = customer.id
                } get InvoiceTable.id
        }

        return fetchInvoice(id!!)
    }

    /**
     * Function to update an invoice on the DB.
     * Only the attributes value, currency and status are updated, as the attributes id and customerId should never change
     */
    fun updateInvoice(invoice: Invoice){
        transaction(db){
            InvoiceTable.update({InvoiceTable.id eq invoice.id}){
                it[this.value] = invoice.amount.value
                it[this.currency] = invoice.amount.currency.toString()
                it[this.status] = invoice.status.toString()
            }
        }
    }

    fun fetchCustomer(id: Int): Customer? {
        return transaction(db) {
            CustomerTable
                .select { CustomerTable.id.eq(id) }
                .firstOrNull()
                ?.toCustomer()
        }
    }

    fun fetchCustomers(): List<Customer> {
        return transaction(db) {
            CustomerTable
                .selectAll()
                .map { it.toCustomer() }
        }
    }

    fun createCustomer(currency: Currency): Customer? {
        val id = transaction(db) {
            // Insert the customer and return its new id.
            CustomerTable.insert {
                it[this.currency] = currency.toString()
            } get CustomerTable.id
        }

        return fetchCustomer(id!!)
    }
}
