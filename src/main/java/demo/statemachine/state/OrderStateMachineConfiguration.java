package demo.statemachine.state;

import demo.statemachine.constant.OrderEventEnum;
import demo.statemachine.constant.OrderStateEnum;
import demo.statemachine.domain.BasketOrder;
import demo.statemachine.model.OrderRequest;
import demo.statemachine.service.BasketOrderService;
import demo.statemachine.service.OrderService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Lazy;
import org.springframework.statemachine.StateMachinePersist;
import org.springframework.statemachine.action.Action;
import org.springframework.statemachine.config.EnableStateMachineFactory;
import org.springframework.statemachine.config.EnumStateMachineConfigurerAdapter;
import org.springframework.statemachine.config.builders.StateMachineConfigurationConfigurer;
import org.springframework.statemachine.config.builders.StateMachineStateConfigurer;
import org.springframework.statemachine.config.builders.StateMachineTransitionConfigurer;
import org.springframework.statemachine.guard.Guard;
import org.springframework.statemachine.persist.DefaultStateMachinePersister;
import org.springframework.statemachine.persist.StateMachinePersister;

import java.util.EnumSet;
import java.util.Optional;

import static demo.statemachine.constant.DemoConstants.ORDER_REQUEST_VARIABLE_NAME;
import static demo.statemachine.constant.DemoConstants.SHOULD_ACCEPT_AFTER_VALIDATION;


@Slf4j
@Configuration
@EnableStateMachineFactory(name = "orderStateMachineFactory")
public class OrderStateMachineConfiguration extends EnumStateMachineConfigurerAdapter<OrderStateEnum, OrderEventEnum> {

    private final OrderService orderService;
    private final BasketOrderService basketOrderService;
    private final OrderStateMachineListener orderListener;

    public OrderStateMachineConfiguration(@Lazy OrderService orderService, BasketOrderService basketOrderService, OrderStateMachineListener orderListener) {
        this.orderService = orderService;
        this.basketOrderService = basketOrderService;
        this.orderListener = orderListener;
    }

    @Bean
    public StateMachinePersister<OrderStateEnum, OrderEventEnum, String> orderPersister(
            StateMachinePersist<OrderStateEnum, OrderEventEnum, String> defaultPersist) {
        return new DefaultStateMachinePersister<>(defaultPersist);
    }

    @Override
    public void configure(StateMachineConfigurationConfigurer<OrderStateEnum, OrderEventEnum> config) throws Exception {
        config.withConfiguration()
                .autoStartup(true)
                .listener(orderListener);
    }

    @Override
    public void configure(StateMachineStateConfigurer<OrderStateEnum, OrderEventEnum> states) throws Exception {
        states
                .withStates()
                .initial(OrderStateEnum.INIT)
                // triggers proper event depending on validation result
                .state(OrderStateEnum.VALIDATION_PENDING, context -> {
                    if ((boolean) context.getStateMachine().getExtendedState().getVariables().getOrDefault(SHOULD_ACCEPT_AFTER_VALIDATION, false)) {
                        context.getStateMachine().sendEvent(OrderEventEnum.ORDER_ACCEPT);
                    } else {
                        context.getStateMachine().sendEvent(OrderEventEnum.ORDER_REJECT);
                    }
                })
                // always triggers event
                .state(OrderStateEnum.DISPATCH_PENDING, context -> context.getStateMachine().sendEvent(OrderEventEnum.ORDER_DISPATCH))
                .fork(OrderStateEnum.FORK)
                .join(OrderStateEnum.JOIN)
                .states(EnumSet.allOf(OrderStateEnum.class)).and()
                // fork paths
                .withStates()
                    .parent(OrderStateEnum.FORK)
                    .initial(OrderStateEnum.INVENTORY_PENDING)
                    .end(OrderStateEnum.INVENTORY_OK).and()
                .withStates()
                    .parent(OrderStateEnum.FORK)
                    .initial(OrderStateEnum.PAYMENT_PENDING)
                    .end(OrderStateEnum.PAYMENT_OK)
                // finite states
                .end(OrderStateEnum.REJECTED)
                .end(OrderStateEnum.CANCELLED)
                .end(OrderStateEnum.DELIVERED);
    }

