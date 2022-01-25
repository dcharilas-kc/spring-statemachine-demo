package demo.statemachine.constant;

import com.fasterxml.jackson.annotation.JsonIgnore;

import java.util.List;

public enum OrderStateEnum {

    INIT, CREATED, UNDER_CHECK, CANCELLED, REJECTED, DISPATCHED, DELIVERED;

    @JsonIgnore
    public boolean isFinite() {
        return List.of(REJECTED,CANCELLED,DELIVERED).contains(this);
    }
}
