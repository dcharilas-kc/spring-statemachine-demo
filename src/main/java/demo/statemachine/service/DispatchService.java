package demo.statemachine.service;

import demo.statemachine.model.OrderRequest;
import lombok.SneakyThrows;
import org.springframework.stereotype.Service;

import java.util.Random;

@Service
public class DispatchService {

    private final Random random = new Random();

    @SneakyThrows
    public void dispatchOrder(OrderRequest orderRequest) {
        boolean shouldFail = orderRequest.getProducts().stream().anyMatch(p -> "p3".contains(p.getId()));
        boolean shouldFailWithProbability = orderRequest.getProducts().stream().anyMatch(p -> "p2".contains(p.getId()));
        if (shouldFail || (shouldFailWithProbability && random.nextBoolean())) {
            throw new Exception("Dispatch failed for order " + orderRequest.getCorrelationId());
        }
    }
}
