package com.order.fulfillment.service;

import com.order.fulfillment.model.*;
import com.order.fulfillment.repository.AlertRepository;
import com.order.fulfillment.repository.NodeRepository;
import com.order.fulfillment.repository.OrderRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;

@Service
public class AlertService {

    @Autowired
    private AlertRepository alertRepository;

    @Autowired
    private NodeRepository nodeRepository;

    @Autowired
    private OrderRepository orderRepository;

    public Alert createAlert(Alert alert) {
        return alertRepository.save(alert);
    }

    public Alert findById(String alertId) {
        return alertRepository.findById(alertId);
    }

    public List<Alert> findByOrderId(String orderId) {
        return alertRepository.findByOrderId(orderId);
    }

    public List<Alert> findByFilters(AlertType type, Boolean resolved) {
        return alertRepository.findByFilters(type, resolved);
    }

    public List<Alert> findAll() {
        return alertRepository.findAll();
    }

    public Alert resolve(String alertId, String resolvedBy, String comment) {
        Alert alert = alertRepository.findById(alertId);
        if (alert != null) {
            alert.setResolved(true);
            alert.setResolvedBy(resolvedBy);
            alert.setResolvedTime(LocalDateTime.now());
            alert.setComment(comment);
            alertRepository.save(alert);
        }
        return alert;
    }

    public int countUnresolved() {
        return alertRepository.countUnresolved();
    }

    public void checkAndCreateTimeoutAlerts() {
        List<FulfillmentNode> processingNodes = nodeRepository.findByStatus(NodeStatus.PROCESSING);

        for (FulfillmentNode node : processingNodes) {
            if (node.getEstimatedCompleteTime() != null &&
                LocalDateTime.now().isAfter(node.getEstimatedCompleteTime())) {

                List<Alert> existingAlerts = alertRepository.findByNodeId(node.getNodeId());
                boolean hasTimeoutAlert = existingAlerts.stream()
                        .anyMatch(a -> a.getType() == AlertType.TIMEOUT && !a.isResolved());

                if (!hasTimeoutAlert) {
                    Alert alert = Alert.createTimeoutAlert(
                            node.getOrderId(),
                            node.getNodeId(),
                            node.getType() + "节点已超时"
                    );
                    alertRepository.save(alert);

                    node.setStatus(NodeStatus.TIMEOUT);
                    nodeRepository.save(node);

                    updateOrderRiskLevel(node.getOrderId());
                }
            }
        }
    }

    public void checkAndCreateStuckAlerts() {
        List<FulfillmentNode> processingNodes = nodeRepository.findByStatus(NodeStatus.PROCESSING);

        for (FulfillmentNode node : processingNodes) {
            if (node.getStartTime() != null) {
                long minutesElapsed = ChronoUnit.MINUTES.between(node.getStartTime(), LocalDateTime.now());

                long expectedMinutes = 0;
                switch (node.getType()) {
                    case PAYMENT:
                        expectedMinutes = 30;
                        break;
                    case PREPARING:
                        expectedMinutes = 24 * 60;
                        break;
                    case SHIPPING:
                        expectedMinutes = 48 * 60;
                        break;
                    case SIGNED:
                        expectedMinutes = 7 * 24 * 60;
                        break;
                }

                if (minutesElapsed > expectedMinutes * 2) {
                    List<Alert> existingAlerts = alertRepository.findByNodeId(node.getNodeId());
                    boolean hasStuckAlert = existingAlerts.stream()
                            .anyMatch(a -> a.getType() == AlertType.STUCK && !a.isResolved());

                    if (!hasStuckAlert) {
                        Alert alert = Alert.createStuckAlert(
                                node.getOrderId(),
                                node.getNodeId(),
                                node.getType() + "节点可能卡住，请检查"
                        );
                        alertRepository.save(alert);
                    }
                }
            }
        }
    }

    private void updateOrderRiskLevel(String orderId) {
        Order order = orderRepository.findById(orderId);
        if (order != null) {
            List<Alert> orderAlerts = alertRepository.findByOrderId(orderId);
            long unresolvedCount = orderAlerts.stream()
                    .filter(a -> !a.isResolved())
                    .count();

            RiskLevel newLevel;
            if (unresolvedCount >= 3) {
                newLevel = RiskLevel.CRITICAL;
            } else if (unresolvedCount == 2) {
                newLevel = RiskLevel.HIGH;
            } else if (unresolvedCount == 1) {
                newLevel = RiskLevel.MEDIUM;
            } else {
                newLevel = RiskLevel.LOW;
            }

            if (newLevel.ordinal() > order.getRiskLevel().ordinal()) {
                order.setRiskLevel(newLevel);
                orderRepository.save(order);

                Alert riskAlert = Alert.createRiskUpAlert(
                        orderId,
                        "订单风险等级从" + order.getRiskLevel() + "升至" + newLevel
                );
                alertRepository.save(riskAlert);
            }
        }
    }
}
