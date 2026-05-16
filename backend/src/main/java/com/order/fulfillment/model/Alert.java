package com.order.fulfillment.model;

import java.time.LocalDateTime;
import java.util.UUID;

public class Alert {
    private String alertId;
    private String orderId;
    private String nodeId;
    private AlertType type;
    private AlertLevel level;
    private String message;
    private LocalDateTime createTime;
    private boolean resolved;
    private String resolvedBy;
    private LocalDateTime resolvedTime;
    private String comment;

    public Alert() {
        this.alertId = UUID.randomUUID().toString();
        this.createTime = LocalDateTime.now();
        this.resolved = false;
    }

    public static Alert createTimeoutAlert(String orderId, String nodeId, String message) {
        Alert alert = new Alert();
        alert.setOrderId(orderId);
        alert.setNodeId(nodeId);
        alert.setType(AlertType.TIMEOUT);
        alert.setLevel(AlertLevel.WARNING);
        alert.setMessage(message);
        return alert;
    }

    public static Alert createStuckAlert(String orderId, String nodeId, String message) {
        Alert alert = new Alert();
        alert.setOrderId(orderId);
        alert.setNodeId(nodeId);
        alert.setType(AlertType.STUCK);
        alert.setLevel(AlertLevel.ERROR);
        alert.setMessage(message);
        return alert;
    }

    public static Alert createFailureAlert(String orderId, String nodeId, String message) {
        Alert alert = new Alert();
        alert.setOrderId(orderId);
        alert.setNodeId(nodeId);
        alert.setType(AlertType.FAILURE);
        alert.setLevel(AlertLevel.ERROR);
        alert.setMessage(message);
        return alert;
    }

    public static Alert createRiskUpAlert(String orderId, String message) {
        Alert alert = new Alert();
        alert.setOrderId(orderId);
        alert.setNodeId(null);
        alert.setType(AlertType.RISK_UP);
        alert.setLevel(AlertLevel.CRITICAL);
        alert.setMessage(message);
        return alert;
    }

    public String getAlertId() {
        return alertId;
    }

    public void setAlertId(String alertId) {
        this.alertId = alertId;
    }

    public String getOrderId() {
        return orderId;
    }

    public void setOrderId(String orderId) {
        this.orderId = orderId;
    }

    public String getNodeId() {
        return nodeId;
    }

    public void setNodeId(String nodeId) {
        this.nodeId = nodeId;
    }

    public AlertType getType() {
        return type;
    }

    public void setType(AlertType type) {
        this.type = type;
    }

    public AlertLevel getLevel() {
        return level;
    }

    public void setLevel(AlertLevel level) {
        this.level = level;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public LocalDateTime getCreateTime() {
        return createTime;
    }

    public void setCreateTime(LocalDateTime createTime) {
        this.createTime = createTime;
    }

    public boolean isResolved() {
        return resolved;
    }

    public void setResolved(boolean resolved) {
        this.resolved = resolved;
    }

    public String getResolvedBy() {
        return resolvedBy;
    }

    public void setResolvedBy(String resolvedBy) {
        this.resolvedBy = resolvedBy;
    }

    public LocalDateTime getResolvedTime() {
        return resolvedTime;
    }

    public void setResolvedTime(LocalDateTime resolvedTime) {
        this.resolvedTime = resolvedTime;
    }

    public String getComment() {
        return comment;
    }

    public void setComment(String comment) {
        this.comment = comment;
    }
}
