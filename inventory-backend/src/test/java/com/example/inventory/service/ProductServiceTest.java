package com.example.inventory.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import com.example.inventory.dto.ProductRequest;
import com.example.inventory.entity.Product;
import com.example.inventory.exception.ProductNotFoundException;
import com.example.inventory.repository.ProductRepository;
import java.math.BigDecimal;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ProductServiceTest {
    @Mock private ProductRepository productRepository;
    @InjectMocks private ProductService productService;

    @Test
    void createCopiesValidatedRequestAndSavesProduct() {
        ProductRequest request = new ProductRequest();
        request.setProductName(" Keyboard ");
        request.setQuantity(20);
        request.setPrice(new BigDecimal("799.00"));
        request.setCategory(" Electronics ");
        request.setThreshold(5);
        when(productRepository.save(any(Product.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Product result = productService.create(request);

        assertEquals("Keyboard", result.getProductName());
        assertEquals("Electronics", result.getCategory());
        assertEquals(20, result.getQuantity());
        assertEquals(new BigDecimal("799.00"), result.getPrice());
        verify(productRepository).save(any(Product.class));
    }

    @Test
    void getByIdThrowsWhenProductDoesNotExist() {
        when(productRepository.findById(404L)).thenReturn(Optional.empty());
        assertThrows(ProductNotFoundException.class, () -> productService.getById(404L));
    }
}
