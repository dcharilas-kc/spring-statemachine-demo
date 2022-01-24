package demo.statemachine.state;

import demo.statemachine.constant.OrderEventEnum;
import demo.statemachine.constant.OrderStateEnum;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Lazy;
import org.springframework.messaging.Message;
import org.springframework.statemachine.StateContext;
import org.springframework.statemachine.StateMachine;
import org.springframework.statemachine.persist.StateMachinePersister;
import org.springframework.statemachine.state.State;
import org.springframework.statemachine.support.StateMachineInterceptor;
import org.springframework.statemachine.transition.Transition;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class OrderStateMachineInterceptor implements StateMachineInterceptor<OrderStateEnum, OrderEventEnum> {
  
  private final StateMachinePersister<OrderStateEnum, OrderEventEnum, String> stateMachinePersister;
  
  public OrderStateMachineInterceptor(@Lazy StateMachinePersister<OrderStateEnum, OrderEventEnum, String> stateMachinePersister) {
    this.stateMachinePersister = stateMachinePersister;
  }
  
  @Override
  public Message<OrderEventEnum> preEvent(Message<OrderEventEnum> message, StateMachine<OrderStateEnum, OrderEventEnum> stateMachine) {
    return message;
  }

  @Override
  public void preStateChange(State<OrderStateEnum, OrderEventEnum> state, Message<OrderEventEnum> message,
                             Transition<OrderStateEnum, OrderEventEnum> transition, StateMachine<OrderStateEnum, OrderEventEnum> stateMachine,
                             StateMachine<OrderStateEnum, OrderEventEnum> stateMachine1) {
    log.info("Attempting to enter state " + transition.getTarget());
  }
  
  @Override
  public void postStateChange(State<OrderStateEnum, OrderEventEnum> state, Message<OrderEventEnum> message,
                              Transition<OrderStateEnum, OrderEventEnum> transition,
                              StateMachine<OrderStateEnum, OrderEventEnum> stateMachine, StateMachine<OrderStateEnum, OrderEventEnum> stateMachine1) {
    log.info("Entered state " + transition.getTarget());
  }
  
  @Override
  public StateContext<OrderStateEnum, OrderEventEnum> preTransition(StateContext<OrderStateEnum, OrderEventEnum> stateContext) {
    return stateContext;
  }
  
  @Override
  public StateContext<OrderStateEnum, OrderEventEnum> postTransition(StateContext<OrderStateEnum, OrderEventEnum> stateContext) {
    saveStateMachine(stateContext.getStateMachine());
    return stateContext;
  }
  
  @Override
  public Exception stateMachineError(StateMachine<OrderStateEnum, OrderEventEnum> stateMachine, Exception e) {
    return e;
  }
  
  @SneakyThrows
  private void saveStateMachine(StateMachine<OrderStateEnum, OrderEventEnum> stateMachine) {
    stateMachinePersister.persist(stateMachine, stateMachine.getId());
  }

}
