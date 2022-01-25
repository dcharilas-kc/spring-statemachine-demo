package demo.statemachine.base;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.core.env.Environment;

@TestConfiguration
public class BaseIntegrationTestConfig {
  
  @Autowired
  private Environment environment;

}
