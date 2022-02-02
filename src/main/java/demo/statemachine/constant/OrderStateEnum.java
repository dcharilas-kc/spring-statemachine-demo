package demo.statemachine.constant;

import com.fasterxml.jackson.annotation.JsonIgnore;

import java.util.List;

public enum OrderStateEnum {

    FORK, JOIN, INIT, CREATED, VALIDATION_PENDING, CANCELLED, REJECTED, ACCEPTED, DISPATCHED, DELIVERED, INVENTORY_PENDING, INVENTORY_OK, PAYMENT_PENDING, PAYMENT_OK, DISPATCH_PENDING;

    @JsonIgnore
    public boolean isFinite() {
        return List.of(REJECTED,CANCELLED,DELIVERED).contains(this);
    }
}
