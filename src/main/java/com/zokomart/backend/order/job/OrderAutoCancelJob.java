package com.zokomart.backend.order.job;

import com.zokomart.backend.order.service.OrderAutoCancelService;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class OrderAutoCancelJob {

    private final OrderAutoCancelService orderAutoCancelService;

    public OrderAutoCancelJob(OrderAutoCancelService orderAutoCancelService) {
        this.orderAutoCancelService = orderAutoCancelService;
    }

    @Scheduled(
            initialDelayString = "${zokomart.order.auto-cancel-scan-interval}",
            fixedDelayString = "${zokomart.order.auto-cancel-scan-interval}"
    )
    public void scanAndAutoCancelExpiredOrders() {
        orderAutoCancelService.autoCancelExpiredOrders();
    }
}
