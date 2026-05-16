package com.order.fulfillment.repository;

import com.order.fulfillment.model.Alert;
import com.order.fulfillment.model.AlertType;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@Repository
public class AlertRepository {
    private final Map<String, Alert> alerts = new ConcurrentHashMap<>();

    public Alert save(Alert alert) {
        alerts.put(alert.getAlertId(), alert);
        return alert;
    }

    public Alert findById(String alertId) {
        return alerts.get(alertId);
    }

    public List<Alert> findByOrderId(String orderId) {
        return alerts.values().stream()
                .filter(alert -> alert.getOrderId().equals(orderId))
                .collect(Collectors.toList());
    }

    public List<Alert> findByNodeId(String nodeId) {
        return alerts.values().stream()
                .filter(alert -> nodeId.equals(alert.getNodeId()))
                .collect(Collectors.toList());
    }

    public List<Alert> findByType(AlertType type) {
        return alerts.values().stream()
                .filter(alert -> alert.getType() == type)
                .collect(Collectors.toList());
    }

    public List<Alert> findByResolved(boolean resolved) {
        return alerts.values().stream()
                .filter(alert -> alert.isResolved() == resolved)
                .collect(Collectors.toList());
    }

    public List<Alert> findByFilters(AlertType type, Boolean resolved) {
        return alerts.values().stream()
                .filter(alert -> {
                    if (type != null && alert.getType() != type) {
                        return false;
                    }
                    if (resolved != null && alert.isResolved() != resolved) {
                        return false;
                    }
                    return true;
                })
                .collect(Collectors.toList());
    }

    public List<Alert> findAll() {
        return new ArrayList<>(alerts.values());
    }

    public void delete(String alertId) {
        alerts.remove(alertId);
    }

    public int countUnresolved() {
        return (int) alerts.values().stream()
                .filter(alert -> !alert.isResolved())
                .count();
    }
}
