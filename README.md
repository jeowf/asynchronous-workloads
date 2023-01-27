# Asynchronous Workloads

A multi-tenant microservice approaches involving components with asynchronous interactions and batch jobs for managing asynchronous workloads. This project was implemented using Java and Spring Boot.

This project queues messages in RabbitMQ queues and consumes these messages asynchronously, taking into account their order among all instances.

## Approaches

In this project, there are two approaches to consume the messages present in the queues. The first approach uses distributed locks (which we will call DL), based on [this implementation](https://blog.rabbitmq.com/posts/2014/02/distributed-semaphores-with-rabbitmq/). The second approach uses Single Active Consumer (SAC) [queues](https://www.rabbitmq.com/consumers.html#single-active-consumer) and a strategy similar to the first one, but does not require a lock queue.

## How to execute

You will need:

 * Java 17
 * Maven Compiler
 * Docker
 * Postman (to perform the tests)

In the root folder of the project, first compile the program with the command:

`./mvnw clean install`

Then run the command below to start the worker. Replace `X` with the number of replicas you want.

`docker-compose up --scale worker-fifo=X`

Look in the project for the `postman` folder and import the collection in your Postman. Adjust the port in the requests, if necessary.

Now you can run the tests.

## Before running the tests

To choose the DL approach, change line 14 of `docker-compose.yml` to `STRATEGY=dl`. If you want to use the SAC approach, change the same line to `STRATEGY=sac`.

The first time you start the application or if you change the execution strategy (between DL and SAC), execute the "Clean" and "Setup" requests that are present in the postman collection, respectively.

## Validation

In this [spreadsheet](https://docs.google.com/spreadsheets/d/1CYX_g3GKV7QGZZ9IfcrsTYiid9LU5y1hPLQMNHE_Rnw/edit?usp=sharing) you can find an example of execution using DL and SAC.