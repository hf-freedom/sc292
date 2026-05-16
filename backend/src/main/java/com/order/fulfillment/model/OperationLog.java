package com.order.fulfillment.model;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class OperationLog {
    private LocalDateTime time;
    private String action;
    private String operator;
    private String message;

    public OperationLog() {
        this.time = LocalDateTime.now();
    }

    public OperationLog(String action, String operator, String message) {
        this.time = LocalDateTime.now();
        this.action = action;
        this.operator = operator;
        this.message = message;
    }

    public LocalDateTime getTime() {
        return time;
    }

    public void setTime(LocalDateTime time) {
        this.time = time;
    }

    public String getAction() {
        return action;
    }

    public void setAction(String action) {
        this.action = action;
    }

    public String getOperator() {
        return operator;
    }

    public void setOperator(String operator) {
        this.operator = operator;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }
}
