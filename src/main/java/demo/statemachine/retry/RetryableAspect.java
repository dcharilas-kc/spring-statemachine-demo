package demo.statemachine.retry;

import com.fasterxml.jackson.databind.ObjectMapper;
import demo.statemachine.constant.OrderEventEnum;
import demo.statemachine.constant.OrderStateEnum;
import demo.statemachine.model.OrderRequest;
import demo.statemachine.retry.annotation.CancelRetryable;
import demo.statemachine.retry.annotation.RetryableParent;
import demo.statemachine.retry.config.RetryableConfiguration;
import demo.statemachine.retry.domain.CorrelationIdAware;
import demo.statemachine.retry.domain.RetryableParentEntity;
import demo.statemachine.retry.service.RetryableService;
import demo.statemachine.service.OrderService;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.core.convert.ConversionService;
import org.springframework.core.env.Environment;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.statemachine.StateMachine;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Map;
import java.util.function.Consumer;

import static demo.statemachine.constant.DemoConstants.ORDER_REQUEST_VARIABLE_NAME;
import static demo.statemachine.retry.constant.RetryableStatus.*;
import static java.util.Objects.requireNonNull;

@Data
@AllArgsConstructor
@Aspect
@Component
@Slf4j
public class RetryableAspect implements BaseAspect {
  
  private final RetryableConfiguration retryableConfiguration;
  private final RetryableService retryableService;
  private final Map<String, Consumer<RetryableParentEntity>> runnableMapPerFlowName;
  private final Environment environment;
  
  private final ObjectMapper objectMapper;
  private final ConversionService conversionService;
  private final OrderService orderService;

  
  @PostConstruct
  private void setUp() {
    runnableMapPerFlowName.put("toDispatched", e -> triggerOrderEvent(e, OrderEventEnum.ORDER_ACCEPT));
  }

  @SneakyThrows
  private void triggerOrderEvent(RetryableParentEntity retryableParent, OrderEventEnum eventEnum) {
    log.info("Triggering PortInEvent {}", eventEnum);
    StateMachine<OrderStateEnum, OrderEventEnum> stateMachine = orderService.getMachine(retryableParent.getCorrelationId(), false);
    OrderRequest numberPortabilityRequest = objectMapper.readValue(
            ((RetryableParentEntity) retryableParent).getSerializedArgument(), OrderRequest.class);
    stateMachine.getExtendedState().getVariables().put(ORDER_REQUEST_VARIABLE_NAME, numberPortabilityRequest);
    stateMachine.sendEvent(eventEnum);
  }

  @Scheduled(cron = "0 * * * * *")
  private void retryParents() {
    log.info("Starting automatic retrying...");
    try {
      retryableService.findPending().forEach(this::performRetryMechanism);
    } catch (Exception e) {
      // ignored, because we print the stacktrace in RetryParent/RetryChild
    }
    log.info("Finished automatic retrying.");

  }

