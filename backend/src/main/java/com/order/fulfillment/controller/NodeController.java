package com.order.fulfillment.controller;

import com.order.fulfillment.model.FulfillmentNode;
import com.order.fulfillment.service.NodeService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/nodes")
@CrossOrigin(origins = "http://localhost:3005")
public class NodeController {

    @Autowired
    private NodeService nodeService;

    @PostMapping("/{nodeId}/retry")
    public ResponseEntity<Map<String, Object>> retryNode(@PathVariable String nodeId) {
        FulfillmentNode node = nodeService.retryNode(nodeId);

        Map<String, Object> data = new HashMap<>();
        data.put("nodeId", node.getNodeId());
        data.put("status", node.getStatus().toString());
        data.put("retryCount", node.getRetryCount());
        data.put("maxRetries", node.getMaxRetries());

        Map<String, Object> result = new HashMap<>();
        result.put("code", 200);
        result.put("message", "节点已重新执行");
        result.put("data", data);

        return ResponseEntity.ok(result);
    }

    @PostMapping("/{nodeId}/complete")
    public ResponseEntity<Map<String, Object>> completeNode(@PathVariable String nodeId) {
        FulfillmentNode node = nodeService.completeNode(nodeId);

        Map<String, Object> data = new HashMap<>();
        data.put("nodeId", node.getNodeId());
        data.put("status", node.getStatus().toString());

        Map<String, Object> result = new HashMap<>();
        result.put("code", 200);
        result.put("message", "节点已完成");
        result.put("data", data);

        return ResponseEntity.ok(result);
    }

    @PostMapping("/{nodeId}/fail")
    public ResponseEntity<Map<String, Object>> failNode(
            @PathVariable String nodeId,
            @RequestBody(required = false) Map<String, String> request) {

        String errorMessage = request != null && request.containsKey("message") 
                ? request.get("message") 
                : "模拟失败：用于测试重试功能";

        FulfillmentNode node = nodeService.failNode(nodeId, errorMessage);

        Map<String, Object> data = new HashMap<>();
        data.put("nodeId", node.getNodeId());
        data.put("status", node.getStatus().toString());
        data.put("errorMessage", node.getErrorMessage());
        data.put("retryCount", node.getRetryCount());
        data.put("maxRetries", node.getMaxRetries());

        Map<String, Object> result = new HashMap<>();
        result.put("code", 200);
        result.put("message", "节点已标记为失败");
        result.put("data", data);

        return ResponseEntity.ok(result);
    }
}
