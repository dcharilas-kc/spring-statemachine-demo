package demo.statemachine.state;

import demo.statemachine.constant.OrderEventEnum;
import demo.statemachine.constant.OrderStateEnum;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.Message;
import org.springframework.statemachine.StateContext;
import org.springframework.statemachine.StateMachine;
import org.springframework.statemachine.listener.StateMachineListener;
import org.springframework.statemachine.state.State;
import org.springframework.statemachine.transition.Transition;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class OrderStateMachineListener implements StateMachineListener<OrderStateEnum, OrderEventEnum> {
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
        log.warn("==> EVENT NOT PROCESSED: {}", message);
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

    }
}
