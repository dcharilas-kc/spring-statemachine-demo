package demo.statemachine.domain;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import demo.statemachine.constant.OrderEventEnum;
import demo.statemachine.constant.OrderStateEnum;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import javax.persistence.*;
import java.io.Serializable;
import java.time.ZonedDateTime;

@Data
@Entity
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(Include.NON_NULL)
@Table(name = "unprocessed_event")
public class UnprocessedEvent implements Serializable {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(name = "correlation_id", nullable = false)
  private String correlationId;

  @Enumerated(EnumType.STRING)
  private OrderStateEnum currentState;

  @Enumerated(EnumType.STRING)
  private OrderEventEnum event;
  
  @CreationTimestamp
  private ZonedDateTime datetime;

  
}
