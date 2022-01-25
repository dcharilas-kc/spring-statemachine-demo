package demo.statemachine.base;

import org.testcontainers.containers.MySQLContainer;

public class TestContainer extends MySQLContainer<TestContainer> {
  public TestContainer() {
    this.start();
  }
}
