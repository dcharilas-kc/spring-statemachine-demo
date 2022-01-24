package demo.statemachine.domain;

import demo.statemachine.constant.OrderEventEnum;
import demo.statemachine.constant.OrderStateEnum;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.statemachine.StateMachineContext;

import javax.persistence.*;
import java.io.Serializable;

@Data
@Entity
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "order_state_machine", indexes = {
    @Index(name = "order_state_machine_correlationId_index", columnList = "correlation_id"),
})
public class OrderStateMachine implements Serializable {
  
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;
  
  @Column(name = "correlation_id", nullable = false)
  private String correlationId;
  
  @ManyToOne(optional = false)
  @JoinColumn(name = "correlation_id", referencedColumnName = "correlationId", updatable = false, insertable = false)
  private BasketOrder order;
  
  @Lob
  private StateMachineContext<OrderStateEnum, OrderEventEnum> stateMachineContext;
  
}
