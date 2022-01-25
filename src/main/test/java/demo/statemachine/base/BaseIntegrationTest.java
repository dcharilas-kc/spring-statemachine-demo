package demo.statemachine.base;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.SneakyThrows;
import org.apache.commons.lang3.ArrayUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.util.TestPropertyValues;
import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.lang.NonNull;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.ResultMatcher;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

public class BaseIntegrationTest implements ApplicationContextInitializer<ConfigurableApplicationContext> {

  public static TestContainer telcoTestContainer = new TestContainer();

  @Autowired
  public MockMvc mockMvc;

  @Autowired
  public ObjectMapper objectMapper;

  @Override
  public void initialize(@NonNull ConfigurableApplicationContext configurableApplicationContext) {
    String[] testProperties = new String[4];
    testProperties[0] = "spring.datasource.url=" + telcoTestContainer.getJdbcUrl() + "?useSSL=false";
    testProperties[1] = "spring.datasource.username=" + telcoTestContainer.getUsername();
    testProperties[2] = "spring.datasource.password=" + telcoTestContainer.getPassword();
    testProperties[3] = "spring.jpa.hibernate.ddl-auto=update";

    String[] mergedProperties = ArrayUtils.addAll(testProperties, getAdditionalProperties());
    TestPropertyValues.of(mergedProperties).applyTo(configurableApplicationContext.getEnvironment());
  }

  protected String[] getAdditionalProperties() {
    return new String[0];
  }

  @SneakyThrows
  protected MvcResult sendAndExpect(HttpMethod httpMethod, String uri, Object body, ResultMatcher resultMatcher) {
    return mockMvc.perform(
        MockMvcRequestBuilders.request(httpMethod, uri)
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(body))
            .accept(MediaType.APPLICATION_JSON))
        .andExpect(resultMatcher)
        .andReturn();
  }
}
