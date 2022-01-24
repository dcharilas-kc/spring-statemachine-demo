
package demo.statemachine.service;

import demo.statemachine.domain.BasketOrder;
import demo.statemachine.repository.BasketOrderRepository;
import lombok.SneakyThrows;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class BasketOrderService {

    private final BasketOrderRepository basketOrderRepository;

    public BasketOrderService(BasketOrderRepository basketOrderRepository) {
        this.basketOrderRepository = basketOrderRepository;
    }

    public Optional<BasketOrder> findOptionalByCorrelationId(String correlationId) {
        return basketOrderRepository.findByCorrelationId(correlationId);
    }

    public BasketOrder save(BasketOrder basketOrder) {
        return basketOrderRepository.save(basketOrder);
    }

    @SneakyThrows
    public BasketOrder findByCorrelationIdOrThrow(String correlationId) {
        return basketOrderRepository.findByCorrelationId(correlationId)
                .orElseThrow(() -> new Exception("No Order was found with correlationId: " + correlationId));
    }

    @SneakyThrows
    public void validateNotDuplicate(String correlationId) {
        if (basketOrderRepository.findByCorrelationId(correlationId).isPresent()) {
            throw new Exception("Correlation Id already exists.");
        }
    }
}
