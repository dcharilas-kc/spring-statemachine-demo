package demo.statemachine.repository;

import demo.statemachine.domain.UnprocessedEvent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UnprocessedEventRepository extends JpaRepository<UnprocessedEvent, Long> {

    Optional<UnprocessedEvent> findByCorrelationId(String correlationId);
}
