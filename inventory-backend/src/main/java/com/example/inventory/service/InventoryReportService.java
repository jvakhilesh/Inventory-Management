package com.example.inventory.service;

import com.example.inventory.entity.Product;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class InventoryReportService {
    private final ProductService productService;

    public byte[] createCsv() {
        List<Product> products = productService.getAll();
        StringBuilder csv = new StringBuilder("ProductId,ProductName,ProductQuantity,ProductPrice,ProductCategory,Threshold\r\n");
        for (Product product : products) {
            csv.append(product.getProductId()).append(',')
                    .append(escape(product.getProductName())).append(',')
                    .append(product.getQuantity()).append(',')
                    .append(product.getPrice()).append(',')
                    .append(escape(product.getCategory())).append(',')
                    .append(product.getThreshold()).append("\r\n");
        }
        return csv.toString().getBytes(StandardCharsets.UTF_8);
    }

    public String createFileName() {
        return "inventory_report_" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss")) + ".csv";
    }

    private String escape(String value) {
        if (value == null) return "";
        String escaped = value.replace("\"", "\"\"");
        return escaped.contains(",") || escaped.contains("\n") || escaped.contains("\r") || escaped.contains("\"")
                ? "\"" + escaped + "\"" : escaped;
    }
}
