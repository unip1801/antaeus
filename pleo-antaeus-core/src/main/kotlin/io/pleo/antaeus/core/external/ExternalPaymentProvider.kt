package io.pleo.antaeus.core.external

import io.pleo.antaeus.core.exceptions.CurrencyMismatchException
import io.pleo.antaeus.core.exceptions.NetworkException
import io.pleo.antaeus.core.services.CustomerService
import io.pleo.antaeus.models.Invoice
import kotlin.random.Random

/**
 * This class is an implementation of the PaymentProvider, based on the object found initially on utils.kt
 *
 * The reasoning behind extracting the object into a full class was to be able to pass the customerService instance
 * in order to be able to throw the CurrencyMismatchException when currencies don't match
 *
 * The base function was also improved to throw NetworkException from time to time in order to simulate the issue.
 */
class ExternalPaymentProvider(private val customerService: CustomerService) : PaymentProvider{

    override fun charge(invoice: Invoice): Boolean {

        //Compare the invoice currency with customer's currency
        val customer = customerService.fetch(invoice.customerId)
        if(customer.currency != invoice.amount.currency){
            throw CurrencyMismatchException(invoice.id,invoice.customerId)
        }

        //We randomly throw the NetworkException
        if(Random.nextInt(from=1,until = 10) % 7 == 0){
            throw NetworkException()
        }

        //Return random success or failure
        return (Random.nextInt(from=1,until = 10) % 7 !=0)
    }

}
