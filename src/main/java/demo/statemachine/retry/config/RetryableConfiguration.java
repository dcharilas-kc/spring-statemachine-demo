package demo.statemachine.retry.config;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.ZonedDateTime;
import java.util.function.BiFunction;

@Builder
@Data
@AllArgsConstructor
@NoArgsConstructor
@Component
public class RetryableConfiguration {
  
  @Builder.Default
  private Integer maxRetryCycles = 2;
  
  @Builder.Default
  private Integer maxConsecutiveRetries = 1;
  
  @Builder.Default
  private BiFunction<ZonedDateTime, Integer, ZonedDateTime> nextExecutionDateTimeComputer
      = (initialExecutedAt, currentRetryCycle) -> ZonedDateTime.now().plusSeconds(30);
}
