package demo.statemachine.repository;

import demo.statemachine.domain.OrderStateMachine;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface OrderStateMachineRepository extends JpaRepository<OrderStateMachine, Long> {

  Optional<OrderStateMachine> findByCorrelationId(String correlationId);

}
