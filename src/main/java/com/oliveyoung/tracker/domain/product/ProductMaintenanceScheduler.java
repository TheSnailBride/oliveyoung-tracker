package com.oliveyoung.tracker.domain.product;

import com.oliveyoung.tracker.domain.product.service.ProductMaintenanceService;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class ProductMaintenanceScheduler {

    private final ProductMaintenanceService productMaintenanceService;

    @Scheduled(cron = "${product.maintenance.cron:0 30 3 * * *}", zone = "${product.maintenance.zone:Asia/Seoul}")
    public void runDailyMaintenance() {
        productMaintenanceService.deleteExpiredProductAlerts();
        productMaintenanceService.markStaleProductsAsSoldOut();
    }
}
