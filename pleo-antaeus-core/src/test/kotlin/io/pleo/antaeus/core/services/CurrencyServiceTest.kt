package io.pleo.antaeus.core.services

import io.pleo.antaeus.models.Currency
import io.pleo.antaeus.models.Invoice
import io.pleo.antaeus.models.InvoiceStatus
import io.pleo.antaeus.models.Money
import org.junit.jupiter.api.Test
import java.math.BigDecimal


class CurrencyServiceTest {

    private val customerService = CurrencyService()

    @Test
    fun `invoice is not modified if currencies match`(){
        val invoice = Invoice(1,1, Money(BigDecimal(10.0),Currency.SEK),InvoiceStatus.PENDING)

        val invoiceOutput = customerService.invoiceCurrencyConversion(invoice, Currency.SEK)
        assert(invoice == invoiceOutput)
    }

    @Test
    fun `invoice is converted from SEK to USD`(){

        val invoiceValue = BigDecimal(10.0)

        val invoice = Invoice(1,1, Money(invoiceValue,Currency.SEK),InvoiceStatus.PENDING)

        val invoiceOutput = customerService.invoiceCurrencyConversion(invoice,Currency.USD)

        assert(invoiceOutput.amount.value == (invoiceValue * BigDecimal(0.11)))

        assert(invoiceOutput.amount.currency == Currency.USD)

    }

    @Test
    fun `invoice is converted from USD to EUR`(){

        val invoiceValue = BigDecimal(15.0)

        val invoice = Invoice(1,1, Money(invoiceValue,Currency.USD),InvoiceStatus.PENDING)

        val invoiceOutput = customerService.invoiceCurrencyConversion(invoice,Currency.EUR)

        assert(invoiceOutput.amount.value == (invoiceValue / BigDecimal(1.13)))

        assert(invoiceOutput.amount.currency == Currency.EUR)
    }


    @Test
    fun `invoice is converted from GBP to DKK`(){

        val invoiceValue = BigDecimal(197.0)

        val invoice = Invoice(1,1, Money(invoiceValue,Currency.GBP),InvoiceStatus.PENDING)

        val invoiceOutput = customerService.invoiceCurrencyConversion(invoice,Currency.DKK)

        assert(invoiceOutput.amount.value == ((invoiceValue * BigDecimal(1.33))/BigDecimal(0.15)))

        assert(invoiceOutput.amount.currency == Currency.DKK)
    }


}