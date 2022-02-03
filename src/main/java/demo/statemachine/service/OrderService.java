package demo.statemachine.service;

import demo.statemachine.constant.OrderEventEnum;
import demo.statemachine.constant.OrderStateEnum;
import demo.statemachine.model.OrderRequest;
import demo.statemachine.retry.annotation.CancelRetryable;
import demo.statemachine.retry.annotation.RetryableParent;
import demo.statemachine.state.OrderStateMachineInterceptor;
import lombok.SneakyThrows;
import lombok.extern.log4j.Log4j2;
import org.springframework.scheduling.annotation.Async;
import org.springframework.statemachine.StateMachine;
import org.springframework.statemachine.config.StateMachineFactory;
import org.springframework.statemachine.persist.StateMachinePersister;
import org.springframework.stereotype.Service;

import static demo.statemachine.constant.DemoConstants.ORDER_REQUEST_VARIABLE_NAME;
import static demo.statemachine.constant.DemoConstants.SHOULD_ACCEPT_AFTER_VALIDATION;

@Log4j2
@Service
public class OrderService {

    private final OrderStateMachineInterceptor stateMachineInterceptor;
    private final StateMachineFactory<OrderStateEnum, OrderEventEnum> stateMachineFactory;
    private final StateMachinePersister<OrderStateEnum, OrderEventEnum, String> stateMachinePersister;
    private final BasketOrderService basketOrderService;
    private final ValidationService validationService;
    private final DispatchService dispatchService;

    public OrderService(OrderStateMachineInterceptor stateMachineInterceptor, StateMachineFactory<OrderStateEnum, OrderEventEnum> stateMachineFactory,
                        StateMachinePersister<OrderStateEnum, OrderEventEnum, String> stateMachinePersister, BasketOrderService basketOrderService,
                        ValidationService validationService, DispatchService dispatchService) {
        this.stateMachineInterceptor = stateMachineInterceptor;
        this.stateMachineFactory = stateMachineFactory;
        this.stateMachinePersister = stateMachinePersister;
        this.basketOrderService = basketOrderService;
        this.validationService = validationService;
        this.dispatchService = dispatchService;
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
            basketOrderService.saveInitialBasketOrder(orderRequest);
        }
        return toReturn;
    }

    @Async
    public void sendEventAsync(OrderRequest orderRequest, OrderEventEnum orderEventEnum) {
        StateMachine<OrderStateEnum, OrderEventEnum> stateMachine = getStateMachine(orderRequest, orderEventEnum == OrderEventEnum.ORDER_SUBMIT);
        stateMachine.getExtendedState().getVariables().put(ORDER_REQUEST_VARIABLE_NAME, orderRequest);
        stateMachine.sendEvent(orderEventEnum);
    }

    @RetryableParent(flowName = "toValidationPending")
    public void toValidationPending(OrderRequest orderRequest, StateMachine<OrderStateEnum, OrderEventEnum> stateMachine) {
        stateMachine.getExtendedState().getVariables().put(SHOULD_ACCEPT_AFTER_VALIDATION, validationService.validateOrder(orderRequest));
    }

    @CancelRetryable(flowNames = {"toValidationPending"})
    public void toCanceled(OrderRequest orderRequest) {
        //TODO do something awesome here
    }

    public void toRejected(OrderRequest orderRequest) {
        //TODO do something awesome here
    }

    @RetryableParent(flowName = "toDispatched")
    public void toDispatched(OrderRequest orderRequest) {
        dispatchService.dispatchOrder(orderRequest);
    }

    public void toAccepted(OrderRequest orderRequest) {
        //TODO do something awesome here
    }

    public void toDelivered(OrderRequest orderRequest) {
        //TODO do something awesome here
    }

    public void toInventoryOk(OrderRequest orderRequest) {
        //TODO do something awesome here
    }

    public void toPaymentOk(OrderRequest orderRequest) {
        //TODO do something awesome here
    }
}
