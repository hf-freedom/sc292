package com.order.fulfillment.controller;

import com.order.fulfillment.model.*;
import com.order.fulfillment.service.NodeService;
import com.order.fulfillment.service.OrderService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/orders")
@CrossOrigin(origins = "http://localhost:3005")
public class OrderController {

    @Autowired
    private OrderService orderService;

    @Autowired
    private NodeService nodeService;

    @PostMapping
    public ResponseEntity<Map<String, Object>> createOrder(@RequestBody Map<String, String> request) {
        String productInfo = request.get("productInfo");
        String shippingAddress = request.get("shippingAddress");

        Order order = orderService.createOrder(productInfo, shippingAddress);

        Map<String, Object> data = new HashMap<>();
        data.put("orderId", order.getOrderId());
        data.put("orderNumber", order.getOrderNumber());

        Map<String, Object> result = new HashMap<>();
        result.put("code", 200);
        result.put("message", "订单创建成功");
        result.put("data", data);

        return ResponseEntity.ok(result);
    }

    @GetMapping
    public ResponseEntity<Map<String, Object>> getOrders(
            @RequestParam(required = false) OrderStatus status,
            @RequestParam(required = false) RiskLevel riskLevel,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {

        List<Order> orders = orderService.findByFilters(status, riskLevel);

        int start = page * size;
        int end = Math.min(start + size, orders.size());
        List<Order> pagedOrders = start < orders.size() ? orders.subList(start, end) : List.of();

        List<Map<String, Object>> orderSummaries = pagedOrders.stream()
                .map(order -> {
                    List<FulfillmentNode> nodes = nodeService.findByOrderId(order.getOrderId());
                    FulfillmentNode currentNode = null;
                    NodeType currentNodeType = NodeType.PAYMENT;

                    if (order.getCurrentNodeId() != null) {
                        currentNode = nodeService.findById(order.getCurrentNodeId());
                        if (currentNode != null) {
                            currentNodeType = currentNode.getType();
                        }
                    }

                    Map<String, Object> summary = new HashMap<>();
                    summary.put("orderId", order.getOrderId());
                    summary.put("orderNumber", order.getOrderNumber());
                    summary.put("productInfo", order.getProductInfo());
                    summary.put("status", order.getStatus());
                    summary.put("riskLevel", order.getRiskLevel());
                    summary.put("currentNodeType", currentNodeType);
                    summary.put("estimatedCompleteTime", order.getEstimatedCompleteTime() != null ?
                            order.getEstimatedCompleteTime().toString() : "");
                    summary.put("createTime", order.getCreateTime().toString());
                    summary.put("cancelled", order.isCancelled());

                    return summary;
                })
                .collect(Collectors.toList());

        Map<String, Object> data = new HashMap<>();
        data.put("content", orderSummaries);
        data.put("totalElements", orders.size());
        data.put("totalPages", (int) Math.ceil((double) orders.size() / size));

        Map<String, Object> result = new HashMap<>();
        result.put("code", 200);
        result.put("message", "success");
        result.put("data", data);

        return ResponseEntity.ok(result);
    }

    @GetMapping("/{orderId}")
    public ResponseEntity<Map<String, Object>> getOrderDetail(@PathVariable String orderId) {
        Map<String, Object> detail = orderService.getOrderDetail(orderId);
        Order order = (Order) detail.get("order");
        List<FulfillmentNode> nodes = (List<FulfillmentNode>) detail.get("nodes");

        List<Map<String, Object>> nodeDetails = nodes.stream()
                .map(node -> {
                    Map<String, Object> nodeMap = new HashMap<>();
                    nodeMap.put("nodeId", node.getNodeId());
                    nodeMap.put("type", node.getType());
                    nodeMap.put("status", node.getStatus());
                    nodeMap.put("maxRetries", node.getMaxRetries());
                    nodeMap.put("retryCount", node.getRetryCount());
                    nodeMap.put("startTime", node.getStartTime() != null ? node.getStartTime().toString() : "");
                    nodeMap.put("estimatedCompleteTime", node.getEstimatedCompleteTime() != null ?
                            node.getEstimatedCompleteTime().toString() : "");
                    nodeMap.put("actualCompleteTime", node.getActualCompleteTime() != null ?
                            node.getActualCompleteTime().toString() : "");
                    nodeMap.put("errorMessage", node.getErrorMessage() != null ? node.getErrorMessage() : "");
                    nodeMap.put("previousNodeId", node.getPreviousNodeId() != null ? node.getPreviousNodeId() : "");
                    nodeMap.put("nextNodeId", node.getNextNodeId() != null ? node.getNextNodeId() : "");

                    List<Map<String, String>> logs = node.getOperationLogs().stream()
                            .map(log -> {
                                Map<String, String> logMap = new HashMap<>();
                                logMap.put("time", log.getTime().toString());
                                logMap.put("action", log.getAction());
                                logMap.put("operator", log.getOperator());
                                logMap.put("message", log.getMessage());
                                return logMap;
                            })
                            .collect(Collectors.toList());

                    nodeMap.put("operationLogs", logs);

                    return nodeMap;
                })
                .collect(Collectors.toList());

        Map<String, Object> orderMap = new HashMap<>();
        orderMap.put("orderId", order.getOrderId());
        orderMap.put("orderNumber", order.getOrderNumber());
        orderMap.put("productInfo", order.getProductInfo());
        orderMap.put("shippingAddress", order.getShippingAddress());
        orderMap.put("status", order.getStatus());
        orderMap.put("riskLevel", order.getRiskLevel());
        orderMap.put("createTime", order.getCreateTime().toString());
        orderMap.put("estimatedCompleteTime", order.getEstimatedCompleteTime() != null ?
                order.getEstimatedCompleteTime().toString() : "");
        orderMap.put("cancelled", order.isCancelled());
        orderMap.put("cancellationReason", order.getCancellationReason() != null ?
                order.getCancellationReason() : "");
        orderMap.put("compensations", order.getCompensations());

        Map<String, Object> data = new HashMap<>();
        data.put("order", orderMap);
        data.put("nodes", nodeDetails);

        Map<String, Object> result = new HashMap<>();
        result.put("code", 200);
        result.put("message", "success");
        result.put("data", data);

        return ResponseEntity.ok(result);
    }

    @PostMapping("/{orderId}/cancel")
    public ResponseEntity<Map<String, Object>> cancelOrder(
            @PathVariable String orderId,
            @RequestBody Map<String, String> request) {

        String reason = request.get("reason");
        Order order = orderService.cancelOrder(orderId, reason);

        boolean compensationExecuted = !order.getCompensations().isEmpty();
        String compensationType = compensationExecuted ?
                order.getCompensations().get(order.getCompensations().size() - 1).getType() : "NONE";

        Map<String, Object> data = new HashMap<>();
        data.put("cancelled", order.isCancelled());
        data.put("compensationExecuted", compensationExecuted);
        data.put("compensationType", compensationType);

        Map<String, Object> result = new HashMap<>();
        result.put("code", 200);
        result.put("message", "订单已取消");
        result.put("data", data);

        return ResponseEntity.ok(result);
    }
}
