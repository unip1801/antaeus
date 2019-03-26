
## Work notes - Paulo Unia

First work session - Sunday - 2h

I have never used Kotlin before, however I work with Java quite often so it feels really familiar. 

This morning I started learning more about Kotlin and Exposed, playing around with the project and the DB.

So far I got a good understanding of the existing codebase, it's now time to plan the enhancements.

First commit: Added a function to fetch pending invoices only.

---

Second work session - Sunday - 2h

Started working on the BillingService. Before actually doing any logic, I decided to add the Kotlin version of Log4J
so I could easily output information while working. 

Once I started working on the function to handle each individual invoice, I realized that it would be important to capture
all the different states an invoice could be on, so I added these new statuses to the InvoiceStatus enum: 
* NETWORK_ERROR
* MISSING_FUNDS
* ERROR

As of now, I have a simple version of the code that handles all "simple" transactions (no exceptions occur). Next steps
are to do some unit testing and update the mock of PaymentProvider in order to have it throw exceptions from time to time.

I will also need to update the seeds to insert discrepancies between the customer's currency and the invoice's currency,
so I can apply corrective measures during the invoice handing process when necessary.

---

Third work session - Monday - 3h

Things started to get very interesting as my understanding of Kotlin grows!

For today, I decided to complexify the situations that can arise during the payment of the invoice, so I changed the 
PaymentProvider provided in the base code for a full class, where I added some logic to throw currency and network exceptions, 
I also added some randomness to the result of the transaction (success or missing funds).

To keep track of how the processing went, I decided to add some logs at the end of the handling process to have
an overall view of the results.

Since the new PaymentProvider could throw currency exceptions, I also updated the setupInitialData function to generate
invoices where the currency is different than the customer's currency, so we cover all possible cases.

Once I started having currency exceptions, I introduced a CurrencyService in order to modify the invoices
to match the customer's currency.

Finally, I also added a retry mechanism so we retry once the processing for all the invoices that got a network exception

Next steps would be to add some unit testing, followed by some sort of scheduling system to initiate the invoice handling
process on the 1st of each month.


---

Fourth working session - Sunday - 4h

I had a very busy week at work and at my administration committee, so wasn't able to continue after monday session.

Today I added some testing for the currency service and worked on the scheduling.

The basic idea behind the scheduling is to use a thread that will be asleep until the first day of next month.
The objective in fact would be to set the base for have enough flexibility so in the future it would be easy to
have several pay dates instead of just one (i.e. payments each week or each two weeks, payment on specific day of month
 for some users, etc). I used the coroutines library from Kotlin to achieve these results
 
 I added a new helper, the DateHelper, in order to regroup all the functions related to date/datetime needed for the
 business logic
 
 I also added some new routes in the API in order to start and stop the billing service thread, as well as get the status. 
 Normally this would be behind a protected API, but for the purpose of this POC the authentication is not needed.


Fifth working session - Sunday - 1h

I decided to update the REST api in order to provide endpoints to process all the invoices or any individual invoice. 
I also added an endpoint to reset the status of all invoices in error to pending.

To do so, I had to refactor the BillingService in order to return the processed invoices, I also added a verification
so we don't ever process invoices that were already paid.


Sixth working session - Sunday - 0.5h

I realized that with my implementation of the BillingService and the rest API there was the possibility to have
concurrent access issues. Technically speaking, someone could trigger the payment of an invoice manually through the API
at the same time that the BillingService is handling the invoices. Since these would be loaded into memory already,
if the payment through the API was made before the BillingService thread would handle the same invoice, we would end up
requesting two payments for the same invoice.

In order to prevent the problem, I used a lock on the BillingService. The implementation is quite simple and works well,
however it wouldn't be scalable for systems where there are tenths of thousands of invoices being handled on the same day. 
In that case, we should implement a lock that locks each invoice instead of locking all the invoices.

---

Seventh working session - Monday - 2h

I decided to refactor the Billing Service as it got very crowded. I extracted the scheduling portion into the 
new SchedulingService and also created the ReportService in order to handle the logging for the batch operations.

Now all the services respect the separation of concerns, as their responsibilities are better defined.



## Antaeus

Antaeus (/ænˈtiːəs/), in Greek mythology, a giant of Libya, the son of the sea god Poseidon and the Earth goddess Gaia. He compelled all strangers who were passing through the country to wrestle with him. Whenever Antaeus touched the Earth (his mother), his strength was renewed, so that even if thrown to the ground, he was invincible. Heracles, in combat with him, discovered the source of his strength and, lifting him up from Earth, crushed him to death.

Welcome to our challenge.

## The challenge

As most "Software as a Service" (SaaS) companies, Pleo needs to charge a subscription fee every month. Our database contains a few invoices for the different markets in which we operate. Your task is to build the logic that will pay those invoices on the first of the month. While this may seem simple, there is space for some decisions to be taken and you will be expected to justify them.

### Structure
The code given is structured as follows. Feel free however to modify the structure to fit your needs.
```
├── pleo-antaeus-app
|
|       Packages containing the main() application. 
|       This is where all the dependencies are instantiated.
|
├── pleo-antaeus-core
|
|       This is where you will introduce most of your new code.
|       Pay attention to the PaymentProvider and BillingService class.
|
├── pleo-antaeus-data
|
|       Module interfacing with the database. Contains the models, mappings and access layer.
|
├── pleo-antaeus-models
|
|       Definition of models used throughout the application.
|
├── pleo-antaeus-rest
|
|        Entry point for REST API. This is where the routes are defined.
└──
```

## Instructions
Fork this repo with your solution. We want to see your progression through commits (don’t commit the entire solution in 1 step) and don't forget to create a README.md to explain your thought process.

Please let us know how long the challenge takes you. We're not looking for how speedy or lengthy you are. It's just really to give us a clearer idea of what you've produced in the time you decided to take. Feel free to go as big or as small as you want.

Happy hacking 😁!

## How to run
```
./docker-start.sh
```

## Libraries currently in use
* [Exposed](https://github.com/JetBrains/Exposed) - DSL for type-safe SQL
* [Javalin](https://javalin.io/) - Simple web framework (for REST)
* [kotlin-logging](https://github.com/MicroUtils/kotlin-logging) - Simple logging framework for Kotlin
* [JUnit 5](https://junit.org/junit5/) - Testing framework
* [Mockk](https://mockk.io/) - Mocking library


