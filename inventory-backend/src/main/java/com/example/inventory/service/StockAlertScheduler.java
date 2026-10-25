package com.example.inventory.service;

import com.example.inventory.entity.Product;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class StockAlertScheduler {
    private static final Logger log = LoggerFactory.getLogger(StockAlertScheduler.class);
    private final ProductService productService;
    private final EmailService emailService;
    private final AuthClientService authClientService;

    @Value("${inventory.stock-alert.recipient:}")
    private String recipient;

    @Value("${inventory.stock-alert.enabled:true}")
    private boolean enabled;


    @Scheduled(fixedRateString = "${inventory.stock-alert.fixed-rate-ms:300000}", initialDelayString = "${inventory.stock-alert.initial-delay-ms:30000}")
    public void checkLowStock() {
        if (!enabled) return;

        Set<String> recipients = new LinkedHashSet<>();
        if (recipient != null && !recipient.isBlank()) {
            recipients.add(recipient.trim());
        }

        try {
            List<String> dynamicRecipients = authClientService.getStockAlertRecipients();
            if (dynamicRecipients != null) {
                for (String email : dynamicRecipients) {
                    if (email != null && !email.isBlank()) {
                        recipients.add(email.trim());
                    }
                }
            }
        } catch (Exception ex) {
            log.error("Could not retrieve stock alert recipients from auth-service: {}", ex.getMessage());
        }

        if (recipients.isEmpty()) {
            log.debug("Stock alert skipped: no alert recipients found");
            return;
        }

        List<Product> lowStockProducts = productService.getLowStockProducts();
        if (lowStockProducts.isEmpty()) {
            log.debug("No low-stock products found");
            return;
        }

        try {
            emailService.sendLowStockAlert(new ArrayList<>(recipients), lowStockProducts);
            log.info("Low-stock alert sent for {} product(s) to {} recipient(s)", lowStockProducts.size(), recipients.size());
        } catch (RuntimeException ex) {
            log.error("Could not send low-stock alert: {}", ex.getMessage());
        }
    }
}
