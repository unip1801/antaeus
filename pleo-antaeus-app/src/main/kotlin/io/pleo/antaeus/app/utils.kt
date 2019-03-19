
import io.pleo.antaeus.core.external.ExternalPaymentProvider
import io.pleo.antaeus.core.services.CustomerService
import io.pleo.antaeus.data.AntaeusDal
import io.pleo.antaeus.models.Currency
import io.pleo.antaeus.models.InvoiceStatus
import io.pleo.antaeus.models.Money
import java.math.BigDecimal
import kotlin.random.Random

// This will create all schemas and setup initial data
internal fun setupInitialData(dal: AntaeusDal) {
    val customers = (1..100).mapNotNull {
        dal.createCustomer(
            currency = Currency.values()[Random.nextInt(0, Currency.values().size)]
        )
    }

    customers.forEach { customer ->
        (1..10).forEach {
            dal.createInvoice(
                amount = Money(
                    value = BigDecimal(Random.nextDouble(10.0, 500.0)),
                    // From time to time we will generate an invoce that uses a different currency than the customer to test the case
                    currency =  if(Random.nextInt(from=1,until = 10) % 7 ==0)  Currency.values()[Random.nextInt(0, Currency.values().size)] else customer.currency
                ),
                customer = customer,
                status = if (it == 1) InvoiceStatus.PENDING else InvoiceStatus.PAID
            )
        }
    }
}

// This is the mocked instance of the payment provider
internal fun getPaymentProvider(customerService: CustomerService)=
    //We return an instance of the mocked class.
    ExternalPaymentProvider(customerService)
