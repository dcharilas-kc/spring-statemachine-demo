package demo.statemachine.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import demo.statemachine.retry.domain.CorrelationIdAware;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class OrderRequest implements CorrelationIdAware {

    private String correlationId;
    private String customerId;
    private List<Product> products;
    private String cancellationReason;

}