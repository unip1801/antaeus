/*
    Configures the rest app along with basic exception handling and URL endpoints.
 */

package io.pleo.antaeus.rest

import io.javalin.Javalin
import io.javalin.apibuilder.ApiBuilder.get
import io.javalin.apibuilder.ApiBuilder.path
import io.pleo.antaeus.core.exceptions.EntityNotFoundException
import io.pleo.antaeus.core.services.BillingService
import io.pleo.antaeus.core.services.CustomerService
import io.pleo.antaeus.core.services.InvoiceService
import io.pleo.antaeus.models.InvoiceStatus
import mu.KotlinLogging

private val logger = KotlinLogging.logger {}

class AntaeusRest (
    private val invoiceService: InvoiceService,
    private val customerService: CustomerService,
    private val billingService: BillingService
) : Runnable {

    override fun run() {
        app.start(7000)
    }

    // Set up Javalin rest app
    private val app = Javalin
        .create()
        .apply {
            // InvoiceNotFoundException: return 404 HTTP status code
            exception(EntityNotFoundException::class.java) { _, ctx ->
                ctx.status(404)
            }
            // Unexpected exception: return HTTP 500
            exception(Exception::class.java) { e, _ ->
                logger.error(e) { "Internal server error" }
            }
            // On 404: return message
            error(404) { ctx -> ctx.json("not found") }
        }

    init {
        // Set up URL endpoints for the rest app
        app.routes {
           path("rest") {
               // Route to check whether the app is running
               // URL: /rest/health
               get("health") {
                   it.json("ok")
               }

               // V1
               path("v1") {

                   path("billingservice") {

                       // Endpoint to start the billingservice thread
                       // URL: /rest/v1/billingservice/start
                       get("start"){
                           it.json(billingService.start())
                       }

                       // Endpoint to stop the billingservice thread
                       // URL: /rest/v1/billingservice/stop
                       get("stop"){
                           it.json( billingService.stop())
                       }

                       // Endpoint to get the billingservice thread status
                       // URL: /rest/v1/billingservice/status
                       get("status"){
                           it.json(billingService.status())
                       }

                       // Endpoint to force invoice handling even if we're not the first of the month
                       // URL: /rest/v1/billingservice/invoicehandling
                       get("payallinvoices"){
                           it.json(billingService.handlePayments())
                       }

                       // Endpoint to force individual invoice handling even if we're not the first of the month
                       // URL: /rest/v1/billingservice/invoicehandling
                       get("payinvoice/:id"){
                           it.json(billingService.handleInvoice(it.pathParam("id").toInt()))
                       }

                   }

                   path("invoices") {
                       // URL: /rest/v1/invoices
                       get {
                           it.json(invoiceService.fetchAll())
                       }

                       // Endpoint to reset all the invoices in error to pending state
                       // URL: /rest/v1/invoices/reseterrors
                       get("reseterrors"){
                           invoiceService.resetAllErrorsToPending()
                           it.json(true)
                       }

                       // URL: /rest/v1/invoices/status/{:status}
                       get("status/:status"){
                           it.json(invoiceService.fetchInvoicesByStatusStr(it.pathParam("status")))
                       }
                       // URL: /rest/v1/invoices/{:id}
                       get(":id") {
                          it.json(invoiceService.fetch(it.pathParam("id").toInt()))
                       }


                   }

                   path("customers") {
                       // URL: /rest/v1/customers
                       get {
                           it.json(customerService.fetchAll())
                       }

                       // URL: /rest/v1/customers/{:id}
                       get(":id") {
                           it.json(customerService.fetch(it.pathParam("id").toInt()))
                       }
                   }
               }
           }
        }
    }
}
