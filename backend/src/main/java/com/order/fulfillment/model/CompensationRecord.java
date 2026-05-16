package com.order.fulfillment.model;

import java.time.LocalDateTime;

public class CompensationRecord {
    private String type;
    private String description;
    private LocalDateTime time;
    private boolean executed;

    public CompensationRecord() {
        this.time = LocalDateTime.now();
        this.executed = false;
    }

    public CompensationRecord(String type, String description) {
        this();
        this.type = type;
        this.description = description;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public LocalDateTime getTime() {
        return time;
    }

    public void setTime(LocalDateTime time) {
        this.time = time;
    }

    public boolean isExecuted() {
        return executed;
    }

    public void setExecuted(boolean executed) {
        this.executed = executed;
    }
}
