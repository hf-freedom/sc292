package com.order.fulfillment.controller;

import com.order.fulfillment.service.OrderService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/stats")
@CrossOrigin(origins = "http://localhost:3005")
public class StatsController {

    @Autowired
    private OrderService orderService;

    @GetMapping
    public ResponseEntity<Map<String, Object>> getStats() {
        Map<String, Object> stats = orderService.getStats();

        return ResponseEntity.ok(Map.of(
                "code", 200,
                "message", "success",
                "data", stats
        ));
    }
}
