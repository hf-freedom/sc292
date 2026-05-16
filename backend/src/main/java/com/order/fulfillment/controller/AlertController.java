package com.order.fulfillment.controller;

import com.order.fulfillment.model.Alert;
import com.order.fulfillment.model.AlertType;
import com.order.fulfillment.service.AlertService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/alerts")
@CrossOrigin(origins = "http://localhost:3005")
public class AlertController {

    @Autowired
    private AlertService alertService;

    @GetMapping
    public ResponseEntity<Map<String, Object>> getAlerts(
            @RequestParam(required = false) AlertType type,
            @RequestParam(required = false) Boolean resolved,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {

        List<Alert> alerts = alertService.findByFilters(type, resolved);

        int start = page * size;
        int end = Math.min(start + size, alerts.size());
        List<Alert> pagedAlerts = start < alerts.size() ? alerts.subList(start, end) : List.of();

        List<Map<String, Object>> alertDetails = pagedAlerts.stream()
                .map(alert -> {
                    Map<String, Object> alertMap = new HashMap<>();
                    alertMap.put("alertId", alert.getAlertId());
                    alertMap.put("orderId", alert.getOrderId());
                    alertMap.put("nodeId", alert.getNodeId() != null ? alert.getNodeId() : "");
                    alertMap.put("type", alert.getType());
                    alertMap.put("level", alert.getLevel());
                    alertMap.put("message", alert.getMessage());
                    alertMap.put("createTime", alert.getCreateTime().toString());
                    alertMap.put("resolved", alert.isResolved());
                    alertMap.put("resolvedBy", alert.getResolvedBy() != null ? alert.getResolvedBy() : "");
                    alertMap.put("resolvedTime", alert.getResolvedTime() != null ? alert.getResolvedTime().toString() : "");
                    alertMap.put("comment", alert.getComment() != null ? alert.getComment() : "");
                    return alertMap;
                })
                .collect(Collectors.toList());

        Map<String, Object> data = new HashMap<>();
        data.put("content", alertDetails);
        data.put("totalElements", alerts.size());
        data.put("totalPages", (int) Math.ceil((double) alerts.size() / size));

        Map<String, Object> result = new HashMap<>();
        result.put("code", 200);
        result.put("message", "success");
        result.put("data", data);

        return ResponseEntity.ok(result);
    }

    @PostMapping("/{alertId}/resolve")
    public ResponseEntity<Map<String, Object>> resolveAlert(
            @PathVariable String alertId,
            @RequestBody Map<String, String> request) {

        String resolvedBy = request.get("resolvedBy");
        String comment = request.get("comment");

        alertService.resolve(alertId, resolvedBy, comment);

        Map<String, Object> data = new HashMap<>();
        data.put("alertId", alertId);

        Map<String, Object> result = new HashMap<>();
        result.put("code", 200);
        result.put("message", "预警已处理");
        result.put("data", data);

        return ResponseEntity.ok(result);
    }

    @PostMapping("/scan")
    public ResponseEntity<Map<String, Object>> manualScan() {
        alertService.checkAndCreateTimeoutAlerts();
        alertService.checkAndCreateStuckAlerts();

        int alertCount = alertService.countUnresolved();

        Map<String, Object> data = new HashMap<>();
        data.put("scanned", true);
        data.put("unresolvedAlerts", alertCount);

        Map<String, Object> result = new HashMap<>();
        result.put("code", 200);
        result.put("message", "扫描完成");
        result.put("data", data);

        return ResponseEntity.ok(result);
    }

    @PostMapping("/test-timeout")
    public ResponseEntity<Map<String, Object>> createTestTimeoutAlert(
            @RequestBody Map<String, String> request) {

        String orderId = request.get("orderId");
        String nodeId = request.get("nodeId");
        String message = request.getOrDefault("message", "测试超时预警");

        Alert alert = Alert.createTimeoutAlert(orderId, nodeId, message);
        alertService.createAlert(alert);

        Map<String, Object> data = new HashMap<>();
        data.put("alertId", alert.getAlertId());
        data.put("type", alert.getType());

        Map<String, Object> result = new HashMap<>();
        result.put("code", 200);
        result.put("message", "测试预警已创建");
        result.put("data", data);

        return ResponseEntity.ok(result);
    }

    @GetMapping("/stats")
    public ResponseEntity<Map<String, Object>> getAlertStats() {
        List<Alert> allAlerts = alertService.findAll();

        long totalCount = allAlerts.size();
        long unresolvedCount = allAlerts.stream().filter(a -> !a.isResolved()).count();
        long timeoutCount = allAlerts.stream().filter(a -> a.getType() == AlertType.TIMEOUT).count();
        long stuckCount = allAlerts.stream().filter(a -> a.getType() == AlertType.STUCK).count();
        long failureCount = allAlerts.stream().filter(a -> a.getType() == AlertType.FAILURE).count();

        Map<String, Object> data = new HashMap<>();
        data.put("total", totalCount);
        data.put("unresolved", unresolvedCount);
        data.put("timeoutCount", timeoutCount);
        data.put("stuckCount", stuckCount);
        data.put("failureCount", failureCount);

        Map<String, Object> result = new HashMap<>();
        result.put("code", 200);
        result.put("message", "success");
        result.put("data", data);

        return ResponseEntity.ok(result);
    }
}
