
package demo.statemachine.service;

import demo.statemachine.constant.OrderStateEnum;
import demo.statemachine.domain.BasketOrder;
import demo.statemachine.domain.StateTransition;
import demo.statemachine.exception.CorrelationIdError;
import demo.statemachine.model.OrderRequest;
import demo.statemachine.repository.BasketOrderRepository;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.convert.ConversionService;
import org.springframework.stereotype.Service;

import java.time.ZonedDateTime;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Slf4j
@Service
public class BasketOrderService {

    private final BasketOrderRepository basketOrderRepository;
    private final ConversionService conversionService;

    public BasketOrderService(BasketOrderRepository basketOrderRepository, ConversionService conversionService) {
        this.basketOrderRepository = basketOrderRepository;
        this.conversionService = conversionService;
    }

    public Optional<BasketOrder> findOptionalByCorrelationId(String correlationId) {
        return basketOrderRepository.findByCorrelationId(correlationId);
    }

    public BasketOrder save(BasketOrder basketOrder) {
        return basketOrderRepository.save(basketOrder);
    }

    @SneakyThrows
    public BasketOrder findByCorrelationIdOrThrow(String correlationId) {
        return basketOrderRepository.findByCorrelationId(correlationId)
                .orElseThrow(() -> new CorrelationIdError("No Order was found with correlationId: " + correlationId));
    }

    @SneakyThrows
    public void validateDoesNotExist(String correlationId) {
        if (basketOrderRepository.findByCorrelationId(correlationId).isPresent()) {
            throw new CorrelationIdError("Correlation Id already exists.");
        }
    }

    @SneakyThrows
    public void validateExists(String correlationId) {
        findByCorrelationIdOrThrow(correlationId);
    }

    @SneakyThrows
    public void validateNotInFiniteState(String correlationId) {
        BasketOrder basketOrder = findByCorrelationIdOrThrow(correlationId);
        if (OrderStateEnum.valueOf(basketOrder.getState()).isFinite()) {
            throw new Exception("Order is in finite state.");
        }
    }

    public void saveInitialBasketOrder(OrderRequest orderRequest) {
        BasketOrder basketOrder = BasketOrder.builder()
                .customerId(orderRequest.getCustomerId())
                .correlationId(orderRequest.getCorrelationId())
                .state(OrderStateEnum.INIT.toString())
                .transitions(Stream.of(StateTransition.builder()
                        .status(OrderStateEnum.INIT.toString())
                        .datetime(ZonedDateTime.now())
                        .payload(conversionService.convert(orderRequest, String.class))
                        .build()).collect(Collectors.toList()))
                .build();
        basketOrderRepository.save(basketOrder);
        log.info("Initialized order " + orderRequest.getCorrelationId());
    }

    public BasketOrder changeOrderStatus(OrderRequest orderRequest, OrderStateEnum state) {
        BasketOrder basketOrder = findByCorrelationIdOrThrow(orderRequest.getCorrelationId());
        // do not log internal states
        if (state.skipLog()) {
            return basketOrder;
        }

        basketOrder.setState(state.toString());
        basketOrder.getTransitions().add(StateTransition.builder()
                .status(state.toString())
                .datetime(ZonedDateTime.now())
                .payload(state.hasPayload() ? conversionService.convert(orderRequest, String.class) : null)
                .build());
        log.info("Change state of order " + orderRequest.getCorrelationId() + " to " +state);
        if (state == OrderStateEnum.CANCELLED) {
            basketOrder.setTerminationReason(orderRequest.getCancellationReason());
        } else if (state == OrderStateEnum.REJECTED) {
            //TODO should support multiple rejection reasons
            basketOrder.setTerminationReason("validation error");
        }
        basketOrderRepository.save(basketOrder);
        return basketOrder;
    }

}
