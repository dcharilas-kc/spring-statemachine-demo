package demo.statemachine.repository;

import demo.statemachine.domain.BasketOrder;
import demo.statemachine.domain.OrderStateMachine;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface BasketOrderRepository extends JpaRepository<BasketOrder, Long> {

  Optional<BasketOrder> findByCorrelationId(String correlationId);

}
