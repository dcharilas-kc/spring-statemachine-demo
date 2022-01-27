package demo.statemachine.service;

import demo.statemachine.constant.OrderEventEnum;
import demo.statemachine.constant.OrderStateEnum;
import demo.statemachine.domain.BasketOrder;
import demo.statemachine.domain.StateTransition;
import demo.statemachine.model.OrderRequest;
import demo.statemachine.repository.OrderStateMachineRepository;
import demo.statemachine.retry.annotation.CancelRetryable;
import demo.statemachine.retry.annotation.RetryableParent;
import demo.statemachine.state.OrderStateMachineInterceptor;
import lombok.SneakyThrows;
import lombok.extern.log4j.Log4j2;
import org.springframework.core.convert.ConversionService;
import org.springframework.core.task.TaskExecutor;
import org.springframework.statemachine.StateMachine;
import org.springframework.statemachine.config.StateMachineFactory;
import org.springframework.statemachine.persist.StateMachinePersister;
import org.springframework.stereotype.Service;

import java.time.ZonedDateTime;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static demo.statemachine.constant.DemoConstants.ORDER_REQUEST_VARIABLE_NAME;
import static demo.statemachine.constant.DemoConstants.SHOULD_ACCEPT_AFTER_VALIDATION;
import static java.util.concurrent.CompletableFuture.runAsync;

@Log4j2
@Service
public class OrderService {

    private final OrderStateMachineInterceptor stateMachineInterceptor;
    private final StateMachineFactory<OrderStateEnum, OrderEventEnum> stateMachineFactory;
    private final StateMachinePersister<OrderStateEnum, OrderEventEnum, String> stateMachinePersister;
    private final OrderStateMachineRepository orderStateMachineRepository;
    private final BasketOrderService basketOrderService;
    private final ConversionService conversionService;
    private final ValidationService validationService;
    private final DispatchService dispatchService;
    private final TaskExecutor taskExecutor;

    public OrderService(OrderStateMachineInterceptor stateMachineInterceptor, StateMachineFactory<OrderStateEnum, OrderEventEnum> stateMachineFactory,
                        StateMachinePersister<OrderStateEnum, OrderEventEnum, String> stateMachinePersister,
                        OrderStateMachineRepository orderStateMachineRepository, BasketOrderService basketOrderService, ConversionService conversionService, ValidationService validationService, DispatchService dispatchService, TaskExecutor taskExecutor) {
        this.stateMachineInterceptor = stateMachineInterceptor;
        this.stateMachineFactory = stateMachineFactory;
        this.stateMachinePersister = stateMachinePersister;
        this.orderStateMachineRepository = orderStateMachineRepository;
        this.basketOrderService = basketOrderService;
        this.conversionService = conversionService;
        this.validationService = validationService;
        this.dispatchService = dispatchService;
        this.taskExecutor = taskExecutor;
    }

    @SneakyThrows
    public StateMachine<OrderStateEnum, OrderEventEnum> getMachine(String correlationId, boolean isInit) {
        var stateMachine = stateMachineFactory.getStateMachine(correlationId);
    var toReturn = isInit ? stateMachine : stateMachinePersister.restore(stateMachine, correlationId);
        toReturn.getStateMachineAccessor().withRegion().addStateMachineInterceptor(stateMachineInterceptor);
        return toReturn;
    }

    @SneakyThrows
    public StateMachine<OrderStateEnum, OrderEventEnum> getStateMachine(OrderRequest orderRequest, boolean isInit) {
        var toReturn = getMachine(orderRequest.getCorrelationId(), isInit);
        if (isInit) {
            saveInitialBasketOrder(orderRequest);
        }
        return toReturn;
    }

    private void saveInitialBasketOrder(OrderRequest orderRequest) {
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
        basketOrderService.save(basketOrder);
        log.info("Initialized order " + orderRequest.getCorrelationId());
    }

    public void sendEventAsync(OrderRequest orderRequest, OrderEventEnum orderEventEnum) {
        runAsync(() -> {
            StateMachine<OrderStateEnum, OrderEventEnum> stateMachine = getStateMachine(orderRequest, orderEventEnum == OrderEventEnum.ORDER_SUBMIT);
            stateMachine.getExtendedState().getVariables().put(ORDER_REQUEST_VARIABLE_NAME, orderRequest);
            stateMachine.sendEvent(orderEventEnum);
        }, taskExecutor);
    }

    public void toCreated(OrderRequest orderRequest) {
        BasketOrder basketOrder = changeOrderStatus(orderRequest,OrderStateEnum.CREATED, false);
        basketOrderService.save(basketOrder);
    }

    @CancelRetryable(flowNames = {"toDispatched"})
    public void toCanceled(OrderRequest orderRequest) {
        BasketOrder basketOrder = changeOrderStatus(orderRequest,OrderStateEnum.CANCELLED, true);
        basketOrder.setTerminationReason(orderRequest.getCancellationReason());
        basketOrderService.save(basketOrder);
    }

    public void toRejected(OrderRequest orderRequest) {
        BasketOrder basketOrder = changeOrderStatus(orderRequest,OrderStateEnum.REJECTED, false);
        basketOrder.setTerminationReason("Something wrong");
        basketOrderService.save(basketOrder);
    }

    public void toCheckPending(OrderRequest orderRequest, StateMachine<OrderStateEnum, OrderEventEnum> stateMachine) {
        BasketOrder basketOrder = changeOrderStatus(orderRequest,OrderStateEnum.UNDER_CHECK, false);
        basketOrderService.save(basketOrder);
        stateMachine.getExtendedState().getVariables().put(SHOULD_ACCEPT_AFTER_VALIDATION, validationService.validateOrder(orderRequest));
    }

    @RetryableParent(flowName = "toDispatched")
    public void toDispatched(OrderRequest orderRequest) {
        dispatchService.dispatchOrder(orderRequest);
        basketOrderService.validateNotInFiniteState(orderRequest.getCorrelationId());
        BasketOrder basketOrder = changeOrderStatus(orderRequest,OrderStateEnum.DISPATCHED, false);
        basketOrderService.save(basketOrder);
    }

    public void toDelivered(OrderRequest orderRequest) {
        BasketOrder basketOrder = changeOrderStatus(orderRequest,OrderStateEnum.DELIVERED, true);
        basketOrderService.save(basketOrder);
    }

    private BasketOrder changeOrderStatus(OrderRequest orderRequest, OrderStateEnum state, boolean logPayload) {
        BasketOrder basketOrder = basketOrderService.findByCorrelationIdOrThrow(orderRequest.getCorrelationId());
        basketOrder.setState(state.toString());
        basketOrder.getTransitions().add(StateTransition.builder()
                .status(state.toString())
                .datetime(ZonedDateTime.now())
                .payload(logPayload ? conversionService.convert(orderRequest, String.class) : null)
                .build());
        log.info("Change state of order " + orderRequest.getCorrelationId() + " to " +state);
        return basketOrder;
    }
}
