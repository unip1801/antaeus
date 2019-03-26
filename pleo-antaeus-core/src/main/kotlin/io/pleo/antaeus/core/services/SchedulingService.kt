package io.pleo.antaeus.core.services

import io.pleo.antaeus.core.helpers.DateHelper
import kotlinx.coroutines.*
import org.apache.logging.log4j.kotlin.Logging

/***
 * The Scheduling Service is in charge of triggering the invoice handling in the Billing Service at the appropriate time
 *
 * Since this is a draft, only one pay date is used, the first day of each month. However, this class could be updated
 * in order to have several invoice handling calendars.
 */
class SchedulingService( private val billingService: BillingService) : Logging {

    //Objects for the thread
    private var thread : Job? = null
    private var threadShouldRun : Boolean = false

    /**
     * Function to start the BillingService thread.
     *
     * @return true if the thread was started, false if it was already started
     */
    fun start(): Boolean{

        //We start the thread only if it's not already running
        if(thread == null){
            logger.info("Starting the Billing Service")

            threadShouldRun = true

            thread = GlobalScope.launch {
                main()
            }

            return true

        }else{
            logger.info("Cannot start the billing service - It is already running")
            return false
        }

    }

    /**
     * Function to stop the processing of the current thread
     *
     * @return true if we stopped the thread, false if the thread was already stopped
     */
    fun stop() : Boolean {

        if(thread == null){
            return false
        }

        logger.info("About to stop the thread")
        threadShouldRun = false

        //Cancel the thread and join it with the main thread
        thread?.cancel()
        runBlocking {
            thread?.join()
        }

        thread = null
        logger.info("Billing service has been stopped")

        return true
    }

    /**
     * Returns true if the thread is running - Return false if not.
     */
    fun status() = (thread != null)


    /**
     * This is the function being run by the thread, so it must never end unless we want to stop the thread
     *
     *
     */
    private suspend fun main(){

        var delayToFirstOfNextMonth: Long

        //We continue to execute the function unless we want to stop the thread
        while(threadShouldRun()){
            logger.info("Loop - SchedulingService.Main")

            //We handle the invoices only if we're on the first of the month
            if(DateHelper.isFirstOfTheMonth()){
                logger.info("We are the first of the month - Processing invoices")
                billingService.handlePayments()
            }

            //How much seconds we need to sleep until first day of next month
            delayToFirstOfNextMonth = DateHelper.msUntilFirstOfNextMonth()

            logger.info("Next payment date: ${DateHelper.firstOfNextMonth()}")
            logger.info("Milliseconds to sleep until next first of the month: $delayToFirstOfNextMonth")
            logger.info("Putting thread to sleep - Good night!")

            //Put the thread to sleep
            delay(delayToFirstOfNextMonth)

        }

    }


    /**
     * Returns true if the thread should continue running or false if it should end its infinite loop
     *
     * Note: We use a function here instead of directly accessing the attribute threadShouldRun as
     * the logic for this could potentially grow into something more complex than a simple attribute
     */
    private fun threadShouldRun(): Boolean{
        return threadShouldRun
    }

}