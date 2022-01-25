package demo.statemachine.exception;

public class CorrelationIdError extends Exception {

    public CorrelationIdError(String s) {
        super(s);
    }
}
