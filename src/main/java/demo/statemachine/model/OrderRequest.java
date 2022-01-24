package demo.statemachine.model;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class OrderRequest {

    private String correlationId;
    private String customerId;
    private List<Product> products;
    private String cancellationReason;

}