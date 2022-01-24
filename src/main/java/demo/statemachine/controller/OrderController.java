package demo.statemachine.controller;

import demo.statemachine.constant.OrderEventEnum;
import demo.statemachine.model.OrderRequest;
import demo.statemachine.model.RequestContext;
import demo.statemachine.service.BasketOrderService;
import demo.statemachine.service.OrderService;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.validation.constraints.NotNull;

@RestController
@RequestMapping("/order")
public class OrderController {

    private final RequestContext requestContext;
    private final OrderService orderService;
    private final BasketOrderService basketOrderService;

    public OrderController(RequestContext requestContext, OrderService orderService, BasketOrderService basketOrderService) {
        this.requestContext = requestContext;
        this.orderService = orderService;
        this.basketOrderService = basketOrderService;
    }

    @PostMapping(path = "/submit", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<String> submitOrder(@NotNull @RequestBody OrderRequest orderRequest) {
        basketOrderService.validateNotDuplicate(orderRequest.getCorrelationId());
        requestContext.setCorrelationId(orderRequest.getCorrelationId());
        orderService.sendEventAsync(orderRequest,OrderEventEnum.ORDER_SUBMIT);
        return new ResponseEntity<>(HttpStatus.OK);
    }

    @PutMapping(path = "/{correlationId}/cancel", consumes = MediaType.TEXT_PLAIN_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<String> cancelOrderRequest(@NotNull @PathVariable String correlationId, @RequestBody String reason) {
        requestContext.setCorrelationId(correlationId);
        orderService.sendEventAsync(OrderRequest.builder().correlationId(correlationId).cancellationReason(reason).build(), OrderEventEnum.ORDER_CANCEL);
        return new ResponseEntity<>(HttpStatus.OK);
    }

    @PutMapping(path = "/{correlationId}/confirm", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<String> confirmOrder(@NotNull @PathVariable String correlationId) {
        requestContext.setCorrelationId(correlationId);
        orderService.sendEventAsync(OrderRequest.builder().correlationId(correlationId).build(), OrderEventEnum.ORDER_CONFIRM);
        return new ResponseEntity<>(HttpStatus.OK);
    }
}
