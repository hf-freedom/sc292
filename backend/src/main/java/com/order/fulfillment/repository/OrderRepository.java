package com.order.fulfillment.repository;

import com.order.fulfillment.model.Order;
import com.order.fulfillment.model.OrderStatus;
import com.order.fulfillment.model.RiskLevel;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@Repository
public class OrderRepository {
    private final Map<String, Order> orders = new ConcurrentHashMap<>();

    public Order save(Order order) {
        orders.put(order.getOrderId(), order);
        return order;
    }

    public Order findById(String orderId) {
        return orders.get(orderId);
    }

    public List<Order> findAll() {
        return new ArrayList<>(orders.values());
    }

    public List<Order> findByStatus(OrderStatus status) {
        return orders.values().stream()
                .filter(order -> order.getStatus() == status)
                .collect(Collectors.toList());
    }

    public List<Order> findByRiskLevel(RiskLevel riskLevel) {
        return orders.values().stream()
                .filter(order -> order.getRiskLevel() == riskLevel)
                .collect(Collectors.toList());
    }

    public List<Order> findByFilters(OrderStatus status, RiskLevel riskLevel) {
        return orders.values().stream()
                .filter(order -> {
                    if (status != null && order.getStatus() != status) {
                        return false;
                    }
                    if (riskLevel != null && order.getRiskLevel() != riskLevel) {
                        return false;
                    }
                    return true;
                })
                .collect(Collectors.toList());
    }

    public void delete(String orderId) {
        orders.remove(orderId);
    }

    public int count() {
        return orders.size();
    }
}