    @Override
    public void configure(StateMachineTransitionConfigurer<OrderStateEnum, OrderEventEnum> transitions) throws Exception {
        transitions
                .withExternal()
                    .source(OrderStateEnum.INIT).target(OrderStateEnum.VALIDATION_PENDING).event(OrderEventEnum.ORDER_SUBMIT).action(initAction()).and()
                .withExternal()
                    .source(OrderStateEnum.VALIDATION_PENDING).target(OrderStateEnum.CANCELLED).event(OrderEventEnum.ORDER_CANCEL).action(cancelAction()).and()
                .withExternal()
                    .source(OrderStateEnum.VALIDATION_PENDING).target(OrderStateEnum.REJECTED).event(OrderEventEnum.ORDER_REJECT).action(rejectAction()).and()
                .withExternal()
                    .source(OrderStateEnum.VALIDATION_PENDING).target(OrderStateEnum.ACCEPTED).event(OrderEventEnum.ORDER_ACCEPT).action(acceptAction()).guard(notInFiniteState()).and()
                .withExternal()
                    .source(OrderStateEnum.DISPATCH_PENDING).target(OrderStateEnum.DISPATCHED).event(OrderEventEnum.ORDER_DISPATCH).action(dispatchAction()).guard(notInFiniteState()).and()
                .withExternal()
                    .source(OrderStateEnum.DISPATCHED).target(OrderStateEnum.DELIVERED).event(OrderEventEnum.ORDER_CONFIRM).action(completeAction()).guard(notInFiniteState()).and()
                .withExternal()
                    .source(OrderStateEnum.INVENTORY_PENDING).target(OrderStateEnum.INVENTORY_OK).action(validateInventoryAction()).guard(notInFiniteState()).and()
                .withExternal()
                    .source(OrderStateEnum.PAYMENT_PENDING).target(OrderStateEnum.PAYMENT_OK).action(validatePaymentAction()).guard(notInFiniteState()).and()
                .withExternal()
                    .source(OrderStateEnum.JOIN).target(OrderStateEnum.DISPATCH_PENDING).guard(notInFiniteState()).guard(notInFiniteState()).and()
                .withExternal()
                    .source(OrderStateEnum.ACCEPTED).target(OrderStateEnum.FORK).timer(100).guard(notInFiniteState()).and()

                .withFork().source(OrderStateEnum.FORK).target(OrderStateEnum.INVENTORY_PENDING).target(OrderStateEnum.PAYMENT_PENDING).and()
                .withJoin().source(OrderStateEnum.INVENTORY_OK).source(OrderStateEnum.PAYMENT_OK).target(OrderStateEnum.JOIN);
    }

    private Guard<OrderStateEnum, OrderEventEnum> notInFiniteState() {
        return context -> {
            OrderRequest orderRequest = (OrderRequest) context.getExtendedState().getVariables().get(ORDER_REQUEST_VARIABLE_NAME);
            Optional<BasketOrder> basketOrder = basketOrderService.findOptionalByCorrelationId(orderRequest.getCorrelationId());
            return !basketOrder.isPresent() || !OrderStateEnum.valueOf(basketOrder.get().getState()).isFinite();
        };
    }

    private Action<OrderStateEnum, OrderEventEnum> initAction() {
        return context -> {
            OrderRequest orderRequest = (OrderRequest) context.getExtendedState().getVariables().get(ORDER_REQUEST_VARIABLE_NAME);
            orderService.toValidationPending(orderRequest, context.getStateMachine());
        };
    }

    public Action<OrderStateEnum, OrderEventEnum> dispatchAction() {
        return context -> {
            OrderRequest orderRequest = (OrderRequest) context.getExtendedState().getVariables().get(ORDER_REQUEST_VARIABLE_NAME);
            orderService.toDispatched(orderRequest);
        };
    }

    public Action<OrderStateEnum, OrderEventEnum> acceptAction() {
        return context -> {
            OrderRequest orderRequest = (OrderRequest) context.getExtendedState().getVariables().get(ORDER_REQUEST_VARIABLE_NAME);
            orderService.toAccepted(orderRequest);
        };
    }

    private Action<OrderStateEnum, OrderEventEnum> rejectAction() {
        return context -> {
            OrderRequest orderRequest = (OrderRequest) context.getExtendedState().getVariables().get(ORDER_REQUEST_VARIABLE_NAME);
            orderService.toRejected(orderRequest);
        };
    }

    private Action<OrderStateEnum, OrderEventEnum> completeAction() {
        return context -> {
            OrderRequest orderRequest = (OrderRequest) context.getExtendedState().getVariables().get(ORDER_REQUEST_VARIABLE_NAME);
            orderService.toDelivered(orderRequest);
        };
    }

    private Action<OrderStateEnum, OrderEventEnum> cancelAction() {
        return context -> {
            OrderRequest orderRequest = (OrderRequest) context.getExtendedState().getVariables().get(ORDER_REQUEST_VARIABLE_NAME);
            orderService.toCanceled(orderRequest);
        };
    }

    private Action<OrderStateEnum, OrderEventEnum> validateInventoryAction() {
        return context -> {
            OrderRequest orderRequest = (OrderRequest) context.getExtendedState().getVariables().get(ORDER_REQUEST_VARIABLE_NAME);
            orderService.toInventoryOk(orderRequest);
        };
    }

    private Action<OrderStateEnum, OrderEventEnum> validatePaymentAction() {
        return context -> {
            OrderRequest orderRequest = (OrderRequest) context.getExtendedState().getVariables().get(ORDER_REQUEST_VARIABLE_NAME);
            orderService.toPaymentOk(orderRequest);
        };
    }

}
