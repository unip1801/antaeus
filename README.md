
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


