package demo.statemachine.retry.repository;

import demo.statemachine.retry.constant.RetryableStatus;
import demo.statemachine.retry.domain.RetryableParentEntity;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

import java.time.ZonedDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface RetryableParentRepository extends CrudRepository<RetryableParentEntity, Long> {

  List<RetryableParentEntity> findAllByRetryableStatusAndNextCycleExecutedAtBefore(RetryableStatus retryableStatus, ZonedDateTime zonedDateTime);

  Optional<RetryableParentEntity> findByFlowNameAndCorrelationId(String flowName, String correlationId);

  Optional<RetryableParentEntity> findByCorrelationId(String correlationId);

  void deleteAllByIdIn(List<Long> correlationIds);

  List<RetryableParentEntity> findAllByCorrelationIdIn(List<String> correlationIds);

}
