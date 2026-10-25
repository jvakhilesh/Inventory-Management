package com.example.inventory.controller;

import com.example.inventory.advice.ApiResponse;
import com.example.inventory.dto.ProductRequest;
import com.example.inventory.entity.Product;
import com.example.inventory.service.ProductService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;

@RestController
@RequestMapping("/api/products")
@RequiredArgsConstructor
public class ProductController {

    private final ProductService productService;

    @PostMapping
    public ResponseEntity<ApiResponse<Product>> create(
            @Valid @RequestBody ProductRequest request) {

        Product product = productService.create(request);

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(new ApiResponse<>(product));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<Product>> getById(
            @PathVariable Long id) {

        return ResponseEntity.ok(
                new ApiResponse<>(productService.getById(id))
        );
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<Product>>> getAll() {

        return ResponseEntity.ok(
                new ApiResponse<>(productService.getAll())
        );
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<Product>> update(
            @PathVariable Long id,
            @Valid @RequestBody ProductRequest request) {

        return ResponseEntity.ok(
                new ApiResponse<>(productService.update(id, request))
        );
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {

        productService.delete(id);

        return ResponseEntity.noContent().build();
    }

    @GetMapping("/filter/price")
    public ResponseEntity<ApiResponse<List<Product>>> filterByPrice(
            @RequestParam BigDecimal minPrice,
            @RequestParam BigDecimal maxPrice) {

        return ResponseEntity.ok(
                new ApiResponse<>(
                        productService.filterByPrice(minPrice, maxPrice)
                )
        );
    }

    @GetMapping("/low-stock")
    public ResponseEntity<ApiResponse<List<Product>>> getLowStockProducts() {

        return ResponseEntity.ok(
                new ApiResponse<>(productService.getLowStockProducts())
        );
    }
}