package io.pleo.antaeus.core.services
import io.pleo.antaeus.models.Currency
import io.pleo.antaeus.models.Invoice
import io.pleo.antaeus.models.Money
import java.math.BigDecimal


/**
 * This service is used to do currency conversion on invoices
 *
 * In order to have the simplest solution possible, the class keeps the conversion multipliers from each
 * currency to USD, since we use USD as the base for the adjustments.
 *
 */
class CurrencyService {


    private val eurValue = BigDecimal(1.13)
    private val dkkValue = BigDecimal(0.15)
    private val sekValue = BigDecimal(0.11)
    private val gbpValue = BigDecimal(1.33)


    /**
     * @param invoice The invoice that we want to update
     * @param currency the currency that the invoice should be in
     * @return a new invoice updated with the proper currency and value
     */
    fun invoiceCurrencyConversion(invoice: Invoice, currency: Currency): Invoice{

        //If currencies match, we return the invoice without any update
        if(invoice.amount.currency == currency)
            return invoice

        //Else, we start by converting the invoice value to USD
        val usdValue = convertToUsd(invoice.amount.value, invoice.amount.currency)

        //Then we convert USD to the final desired value
        val finalValue = convertUsdTo(usdValue,currency)

        //Now we create a new instance of the invoice and we return it
        // NOTE: Be cautios here, this new invoice instance is not saved in the DB
        return Invoice(invoice.id, invoice.customerId, Money(finalValue,currency),invoice.status )

    }

    /**
     * Convert a value in currency originalCurrency into USD$
     *
     * @param value the value that we want to convert
     * @param originalCurrency the currency to convert from
     * @return the value converted to USD$
     */
    private fun convertToUsd(value: BigDecimal, originalCurrency: Currency): BigDecimal =
        when(originalCurrency){
            Currency.EUR -> value*eurValue
            Currency.USD -> value
            Currency.DKK -> value*dkkValue
            Currency.GBP -> value*gbpValue
            Currency.SEK -> value*sekValue
        }

    /**
     * Convert USD to a certain currency
     *
     * @param value the USD value to convert to another currency
     * @param finalCurrency the target currency
     * @return the usd value converted to the expected currency
     */
    private fun convertUsdTo(value: BigDecimal, finalCurrency: Currency): BigDecimal =
        when(finalCurrency){
            Currency.EUR -> value/eurValue
            Currency.USD -> value
            Currency.DKK -> value/dkkValue
            Currency.GBP -> value/gbpValue
            Currency.SEK -> value/sekValue
        }


}