  @SneakyThrows
  @SuppressWarnings("unchecked")
  @Around("@annotation(demo.statemachine.retry.annotation.RetryableParent)")
  private Object wrapParent(ProceedingJoinPoint joinPoint) {
    Method method = extractMethod(joinPoint);
    RetryableParent retryableParentAnnotation = method.getAnnotation(RetryableParent.class);
    CorrelationIdAware correlationIdAware = (CorrelationIdAware) joinPoint.getArgs()[0];
    log.info("Intercepting @RetryableParent method: {} - CorrelationId: {}", method.getName(), correlationIdAware.getCorrelationId());

    retryableService.findOptional(retryableParentAnnotation, correlationIdAware)
            .ifPresentOrElse((e) -> {
            }, () -> createRetryableParent(retryableParentAnnotation, correlationIdAware));

    int consecutiveRetriesLeft = getRetryableConfiguration().getMaxConsecutiveRetries();
    Exception lastExceptionThrown = null;
    while (consecutiveRetriesLeft > 0) {
      try {
        Object object = joinPoint.proceed();
        log.info("Marking @RetryableParent method: {} as COMPLETE - FlowName: {} - Correlation Id: {}",
                method.getName(), retryableParentAnnotation.flowName(), correlationIdAware.getCorrelationId());
        retryableService.markAsComplete(retryableParentAnnotation, correlationIdAware);
        return object;
      } catch (Exception e) {
        lastExceptionThrown = e;
        // Something went wrong
        consecutiveRetriesLeft--;
        log.error("Exception occurred while retrying @RetryableParent method: {} - FlowName: {} "
                        + "- Correlation Id: {} - Consecutive Retries left: {} of: {}. Message: {}",
                method.getName(), retryableParentAnnotation.flowName(), correlationIdAware.getCorrelationId(),
                consecutiveRetriesLeft, getRetryableConfiguration().getMaxConsecutiveRetries(), e.getMessage());
      }
    }

    // Reaching this point means all the X consecutive retries have failed
    RetryableParentEntity retryableParentEntity = retryableService.findOrThrow(retryableParentAnnotation, correlationIdAware);

    if (retryableParentEntity.getCurrentRetryCycle().equals(getRetryableConfiguration().getMaxRetryCycles())) {
      log.error("Marking @RetryableParent method: {} as FAILED - FlowName: {} - Correlation Id: {}",
              method.getName(), retryableParentEntity.getFlowName(), retryableParentEntity.getCorrelationId());
      retryableService.markAs(retryableParentAnnotation, correlationIdAware, FAILED);
      /*
       * Fallout
       */
      executeFallout(correlationIdAware.getCorrelationId(), lastExceptionThrown);
    } else {
      retryableService.updateNextExecutionValues(retryableParentEntity);
    }
    throw requireNonNull(lastExceptionThrown);
    //return joinPoint.proceed();
  }

  @SneakyThrows
  @SuppressWarnings("unchecked")
  @Around("@annotation(demo.statemachine.retry.annotation.CancelRetryable)")
  private Object wrapCancelRetryable(ProceedingJoinPoint joinPoint) {
    Method method = extractMethod(joinPoint);
    CancelRetryable cancelRetryable = method.getAnnotation(CancelRetryable.class);
    CorrelationIdAware correlationIdAware = (CorrelationIdAware) joinPoint.getArgs()[0];
    Arrays.stream(cancelRetryable.flowNames())
            .forEach(flowName -> {
              log.info("Intercepting @CancelRetryable::flowName: {} methodName: {} - Correlation Id: {}. Marking as CANCELED.",
                      flowName, method.getName(), correlationIdAware.getCorrelationId());
              retryableService.markAs(flowName, correlationIdAware, CANCELED);
            });

    return joinPoint.proceed();
  }

  private void performRetryMechanism(RetryableParentEntity retryableParentEntity) {
    try {
      getRunnableMapPerFlowName().getOrDefault(
                      retryableParentEntity.getFlowName(),
                      (e) -> log.error("Could not find flowName, {}", retryableParentEntity.getFlowName()))
              .accept(retryableParentEntity);
    } catch (Exception ex) {
      log.error("Failed to performRetryMechanism for flowName:{} and correlationId:{}",
              retryableParentEntity.getFlowName(), retryableParentEntity.getCorrelationId());
    }
  }

  @SneakyThrows
  public RetryableParentEntity createRetryableParent(RetryableParent annotation, CorrelationIdAware correlationIdAware) {
    return retryableService.save(
            RetryableParentEntity
                    .builder()
                    .correlationId(correlationIdAware.getCorrelationId())
                    .flowName(annotation.flowName())
                    .serializedArgument(objectMapper.writeValueAsString(correlationIdAware))
                    .build());
  }

  public void executeFallout(String correlationId, Exception e) {
    log.warn("Execute fallout for order " +correlationId);
  }

}
