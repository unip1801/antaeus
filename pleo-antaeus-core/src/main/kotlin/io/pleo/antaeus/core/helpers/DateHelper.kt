package io.pleo.antaeus.core.helpers

import java.time.Duration
import java.time.LocalDateTime
import java.time.LocalDateTime.now
import java.util.Calendar



class DateHelper{

    /**
     * Our intent is to be able to call these functions without instantiating the class, so we place them
     * as companion objects
     */
    companion object {

        /**
         * Returns true if today is the first day of the month
         */
        fun isFirstOfTheMonth():Boolean{
            val currentDate = Calendar.getInstance()
            return (currentDate.get(Calendar.DAY_OF_MONTH) == 24)
        }

        /**
         * Returns a LocalDate object representing first day of next month
         */
        fun firstOfNextMonth(): LocalDateTime{
            val currentDate = now()

            return when{
                currentDate.monthValue != 12 -> LocalDateTime.of(currentDate.year, currentDate.monthValue + 1, 1,0,0)
                else -> LocalDateTime.of(currentDate.year+1,1,1,0,0)
            }
        }

        /**
         * Returns the number of milliseconds until 00:00 of the first day of next month
         */
        fun msUntilFirstOfNextMonth(): Long{

            return Duration.between(now(), firstOfNextMonth()).toMillis()

        }

    }


}

