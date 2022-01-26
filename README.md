# General description

This service demonstrates usage of Spring Statemachine ([https://spring.io/projects/spring-statemachine](https://spring.io/projects/spring-statemachine)) 
through a simple scenario for order management. The order states are depicted in the following diagram:  
![](/Users/dimitris/Desktop/Workspace/VARIOUS/spring-statemachine-demo/order_statedrawio.jpg)

The service listens on port **8080** and requires a mySQL DB with _root/root_ credentials. This configuration can be changed in _application.yml_.  
Java version: 17

# API

All orders are uniquely identified through the `correlationId` field.  
The following endpoints are exposed as part of the order lifecycle:

1) New order submit (needs non existing correlation id)  
`curl --location --request POST 'http://localhost:8080/order/submit' \
--header 'Content-Type: application/json' \
--data-raw '{
"correlationId": "100057",
"customerId": "12345",
"products": [
{
"id": "p1",
"quantity": 2
}
]
}'`

2) Order cancellation (needs existing correlation id)  
   `curl --location --request PUT 'http://localhost:8080/order/100057/cancel' \
   --header 'Content-Type: text/plain' \
   --data-raw 'Changed my mind'`

3) Order delivery confirmation (needs existing correlation id)  
   `curl --location --request PUT 'http://localhost:8080/order/100055/confirm' \
   --data-raw ''`

# Mocks

All orders having product with id `p1` or `p2` or `p3` and quantity < 100 are automatically accepted. If an order does not satisfy these criteria, it is automatically rejected.  

Order dispatch always succeeds if only `p1` exists within products. If `p3` exists, dispatch always fails. If `p2` exists, dispatch fails with a 50% probability. 

# Retry

Order dispatch supports retry. Related configuration can be found in `RetryableConfiguration`. Currently, 2 retries are performed with _30''_ interval.