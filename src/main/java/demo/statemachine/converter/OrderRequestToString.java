package demo.statemachine.converter;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import demo.statemachine.model.OrderRequest;
import lombok.AllArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.log4j.Log4j2;
import org.springframework.core.convert.converter.Converter;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Component;

import static java.util.Objects.isNull;

@Log4j2
@AllArgsConstructor
@Component
public class OrderRequestToString implements Converter<OrderRequest, String> {
  
  private final ObjectMapper objectMapper;
  
  @SneakyThrows
  @Override
  public String convert(@Nullable OrderRequest source) {
    if (isNull(source)) {
      return null;
    }
    try {
      return objectMapper.writeValueAsString(source);
    } catch (JsonProcessingException e) {
      log.error("Failed to convert OrderRequest to String. Message: {}", e.getMessage());
      throw new Exception("CONVERSION_ERROR");
    }
  }
  
}
