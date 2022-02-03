package demo.statemachine.constant;

import com.fasterxml.jackson.annotation.JsonIgnore;

import java.util.List;

public enum OrderStateEnum {

    INIT, VALIDATION_PENDING, CANCELLED, REJECTED, ACCEPTED, DISPATCHED, DELIVERED , INVENTORY_OK, PAYMENT_OK,

    // internal intermediate states
    FORK, JOIN, INVENTORY_PENDING, PAYMENT_PENDING, DISPATCH_PENDING;

    @JsonIgnore
    public boolean isFinite() {
        return List.of(REJECTED,CANCELLED,DELIVERED).contains(this);
    }

    @JsonIgnore
    public boolean hasPayload() {
        return List.of(INIT,CANCELLED,DELIVERED).contains(this);
    }

    @JsonIgnore
    public boolean skipLog() {
        return List.of(FORK).contains(this);
    }
}
