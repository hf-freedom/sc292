package com.order.fulfillment.repository;

import com.order.fulfillment.model.FulfillmentNode;
import com.order.fulfillment.model.NodeStatus;
import com.order.fulfillment.model.NodeType;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@Repository
public class NodeRepository {
    private final Map<String, FulfillmentNode> nodes = new ConcurrentHashMap<>();

    private static final List<NodeType> NODE_ORDER = List.of(
            NodeType.PAYMENT,
            NodeType.PREPARING,
            NodeType.SHIPPING,
            NodeType.SIGNED
    );

    public FulfillmentNode save(FulfillmentNode node) {
        nodes.put(node.getNodeId(), node);
        return node;
    }

    public FulfillmentNode findById(String nodeId) {
        return nodes.get(nodeId);
    }

    public List<FulfillmentNode> findByOrderId(String orderId) {
        return nodes.values().stream()
                .filter(node -> node.getOrderId().equals(orderId))
                .sorted(Comparator.comparingInt(n -> NODE_ORDER.indexOf(n.getType())))
                .collect(Collectors.toList());
    }

    public List<FulfillmentNode> findByStatus(NodeStatus status) {
        return nodes.values().stream()
                .filter(node -> node.getStatus() == status)
                .collect(Collectors.toList());
    }

    public List<FulfillmentNode> findAll() {
        return new ArrayList<>(nodes.values());
    }

    public void delete(String nodeId) {
        nodes.remove(nodeId);
    }

    public void deleteByOrderId(String orderId) {
        nodes.entrySet().removeIf(entry -> entry.getValue().getOrderId().equals(orderId));
    }
}
