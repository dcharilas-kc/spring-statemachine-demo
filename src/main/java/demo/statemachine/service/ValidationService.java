package demo.statemachine.service;

import demo.statemachine.model.OrderRequest;
import lombok.SneakyThrows;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ValidationService {

    private final static List<String> VALID_PRODUCTS = List.of("p-1");
    private final static int MAX_ALLOWED_QUANTITY = 100;

    @SneakyThrows
    public boolean validateOrder(OrderRequest orderRequest) {
        Thread.sleep(5000);
        return !orderRequest.getProducts().stream()
                .anyMatch(p -> !VALID_PRODUCTS.contains(p.getId()) || p.getQuantity() > MAX_ALLOWED_QUANTITY);
    }
}
