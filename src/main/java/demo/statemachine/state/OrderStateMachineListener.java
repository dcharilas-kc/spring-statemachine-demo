package demo.statemachine.state;

import demo.statemachine.constant.OrderEventEnum;
import demo.statemachine.constant.OrderStateEnum;
import demo.statemachine.domain.BasketOrder;
import demo.statemachine.domain.UnprocessedEvent;
import demo.statemachine.model.OrderRequest;
import demo.statemachine.service.EventService;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.Message;
import org.springframework.statemachine.StateContext;
import org.springframework.statemachine.StateMachine;
import org.springframework.statemachine.listener.StateMachineListener;
import org.springframework.statemachine.state.State;
import org.springframework.statemachine.transition.Transition;
import org.springframework.stereotype.Component;

import static demo.statemachine.constant.DemoConstants.ORDER_REQUEST_VARIABLE_NAME;

@Slf4j
@Component
@AllArgsConstructor
public class OrderStateMachineListener implements StateMachineListener<OrderStateEnum, OrderEventEnum> {

    private final EventService eventService;

    @Override
    public void stateChanged(State<OrderStateEnum, OrderEventEnum> state, State<OrderStateEnum, OrderEventEnum> state1) {

    }

    @Override
    public void stateEntered(State<OrderStateEnum, OrderEventEnum> state) {

    }

    @Override
    public void stateExited(State<OrderStateEnum, OrderEventEnum> state) {

    }

    @Override
    public void eventNotAccepted(Message<OrderEventEnum> message) {
        //use stateContext instead to get context information
    }

    @Override
    public void transition(Transition<OrderStateEnum, OrderEventEnum> transition) {

    }

    @Override
    public void transitionStarted(Transition<OrderStateEnum, OrderEventEnum> transition) {

    }

    @Override
    public void transitionEnded(Transition<OrderStateEnum, OrderEventEnum> transition) {

    }

    @Override
    public void stateMachineStarted(StateMachine<OrderStateEnum, OrderEventEnum> stateMachine) {

    }

    @Override
    public void stateMachineStopped(StateMachine<OrderStateEnum, OrderEventEnum> stateMachine) {

    }

    @Override
    public void stateMachineError(StateMachine<OrderStateEnum, OrderEventEnum> stateMachine, Exception e) {

    }

    @Override
    public void extendedStateChanged(Object o, Object o1) {

    }

    @Override
    public void stateContext(StateContext<OrderStateEnum, OrderEventEnum> stateContext) {
        if (stateContext.getStage() == StateContext.Stage.EVENT_NOT_ACCEPTED) {
            log.warn("==> EVENT NOT PROCESSED: {}", stateContext.getEvent());
            eventService.saveUnprocessedEvent(stateContext);
        }
    }
}
