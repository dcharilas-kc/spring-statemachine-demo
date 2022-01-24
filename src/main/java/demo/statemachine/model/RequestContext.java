package demo.statemachine.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.web.context.annotation.RequestScope;

import javax.annotation.ManagedBean;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@ManagedBean
@RequestScope
public class RequestContext {
  
  private String correlationId;
}
