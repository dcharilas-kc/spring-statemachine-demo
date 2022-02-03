package demo.statemachine.service;

import demo.statemachine.constant.OrderEventEnum;
import demo.statemachine.constant.OrderStateEnum;
import demo.statemachine.domain.UnprocessedEvent;
import demo.statemachine.model.OrderRequest;
import demo.statemachine.repository.UnprocessedEventRepository;
import lombok.AllArgsConstructor;
import org.springframework.statemachine.StateContext;
import org.springframework.stereotype.Service;

import static demo.statemachine.constant.DemoConstants.ORDER_REQUEST_VARIABLE_NAME;

@Service
@AllArgsConstructor
public class EventService {

    private final UnprocessedEventRepository unprocessedEventRepository;

    public void saveUnprocessedEvent(StateContext<OrderStateEnum, OrderEventEnum> stateContext) {
        OrderRequest orderRequest = (OrderRequest) stateContext.getExtendedState().getVariables().get(ORDER_REQUEST_VARIABLE_NAME);
        UnprocessedEvent event = UnprocessedEvent.builder()
                .currentState(stateContext.getStateMachine().getState().getId())
                .event(stateContext.getEvent())
                .correlationId(orderRequest.getCorrelationId())
                .build();
        unprocessedEventRepository.save(event);
    }
}
