package com.order.fulfillment.model;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class FulfillmentNode {
    private String nodeId;
    private String orderId;
    private NodeType type;
    private NodeStatus status;
    private int maxRetries;
    private int retryCount;
    private LocalDateTime startTime;
    private LocalDateTime estimatedCompleteTime;
    private LocalDateTime actualCompleteTime;
    private String errorMessage;
    private List<OperationLog> operationLogs;
    private String previousNodeId;
    private String nextNodeId;

    public FulfillmentNode() {
        this.nodeId = UUID.randomUUID().toString();
        this.status = NodeStatus.PENDING;
        this.maxRetries = 3;
        this.retryCount = 0;
        this.operationLogs = new ArrayList<>();
    }

    public static FulfillmentNode createPaymentNode(String orderId) {
        FulfillmentNode node = new FulfillmentNode();
        node.setOrderId(orderId);
        node.setType(NodeType.PAYMENT);
        node.setEstimatedCompleteTime(LocalDateTime.now().plusMinutes(30));
        return node;
    }

    public static FulfillmentNode createPreparingNode(String orderId, String previousNodeId) {
        FulfillmentNode node = new FulfillmentNode();
        node.setOrderId(orderId);
        node.setType(NodeType.PREPARING);
        node.setPreviousNodeId(previousNodeId);
        node.setEstimatedCompleteTime(LocalDateTime.now().plusHours(24));
        return node;
    }

    public static FulfillmentNode createShippingNode(String orderId, String previousNodeId) {
        FulfillmentNode node = new FulfillmentNode();
        node.setOrderId(orderId);
        node.setType(NodeType.SHIPPING);
        node.setPreviousNodeId(previousNodeId);
        node.setEstimatedCompleteTime(LocalDateTime.now().plusHours(48));
        return node;
    }

    public static FulfillmentNode createSignedNode(String orderId, String previousNodeId) {
        FulfillmentNode node = new FulfillmentNode();
        node.setOrderId(orderId);
        node.setType(NodeType.SIGNED);
        node.setPreviousNodeId(previousNodeId);
        node.setEstimatedCompleteTime(LocalDateTime.now().plusDays(7));
        return node;
    }

    public String getNodeId() {
        return nodeId;
    }

    public void setNodeId(String nodeId) {
        this.nodeId = nodeId;
    }

    public String getOrderId() {
        return orderId;
    }

    public void setOrderId(String orderId) {
        this.orderId = orderId;
    }

    public NodeType getType() {
        return type;
    }

    public void setType(NodeType type) {
        this.type = type;
    }

    public NodeStatus getStatus() {
        return status;
    }

    public void setStatus(NodeStatus status) {
        this.status = status;
    }

    public int getMaxRetries() {
        return maxRetries;
    }

    public void setMaxRetries(int maxRetries) {
        this.maxRetries = maxRetries;
    }

    public int getRetryCount() {
        return retryCount;
    }

    public void setRetryCount(int retryCount) {
        this.retryCount = retryCount;
    }

    public LocalDateTime getStartTime() {
        return startTime;
    }

    public void setStartTime(LocalDateTime startTime) {
        this.startTime = startTime;
    }

    public LocalDateTime getEstimatedCompleteTime() {
        return estimatedCompleteTime;
    }

    public void setEstimatedCompleteTime(LocalDateTime estimatedCompleteTime) {
        this.estimatedCompleteTime = estimatedCompleteTime;
    }

    public LocalDateTime getActualCompleteTime() {
        return actualCompleteTime;
    }

    public void setActualCompleteTime(LocalDateTime actualCompleteTime) {
        this.actualCompleteTime = actualCompleteTime;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }

    public List<OperationLog> getOperationLogs() {
        return operationLogs;
    }

    public void setOperationLogs(List<OperationLog> operationLogs) {
        this.operationLogs = operationLogs;
    }

    public String getPreviousNodeId() {
        return previousNodeId;
    }

    public void setPreviousNodeId(String previousNodeId) {
        this.previousNodeId = previousNodeId;
    }

    public String getNextNodeId() {
        return nextNodeId;
    }

    public void setNextNodeId(String nextNodeId) {
        this.nextNodeId = nextNodeId;
    }

    public void addLog(String action, String operator, String message) {
        this.operationLogs.add(new OperationLog(action, operator, message));
    }
}
