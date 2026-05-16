package com.order.fulfillment.model;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class Order {
    private String orderId;
    private String orderNumber;
    private String productInfo;
    private String shippingAddress;
    private OrderStatus status;
    private RiskLevel riskLevel;
    private LocalDateTime createTime;
    private LocalDateTime estimatedCompleteTime;
    private String currentNodeId;
    private List<String> nodeIds;
    private boolean cancelled;
    private String cancellationReason;
    private List<CompensationRecord> compensations;

    public Order() {
        this.orderId = UUID.randomUUID().toString();
        this.status = OrderStatus.PENDING;
        this.riskLevel = RiskLevel.LOW;
        this.createTime = LocalDateTime.now();
        this.nodeIds = new ArrayList<>();
        this.compensations = new ArrayList<>();
        this.cancelled = false;
        this.orderNumber = generateOrderNumber();
    }

    private String generateOrderNumber() {
        return "ORD" + System.currentTimeMillis();
    }

    public String getOrderId() {
        return orderId;
    }

    public void setOrderId(String orderId) {
        this.orderId = orderId;
    }

    public String getOrderNumber() {
        return orderNumber;
    }

    public void setOrderNumber(String orderNumber) {
        this.orderNumber = orderNumber;
    }

    public String getProductInfo() {
        return productInfo;
    }

    public void setProductInfo(String productInfo) {
        this.productInfo = productInfo;
    }

    public String getShippingAddress() {
        return shippingAddress;
    }

    public void setShippingAddress(String shippingAddress) {
        this.shippingAddress = shippingAddress;
    }

    public OrderStatus getStatus() {
        return status;
    }

    public void setStatus(OrderStatus status) {
        this.status = status;
    }

    public RiskLevel getRiskLevel() {
        return riskLevel;
    }

    public void setRiskLevel(RiskLevel riskLevel) {
        this.riskLevel = riskLevel;
    }

    public LocalDateTime getCreateTime() {
        return createTime;
    }

    public void setCreateTime(LocalDateTime createTime) {
        this.createTime = createTime;
    }

    public LocalDateTime getEstimatedCompleteTime() {
        return estimatedCompleteTime;
    }

    public void setEstimatedCompleteTime(LocalDateTime estimatedCompleteTime) {
        this.estimatedCompleteTime = estimatedCompleteTime;
    }

    public String getCurrentNodeId() {
        return currentNodeId;
    }

    public void setCurrentNodeId(String currentNodeId) {
        this.currentNodeId = currentNodeId;
    }

    public List<String> getNodeIds() {
        return nodeIds;
    }

    public void setNodeIds(List<String> nodeIds) {
        this.nodeIds = nodeIds;
    }

    public boolean isCancelled() {
        return cancelled;
    }

    public void setCancelled(boolean cancelled) {
        this.cancelled = cancelled;
    }

    public String getCancellationReason() {
        return cancellationReason;
    }

    public void setCancellationReason(String cancellationReason) {
        this.cancellationReason = cancellationReason;
    }

    public List<CompensationRecord> getCompensations() {
        return compensations;
    }

    public void setCompensations(List<CompensationRecord> compensations) {
        this.compensations = compensations;
    }

    public void addCompensation(CompensationRecord record) {
        this.compensations.add(record);
    }
}
