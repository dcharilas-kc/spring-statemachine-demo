package demo.statemachine.state;

import demo.statemachine.constant.OrderEventEnum;
import demo.statemachine.constant.OrderStateEnum;
import demo.statemachine.domain.OrderStateMachine;
import demo.statemachine.repository.OrderStateMachineRepository;
import lombok.AllArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.statemachine.StateMachineContext;
import org.springframework.statemachine.StateMachinePersist;
import org.springframework.stereotype.Component;

@Slf4j
@AllArgsConstructor
@Component
public class OrderStateMachinePersist implements StateMachinePersist<OrderStateEnum, OrderEventEnum, String> {
  
  private final OrderStateMachineRepository orderStateMachineRepository;
  
  @Override
  public void write(StateMachineContext<OrderStateEnum, OrderEventEnum> stateMachineContext, String correlationId) {
    OrderStateMachine stateMachine = orderStateMachineRepository.findByCorrelationId(correlationId)
        .orElse(OrderStateMachine.builder()
            .correlationId(correlationId)
            .build());
    stateMachine.setStateMachineContext(stateMachineContext);
    orderStateMachineRepository.save(stateMachine);
  }

  @SneakyThrows
  @Override
  public StateMachineContext<OrderStateEnum, OrderEventEnum> read(String correlationId) {
    return orderStateMachineRepository.findByCorrelationId(correlationId)
        .orElseThrow(() -> new Exception("No StateMachine found with correlationId: " + correlationId))
        .getStateMachineContext();
  }
}
