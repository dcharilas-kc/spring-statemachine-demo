package demo.statemachine.exception;

import lombok.extern.log4j.Log4j2;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.context.request.WebRequest;

import java.util.stream.Collectors;

import static org.springframework.http.HttpStatus.BAD_REQUEST;
import static org.springframework.http.HttpStatus.INTERNAL_SERVER_ERROR;

@Log4j2
@ControllerAdvice
public class GlobalExceptionHandler {

  @ResponseBody
  @ResponseStatus(BAD_REQUEST)
  @ExceptionHandler(MethodArgumentNotValidException.class)
  public ResponseEntity<String> handleMethodArgumentNotValidException(final MethodArgumentNotValidException ex, WebRequest request) {
    var debugMessage = ex.getBindingResult().getFieldErrors()
        .stream()
        .map(e -> String.format("'%s':'%s'", e.getField(), e.getDefaultMessage()))
        .collect(Collectors.joining(", "));
    var requestDescription = request.getDescription(false);
    log.warn("Bad Request: {} | Debug: {}", requestDescription, debugMessage);
    return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
  }

  @ResponseBody
  @ResponseStatus(INTERNAL_SERVER_ERROR)
  @ExceptionHandler(Exception.class)
  public ResponseEntity<String> handleException(Exception exception, WebRequest request) {
    if (exception instanceof NullPointerException) {
      log.error(exception.getMessage(), exception);
    } else {
      log.error(exception.getMessage());
      exception.printStackTrace();
    }
    return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
  }
}
