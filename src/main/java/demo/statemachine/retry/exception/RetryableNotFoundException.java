package demo.statemachine.retry.exception;

public class RetryableNotFoundException extends RuntimeException {
  
  public RetryableNotFoundException(String message) {
    super(message);
  }
}
