package com.order.fulfillment.service;

import com.order.fulfillment.model.*;
import com.order.fulfillment.repository.NodeRepository;
import com.order.fulfillment.repository.OrderRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class NodeService {

    @Autowired
    private NodeRepository nodeRepository;

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private AlertService alertService;

    public FulfillmentNode findById(String nodeId) {
        return nodeRepository.findById(nodeId);
    }

    public List<FulfillmentNode> findByOrderId(String orderId) {
        return nodeRepository.findByOrderId(orderId);
    }

    public FulfillmentNode retryNode(String nodeId) {
        FulfillmentNode node = nodeRepository.findById(nodeId);
        if (node != null && (node.getStatus() == NodeStatus.FAILED || node.getStatus() == NodeStatus.TIMEOUT)) {
            if (node.getRetryCount() < node.getMaxRetries()) {
                node.setStatus(NodeStatus.PROCESSING);
                node.setRetryCount(node.getRetryCount() + 1);
                node.setErrorMessage(null);
                node.addLog("RETRY", "SYSTEM", "节点重试，第" + node.getRetryCount() + "次");
                nodeRepository.save(node);
            } else {
                node.setStatus(NodeStatus.MANUAL);
                node.addLog("MANUAL", "SYSTEM", "重试次数耗尽，进入人工处理");
                nodeRepository.save(node);
            }
        }
        return node;
    }

    public FulfillmentNode completeNode(String nodeId) {
        FulfillmentNode node = nodeRepository.findById(nodeId);
        if (node != null) {
            node.setStatus(NodeStatus.COMPLETED);
            node.setActualCompleteTime(LocalDateTime.now());
            node.addLog("COMPLETE", "SYSTEM", "节点已完成");
            nodeRepository.save(node);

            if (node.getNextNodeId() != null) {
                activateNextNode(node.getNextNodeId());
            } else {
                completeOrder(node.getOrderId());
            }
        }
        return node;
    }

    public void activateNextNode(String nextNodeId) {
        FulfillmentNode nextNode = nodeRepository.findById(nextNodeId);
        if (nextNode != null && nextNode.getStatus() == NodeStatus.PENDING) {
            nextNode.setStatus(NodeStatus.PROCESSING);
            nextNode.setStartTime(LocalDateTime.now());
            nextNode.addLog("ACTIVATE", "SYSTEM", "节点已激活");
            nodeRepository.save(nextNode);

            Order order = orderRepository.findById(nextNode.getOrderId());
            if (order != null) {
                order.setCurrentNodeId(nextNodeId);
                order.setStatus(OrderStatus.PROCESSING);
                orderRepository.save(order);
            }
        }
    }

    private void completeOrder(String orderId) {
        Order order = orderRepository.findById(orderId);
        if (order != null) {
            order.setStatus(OrderStatus.COMPLETED);
            order.setEstimatedCompleteTime(LocalDateTime.now());
            orderRepository.save(order);
        }
    }

    public void advanceToNextNode(String orderId) {
        List<FulfillmentNode> nodes = nodeRepository.findByOrderId(orderId);

        for (FulfillmentNode node : nodes) {
            if (node.getStatus() == NodeStatus.COMPLETED && node.getNextNodeId() != null) {
                activateNextNode(node.getNextNodeId());
                break;
            }
        }
    }

    public LocalDateTime calculateEstimatedTime(Order order) {
        List<FulfillmentNode> nodes = nodeRepository.findByOrderId(order.getOrderId());

        boolean hasManualNode = nodes.stream()
                .anyMatch(n -> n.getStatus() == NodeStatus.MANUAL);

        if (hasManualNode) {
            return null;
        }

        long extraHours = 0;
        for (FulfillmentNode node : nodes) {
            if (node.getStatus() == NodeStatus.FAILED) {
                extraHours += 2;
            } else if (node.getStatus() == NodeStatus.TIMEOUT) {
                extraHours += 5;
            }
        }

        return LocalDateTime.now().plusHours(48 + extraHours);
    }

    public void checkAndAdvanceNodes() {
        List<Order> activeOrders = orderRepository.findAll();

        for (Order order : activeOrders) {
            if (order.getStatus() == OrderStatus.PROCESSING && !order.isCancelled()) {
                advanceToNextNode(order.getOrderId());
            }
        }
    }

    public FulfillmentNode failNode(String nodeId, String errorMessage) {
        FulfillmentNode node = nodeRepository.findById(nodeId);
        if (node != null && node.getStatus() == NodeStatus.PROCESSING) {
            node.setStatus(NodeStatus.FAILED);
            node.setErrorMessage(errorMessage);
            node.addLog("FAIL", "SYSTEM", "节点失败: " + errorMessage);
            nodeRepository.save(node);

            Alert alert = Alert.createFailureAlert(
                    node.getOrderId(),
                    node.getNodeId(),
                    node.getType() + "节点失败: " + errorMessage
            );
            alertService.createAlert(alert);
        }
        return node;
    }
}
