---
title: "Step 7 - Function calling and tools"
layout: page
content-toc: true
---
# Step 07 - Function calling and Tools

The RAG pattern allows passing knowledge to the LLM based on your own data.
It's a very popular pattern, but not the only one that can be used.

In this step, we are going to see another way to give superpowers to the LLM: [Function Calling](https://docs.quarkiverse.io/quarkus-langchain4j/dev/function-calling.html).
Basically, we will allow the LLM to call a function that you have defined in your code.
The LLM will decide when and with which parameters to call the function.
Of course, make sure that you do not allow the LLM to call a function that could be harmful to your system, and make sure to sanitize any input data.

## Function calling

Function calling is a mechanism offered by some LLMs (GPTs, Llama...). It allows the LLM to call a function that you have defined in your application.
When the application sends the user message to the LLM, it also sends the list of functions that the LLM can call.

Then the LLM can decide, if it wants, to call one of these functions with the parameters it wants.
The application receives the method invocation request and executes the function with the parameters provided by the LLM.
The result is sent back to the LLM, which can use it to continue the conversation, and compute the next message.

![Function calling](../../images/function-calling.png)

In this step, we are going to see how to implement function calling in our application.
We will set up a database and create a function that allows the LLM to retrieve data (bookings, customers...) from the database.

The final code is available in the `step-07` folder.
However, we recommend you follow the step-by-step guide to understand how it works, and the different steps to implement this pattern.

## A couple of new dependencies

Before starting, we need to install a couple of new dependencies.
<mark>Open the `pom.xml` file and add the following dependencies:</mark>

**pom.xml**
```xml
{snippet:insert("section-1/step-07/pom.xml", "step-7")}
```

> [!TIP]
> You could also open another terminal and run
>
> ```shell
> ./mvnw quarkus:add-extension -Dextensions="hibernate-orm-panache,jdbc-postgresql"
> ```

If you are not familiar with Panache, it's a layer on top of Hibernate ORM that simplifies the interaction with the database.
You can find more information about Panache [here](https://quarkus.io/guides/hibernate-orm-panache).

## Preparing the entities

Now that we have the dependencies, we can create a couple of entities.
We are going to store a list of bookings in the database.
Each booking is associated with a customer.
A customer can have multiple bookings.

<mark>Create the `dev.langchain4j.quarkus.workshop.Customer` entity class with the following content:</mark>

**Customer.java**
```java
{snippet:insert("section-1/step-07/src/main/java/dev/langchain4j/quarkus/workshop/Customer.java")}
```

<mark>Then create the `dev.langchain4j.quarkus.workshop.Booking` entity class with the following content:</mark>

**Booking.java**
```java
{snippet:insert("section-1/step-07/src/main/java/dev/langchain4j/quarkus/workshop/Booking.java")}
```

<mark>While we are at it, let's create the `dev.langchain4j.quarkus.workshop.Exceptions` class containing a set of `Exception`s we will be using:</mark>

**Exceptions.java**
```java
{snippet:insert("section-1/step-07/src/main/java/dev/langchain4j/quarkus/workshop/Exceptions.java")}
```

Alright, we have our entities and exceptions.
Let's add some data to the database.

<mark>Create the `src/main/resources/import.sql` file with the following content:</mark>

**import.sql**
```sql
{snippet:insert("section-1/step-07/src/main/resources/import.sql")}
```

This file will be executed when the application starts, and will insert some data into the database.
Without specific configuration, it will only be applied in dev mode (`./mvnw quarkus:dev`).

## Defining Tools

Alright, we now have everything we need to create a function that allows the LLM to retrieve data from the database.
We are going to create a `BookingRepository` class that will contain a set of functions to interact with the database.

<mark>Create the `dev.langchain4j.quarkus.workshop.BookingRepository` class with the following content:</mark>

**BookingRepository.java**
```java
{snippet:insert("section-1/step-07/src/main/java/dev/langchain4j/quarkus/workshop/BookingRepository.java")}
```

The _repository_ defines three methods:

- `cancelBooking` to cancel a booking. It checks if the booking can be cancelled and deletes it from the database.
- `listBookingsForCustomer` to list all bookings for a customer.
- `getBookingDetails` to retrieve the details of a booking.

Each method is annotated with the `@Tool` annotation.
That is how we tell the LLM that these methods can be called.
The optional value of the annotation can gives more information about the tool, so the LLM can pick the right one.

## Giving a toolbox to the LLM

<mark>Let's now modify our AI service interface (`dev.langchain4j.quarkus.workshop.CustomerSupportAgent`):</mark>

**CustomerSupportAgent.java**
```java hl_lines="7 17-21 23-24"
{snippet:insert("section-1/step-07/src/main/java/dev/langchain4j/quarkus/workshop/CustomerSupportAgent.java")}
```

We have added the `@ToolBox` annotation to the `chat` method.
It lists the classes that contain the tools that the LLM can call.

Also note that we have added a new placeholder `\{current_date}` in the system prompt, so the LLM knows the current date (and can apply the cancellation policy).

The `chat` method now returns a `String` instead of a `Multi<String>`, so the WebSocket endpoint has to return a `String` too.
<mark>Update the `onTextMessage` method of the `CustomerSupportAgentWebSocket` class:</mark>

**CustomerSupportAgentWebSocket.java**
```java
{snippet:insert("section-1/step-07/src/main/java/dev/langchain4j/quarkus/workshop/CustomerSupportAgentWebSocket.java", "tools")}
```

> [!NOTE]
> **Prompt and templates**
> The system message and user messages can contain placeholders.
> The placeholders are replaced by the values provided by the application.
> You can pass parameters to AI service methods and include them in the prompt.
> It uses the [Qute](https://quarkus.io/guides/qute) template engine underneath.
> This is a powerful feature to allow you to have advanced template logic.

## Testing the function calling

Let's test the function calling.
<mark>Make sure the application is running (`./mvnw quarkus:dev`).</mark>

Open your browser and go to [http://localhost:8080](http://localhost:8080).
In the chatbot, let's try the following interactions:

**You**
```text
Hello, I would like to cancel a booking.
```

**AI**: Sure, I can help with that. I'll need a bit more information to proceed: 1. Your first and last name to look up the booking. 2. The booking ID associated with your reservation. Please provide these details so I can assist you further.

**You**
```text
My name is Speedy McWheels. But, I don't remember the booking ID. Can you list all my future bookings?
```

The chatbot lists three bookings, with the IDs 1, 2 and 3.
Their dates depend on the day you run the application, because the sample bookings are created relative to the current date.
The first one starts tomorrow, the second one in two weeks and the third one in a month.

**You**
```text
I would like to cancel the last one.
```

The chatbot should confirm that booking 3 is cancelled.
This booking starts more than 11 days from now and lasts four days, so the cancellation policy allows it.

Alright, let's try now to cancel an invalid booking:

**You**
```text
Can you cancel my first booking?
```

This time the chatbot should refuse, because the booking starts in less than 11 days and the policy only allows cancellations up to 11 days before the start.

<video controls style="max-width: 100%;">
  <source src="/images/chat-booking.mp4" type="video/mp4">
</video>

## Conclusion

In this step, we explored how to implement function calling within our application, enabling us to create _agents_—LLMs that can not only reason but also interact dynamically with the system.

A function in this context is simply a method from your application annotated with `@Tool`. 
The actual implementation of the function is entirely customizable.
For instance, you could extend your chatbot with tools for weather forecasting (by integrating with a remote service), personalized recommendations, or other external data sources.
Additionally, you can leverage more specialized LLMs, routing specific queries—such as legal or insurance-related questions—to models trained in those domains.

Up to now, we have seen how you can expose business logic directly to an LLM, making methods callable as reasoning tools.
This works great inside a single Quarkus application, but what if you want to go a step further and share those tools across services, runtimes, or even different languages?
In the [next step](../step-08/), we’ll explore how to take this Agentic AI concept further with the Model Context Protocol.
