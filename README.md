# Demo for Spring Boot Web MVC and Stripe Payments integration

This project aims to create a RESTful API for accepting Online Payments from a client storefront and processing them via the [Stripe Payment Gateway](https://stripe.com/). The API supports two Stripe payment options: Stripe Hosted Checkout Page and Stripe Payment Intent API. Detailed Architecture Sequence Diagrams for both payment options are explained in the accompanying [blog post](https://misoft.pro/blog/fintech/stripe-payment-integration-options).

Business scope:

The business requirement is defined as "Users must be able to pay for selected products or services and receive an invoice".

## Tech stack:

- [ ] Java 21
- [ ] [Spring Boot Web MVC 3.3.0](https://docs.spring.io/spring-framework/reference/web/webmvc.html)

## Runtime requirements

- JDK 21+

## Compile and package application (tests run included)

```bash
./gradlew clean assemble
```

## Only test run

```bash
./gradlew clean test
```

## Build docker image

```bash
docker build -t backend-server .
```

## Run docker image

```bash
docker run -d -p 8080:8080 --name demo-spring-stripe-server backend-server
```

## Logging

For the moment all server logs are written to the console and file using `ch.qos.logback.core.ConsoleAppender`
and `ch.qos.logback.core.rolling.RollingFileAppender` respectively configured in `logback.xml` file. All logs contain `traceId` value
which is implicitly populated from `org.slf4j.MDC` context and shown in the logs according to `CONSOLE_LOG_PATTERN` defined in
logback.xml.

Log record example with traceId printed right after log level `INFO`:

`2024-07-02 12:55:16.986  INFO [2695f8537e1fe05a841f0df18898e730] 1612 - [          parallel-1]  i.s.a.b.i.s.c.PaymentApi.hostedCheckout(37) : Start checkout`

## Tracing

Distributed `traceId` is attached to every incoming request and automatically propagated to downstream threads and
requests.
Downstream treads/coroutines can access it through implicitly propagated context implemented by `micrometer-tracing`
library.
All API responses contain `X-Trace-Id` header to be able to match every http request with corresponding logs on the server side.
Example of http response header `X-Trace-Id: 7e0674227780f3226ae9a8b7d350a5ee`.

## Metrics

All maintenance endpoints are accessed by following url `http://localhost:8080/api/internal/actuator`. The list of all
app measured metrics are here `http://localhost:8080/api/internal/actuator/metrics`. For example, the number of API
calls since server start is exposed in Prometheus format
by a link `http://localhost:8080/api/internal/actuator/metrics/custom.api.calls.total` and implemented
using `io.micrometer.core.instrument.Counter` from Micrometer library.

## Health checks

Health checks are provided through Spring Boot Actuator by a link http://localhost:8080/api/internal/actuator/health

## Error handling

All thrown exceptions are handled globally by using Spring
@ControllerAdvice at class `pro.misoft.poc.springreactive.kotlin.infra.spring.errorhandling.RestExceptionHandler`. This exception handler convert exception to http response with proper http code and error body. Error body has the localized error message to be shown to the end user and unique internal code to be used by customer support team. Error body json:
```
{
   “httpStatus”: 400,
   “internalCode":"order-4002",
   “errorMessage":"Input fields contain errors",
   "traceId":"7f006775-04b5-4f81-8250-a85ffb976722",
   "subErrors":[
      {
         "objectName":"orderDto",
         "fieldName":"userName”,
         "rejectedValue”:”N”,
         "message":"size must be between 2 and 36"
      }
   ]
}
```
Error body data class:
```
data class ApiError(
 val httpStatus: Int,
    /**
     * Internal code to classify error
     *
     * pattern="${serviceNamePrefix}-${httpErrorCategory}${sequenceNumberUniqueForServiceNameAndHttpErrorCode}".
     *
     * examples=["portfolio-4001", "portfolio-4002","order-5001", "user-4001", "user-4002", "user-5001"]
     */
    val internalCode: String,
    /**
     * Human-readable localized message to display on client side
     */
    val errorMessage: String,
    /**
     * Unique identifier of user request.
     * In case of distributed architecture this identifier is passed to all downstream requests to other services.
     */
    val traceId: String,
    /**
     * Collect information about sub errors,
     * for example specific fields of forms providing human-readable error messages for each field to guide user trough out a flow
     */
    val subErrors: List<ApiSubError> = listOf()
   )
```

## Openapi documentation

`Springdoc-openapi` library is integrated to automatically generate OpenAPI documentation. Endpoint to see OpenAPI spec http://localhost:8080/api/internal/openapi. Swagger-UI is already embedded to web server and can be accessed by url http://localhost:8080/api/internal/swagger-ui. The openapi contract schema can be customized by applying swagger annotations like `io.swagger.v3.oas.annotations.media.Schema`, see example `pro.misoft.poc.springreactive.kotlin.infra.spring.controller.contract.MonetaryAmountSchema`.

## API usage

```bash
curl -v http://localhost:8080/api/v1/payments \
    -H "Content-Type: application/json" \
    -d '{
       "items": [{"name": "Product1", "price": 100}],
       "customerName": "John Doe",
       "customerEmail": "john.doe@example.com",
       "subscriptionId": "sub123",
       "invoiceNeeded": true
     }'
```
