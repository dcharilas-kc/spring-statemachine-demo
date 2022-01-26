package demo.statemachine.retry.service;

import demo.statemachine.retry.annotation.RetryableParent;
import demo.statemachine.retry.config.RetryableConfiguration;
import demo.statemachine.retry.constant.RetryableStatus;
import demo.statemachine.retry.domain.CorrelationIdAware;
import demo.statemachine.retry.domain.RetryableParentEntity;
import demo.statemachine.retry.exception.RetryableNotFoundException;
import demo.statemachine.retry.repository.RetryableParentRepository;
import lombok.AllArgsConstructor;
import lombok.Data;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.ZonedDateTime;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

import static demo.statemachine.retry.constant.RetryableStatus.COMPLETE;
import static demo.statemachine.retry.constant.RetryableStatus.PENDING;

@Data
@AllArgsConstructor
@Service
public class RetryableService {

  private RetryableParentRepository retryableParentRepository;
  private RetryableConfiguration retryableConfiguration;

  @Transactional(propagation = Propagation.MANDATORY)
  public void deleteAllByCorrelationIds(List<String> correlationIds) {
    var parents = retryableParentRepository.findAllByCorrelationIdIn(correlationIds);
    var parentIds = Objects.requireNonNull(parents).stream().map(RetryableParentEntity::getId).collect(Collectors.toList());
    retryableParentRepository.deleteAllByIdIn(parentIds);
  }

  public List<RetryableParentEntity> findPending() {
    return retryableParentRepository.findAllByRetryableStatusAndNextCycleExecutedAtBefore(PENDING, ZonedDateTime.now());
  }

  public RetryableParentEntity save(RetryableParentEntity entity) {
    return retryableParentRepository.save(entity);
  }

  public RetryableParentEntity findOrThrow(RetryableParent retryableParentAnnotation, CorrelationIdAware correlationIdAware) {
    return findOptional(retryableParentAnnotation, correlationIdAware)
            .orElseThrow(() -> new RetryableNotFoundException(
                    String.format("Missing RetryableParentEntity - CorrelationId: %s - FlowName: %s",
                            retryableParentAnnotation.flowName(), correlationIdAware.getCorrelationId())));
  }

  public Optional<RetryableParentEntity> findOptional(RetryableParent retryableParentAnnotation, CorrelationIdAware correlationIdAware) {
    return retryableParentRepository.findByFlowNameAndCorrelationId(retryableParentAnnotation.flowName(), correlationIdAware.getCorrelationId());
  }

  public void markAs(RetryableParent retryableParentAnnotation, CorrelationIdAware correlationIdAware, RetryableStatus retryableStatus) {
    RetryableParentEntity retryableParentEntity = findOrThrow(retryableParentAnnotation, correlationIdAware);
    retryableParentEntity.setRetryableStatus(retryableStatus);
    retryableParentRepository.save(retryableParentEntity);
  }

  public void markAs(String flowName, CorrelationIdAware correlationIdAware, RetryableStatus retryableStatus) {
    Optional<RetryableParentEntity> retryableParentEntity = retryableParentRepository
            .findByFlowNameAndCorrelationId(flowName, correlationIdAware.getCorrelationId());
    retryableParentEntity.ifPresent(e -> {
      e.setRetryableStatus(retryableStatus);
      retryableParentRepository.save(e);
    });
  }

  public void markAsComplete(RetryableParent retryableParentAnnotation, CorrelationIdAware correlationIdAware) {
    RetryableParentEntity retryableParentEntity = findOrThrow(retryableParentAnnotation, correlationIdAware);
    retryableParentEntity.setRetryableStatus(COMPLETE);
    retryableParentRepository.save(retryableParentEntity);
  }

  public void updateNextExecutionValues(RetryableParentEntity retryableParentEntity) {
    retryableParentEntity.setNextCycleExecutedAt(calculateNextCycleExecutedAt(retryableParentEntity));
    retryableParentEntity.setCurrentRetryCycle(retryableParentEntity.getCurrentRetryCycle() + 1);
    retryableParentRepository.save(retryableParentEntity);
  }

  private ZonedDateTime calculateNextCycleExecutedAt(RetryableParentEntity retryableParentEntity) {
    return retryableConfiguration.getNextExecutionDateTimeComputer().apply(
            retryableParentEntity.getInitialCycleExecutedAt(),
            retryableParentEntity.getCurrentRetryCycle()
    );
  }

}
