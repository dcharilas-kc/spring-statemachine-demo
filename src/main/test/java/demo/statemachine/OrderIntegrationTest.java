package demo.statemachine;

import demo.statemachine.base.BaseIntegrationTest;
import demo.statemachine.base.annotation.IntegrationTest;
import demo.statemachine.constant.OrderStateEnum;
import demo.statemachine.domain.BasketOrder;
import demo.statemachine.model.OrderRequest;
import demo.statemachine.model.Product;
import demo.statemachine.repository.BasketOrderRepository;
import lombok.SneakyThrows;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.ResultMatcher;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static java.util.Objects.nonNull;
import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@IntegrationTest
@ContextConfiguration(initializers = OrderIntegrationTest.class)
public class OrderIntegrationTest extends BaseIntegrationTest {
  
  private static final int SLEEP_TIME_AFTER_REQUEST = 200;

  @Autowired
  private BasketOrderRepository basketOrderRepository;
  
  @SneakyThrows
  @BeforeEach
  void setUp() {

  }
  
  @Test
  @DisplayName("Test Submit - Duplicate Correlation Id - FAIL")
  void testSubmitDuplicateCorrelationId() {
    OrderRequest orderRequest = getOrderRequest();
    submitOrder(orderRequest, status().isOk());
    submitOrder(orderRequest, status().isBadRequest());
  }

  @Test
  @DisplayName("Test Submit and Accept - SUCCESS")
  void testSubmitValidOrder() {
    OrderRequest orderRequest = getOrderRequest();
    submitOrder(orderRequest, status().isOk());
    assertOrderState(orderRequest.getCorrelationId(),OrderStateEnum.DISPATCHED);
  }

  @Test
  @DisplayName("Test Submit and Reject - SUCCESS")
  void testSubmitInvalidOrder() {
    OrderRequest orderRequest = getOrderRequest();
    orderRequest.getProducts().get(0).setQuantity(1000);
    submitOrder(orderRequest, status().isOk());
    assertOrderState(orderRequest.getCorrelationId(),OrderStateEnum.REJECTED);
  }

  @Test
  @DisplayName("Test Complete Order - SUCCESS")
  void testCompleteOrder() {
    OrderRequest orderRequest = getOrderRequest();
    submitOrder(orderRequest, status().isOk());
    confirmOrder(orderRequest,status().isOk());
    assertOrderState(orderRequest.getCorrelationId(),OrderStateEnum.DELIVERED);
  }

  @Test
  @DisplayName("Test Complete Order - Missing Correlation Id - FAIL")
  void testCompleteOrderMissingCorrelationId() {
    OrderRequest orderRequest = getOrderRequest();
    confirmOrder(orderRequest,status().isBadRequest());
  }

  @Test
  @DisplayName("Test Cancel Order after Dispatch - FAIL")
  void testCancelAfterDispatch() {
    OrderRequest orderRequest = getOrderRequest();
    submitOrder(orderRequest,status().isOk());
    assertOrderState(orderRequest.getCorrelationId(),OrderStateEnum.DISPATCHED);
    orderRequest.setCancellationReason("changed my mind");
    cancelOrder(orderRequest,status().isOk());
    assertOrderState(orderRequest.getCorrelationId(),OrderStateEnum.DISPATCHED);
  }

  private void assertOrderState(String correlationId, OrderStateEnum state) {
    Optional<BasketOrder> order = basketOrderRepository.findByCorrelationId(correlationId);
    assertTrue(order.isPresent());
    assertEquals(order.get().getState(),state.name());
  }

  private OrderRequest getOrderRequest() {
    return OrderRequest.builder()
            .correlationId(UUID.randomUUID().toString())
            .customerId("customer-123")
            .products(List.of(Product.builder().id("p1").quantity(2).build()))
            .build();
  }
  
  @SneakyThrows
  private MvcResult submitOrder(OrderRequest orderRequest, ResultMatcher resultMatcher) {
    MvcResult mvcResult = mockMvc.perform(MockMvcRequestBuilders.post("/order/submit")
        .contentType(MediaType.APPLICATION_JSON)
        .content(objectMapper.writeValueAsString(orderRequest))
        .accept(MediaType.APPLICATION_JSON))
        .andExpect(resultMatcher)
        .andReturn();
    MILLISECONDS.sleep(SLEEP_TIME_AFTER_REQUEST);
    return mvcResult;
  }

  @SneakyThrows
  private MvcResult cancelOrder(OrderRequest orderRequest, ResultMatcher resultMatcher) {
    MvcResult mvcResult = mockMvc.perform(MockMvcRequestBuilders.put("/order/" + orderRequest.getCorrelationId() + "/cancel")
            .contentType(MediaType.TEXT_PLAIN_VALUE)
            .content(nonNull(orderRequest.getCancellationReason()) ? orderRequest.getCancellationReason() : "-")
            .accept(MediaType.APPLICATION_JSON))
            .andExpect(resultMatcher)
            .andReturn();
    MILLISECONDS.sleep(SLEEP_TIME_AFTER_REQUEST);
    return mvcResult;
  }

  @SneakyThrows
  private MvcResult confirmOrder(OrderRequest orderRequest, ResultMatcher resultMatcher) {
    MvcResult mvcResult = mockMvc.perform(MockMvcRequestBuilders.put("/order/" + orderRequest.getCorrelationId() +"/confirm")
                    .accept(MediaType.APPLICATION_JSON))
            .andExpect(resultMatcher)
            .andReturn();
    MILLISECONDS.sleep(SLEEP_TIME_AFTER_REQUEST);
    return mvcResult;
  }
}
