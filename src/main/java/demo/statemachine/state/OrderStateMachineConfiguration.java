package demo.statemachine.state;

import demo.statemachine.constant.OrderEventEnum;
import demo.statemachine.constant.OrderStateEnum;
import demo.statemachine.model.OrderRequest;
import demo.statemachine.service.OrderService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Lazy;
import org.springframework.messaging.Message;
import org.springframework.statemachine.StateMachinePersist;
import org.springframework.statemachine.action.Action;
import org.springframework.statemachine.config.EnableStateMachineFactory;
import org.springframework.statemachine.config.EnumStateMachineConfigurerAdapter;
import org.springframework.statemachine.config.builders.StateMachineConfigurationConfigurer;
import org.springframework.statemachine.config.builders.StateMachineStateConfigurer;
import org.springframework.statemachine.config.builders.StateMachineTransitionConfigurer;
import org.springframework.statemachine.listener.StateMachineListener;
import org.springframework.statemachine.listener.StateMachineListenerAdapter;
import org.springframework.statemachine.persist.DefaultStateMachinePersister;
import org.springframework.statemachine.persist.StateMachinePersister;

import java.util.EnumSet;

import static demo.statemachine.constant.DemoConstants.ORDER_REQUEST_VARIABLE_NAME;
import static demo.statemachine.constant.DemoConstants.SHOULD_ACCEPT_AFTER_VALIDATION;


@Slf4j
@Configuration
@EnableStateMachineFactory(name = "orderStateMachineFactory")
public class OrderStateMachineConfiguration extends EnumStateMachineConfigurerAdapter<OrderStateEnum, OrderEventEnum> {

    private final OrderService orderService;

    public OrderStateMachineConfiguration(@Lazy OrderService orderService) {
        this.orderService = orderService;
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
                .listener(orderListener());
    }

    @Override
    public void configure(StateMachineStateConfigurer<OrderStateEnum, OrderEventEnum> states) throws Exception {
        states
                .withStates()
                .initial(OrderStateEnum.INIT)
                .state(OrderStateEnum.CREATED, context -> context.getStateMachine().sendEvent(OrderEventEnum.ORDER_VALIDATE))
                .state(OrderStateEnum.UNDER_CHECK, context -> {
                    if ((boolean) context.getStateMachine().getExtendedState().getVariables().getOrDefault(SHOULD_ACCEPT_AFTER_VALIDATION, false)) {
                        context.getStateMachine().sendEvent(OrderEventEnum.ORDER_ACCEPT);
                    } else {
                        context.getStateMachine().sendEvent(OrderEventEnum.ORDER_REJECT);
                    }
                })
                .end(OrderStateEnum.REJECTED)
                .end(OrderStateEnum.CANCELLED)
                .end(OrderStateEnum.DELIVERED)
                .states(EnumSet.allOf(OrderStateEnum.class));
    }

    @Override
    public void configure(StateMachineTransitionConfigurer<OrderStateEnum, OrderEventEnum> transitions) throws Exception {
        transitions.withExternal()
                .source(OrderStateEnum.INIT).target(OrderStateEnum.CREATED).event(OrderEventEnum.ORDER_SUBMIT).action(initAction()).and().withExternal()
                .source(OrderStateEnum.CREATED).target(OrderStateEnum.CANCELLED).event(OrderEventEnum.ORDER_CANCEL).action(cancelAction()).and().withExternal()
                .source(OrderStateEnum.CREATED).target(OrderStateEnum.UNDER_CHECK).event(OrderEventEnum.ORDER_VALIDATE).action(validateAction()).and().withExternal()
                .source(OrderStateEnum.UNDER_CHECK).target(OrderStateEnum.CANCELLED).event(OrderEventEnum.ORDER_CANCEL).action(cancelAction()).and().withExternal()
                .source(OrderStateEnum.UNDER_CHECK).target(OrderStateEnum.REJECTED).event(OrderEventEnum.ORDER_REJECT).action(rejectAction()).and().withExternal()
                .source(OrderStateEnum.UNDER_CHECK).target(OrderStateEnum.DISPATCHED).event(OrderEventEnum.ORDER_ACCEPT).action(acceptAction()).and().withExternal()
                .source(OrderStateEnum.DISPATCHED).target(OrderStateEnum.DELIVERED).event(OrderEventEnum.ORDER_CONFIRM).action(completeAction()).and().withExternal();
    }


    private StateMachineListener<OrderStateEnum, OrderEventEnum> orderListener() {
        return new StateMachineListenerAdapter<>() {
            @Override
            public void eventNotAccepted(Message event) {
                log.warn("Event not processed: {}", event);
            }
        };
    }

    private Action<OrderStateEnum, OrderEventEnum> initAction() {
        return context -> {
          OrderRequest orderRequest = (OrderRequest) context.getExtendedState().getVariables().get(ORDER_REQUEST_VARIABLE_NAME);
          orderService.toCreated(orderRequest);
        };
    }

    public Action<OrderStateEnum, OrderEventEnum> acceptAction() {
        return context -> {
          OrderRequest orderRequest = (OrderRequest) context.getExtendedState().getVariables().get(ORDER_REQUEST_VARIABLE_NAME);
            orderService.toDispatched(orderRequest);
        };
    }

    private Action<OrderStateEnum, OrderEventEnum> rejectAction() {
        return context -> {
          OrderRequest orderRequest = (OrderRequest) context.getExtendedState().getVariables().get(ORDER_REQUEST_VARIABLE_NAME);
            orderService.toRejected(orderRequest);
        };
    }

    private Action<OrderStateEnum, OrderEventEnum> validateAction() {
        return context -> {
          OrderRequest orderRequest = (OrderRequest) context.getExtendedState().getVariables().get(ORDER_REQUEST_VARIABLE_NAME);
          orderService.toCheckPending(orderRequest,context.getStateMachine());
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

}
