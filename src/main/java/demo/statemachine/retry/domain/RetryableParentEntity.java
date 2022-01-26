package demo.statemachine.retry.domain;

import demo.statemachine.retry.constant.RetryableStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import javax.persistence.*;
import java.time.ZonedDateTime;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Entity
@Table(name = "retryable_parent")
public class RetryableParentEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  private String correlationId;

  private String flowName;

  @Builder.Default
  @Enumerated(EnumType.STRING)
  private RetryableStatus retryableStatus = RetryableStatus.PENDING;

  @Builder.Default
  private Integer currentRetryCycle = 0;

  @CreationTimestamp
  private ZonedDateTime initialCycleExecutedAt;

  private ZonedDateTime nextCycleExecutedAt;

  @Lob
  private String serializedArgument;

}
