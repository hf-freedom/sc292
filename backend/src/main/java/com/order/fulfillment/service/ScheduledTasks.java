package com.order.fulfillment.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class ScheduledTasks {

    @Autowired
    private AlertService alertService;

    @Autowired
    private NodeService nodeService;

    @Scheduled(fixedRate = 60000)
    public void scanNodes() {
        alertService.checkAndCreateTimeoutAlerts();
        alertService.checkAndCreateStuckAlerts();
        nodeService.checkAndAdvanceNodes();
    }
}
