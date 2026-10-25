package com.example.inventory.service;

import com.example.inventory.dto.ProductRequest;
import com.example.inventory.entity.Product;
import com.example.inventory.exception.InvalidPriceRangeException;
import com.example.inventory.exception.ProductNotFoundException;
import com.example.inventory.repository.ProductRepository;

import lombok.RequiredArgsConstructor;
import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

@Service
@Transactional
@RequiredArgsConstructor
public class ProductService {

    private final ProductRepository productRepository;
    private final ModelMapper modelMapper;

    public Product create(ProductRequest request) {
        Product product = modelMapper.map(request, Product.class);
        return productRepository.save(product);
    }

    @Transactional(readOnly = true)
    public Product getById(Long id) {
        return productRepository.findById(id)
                .orElseThrow(() -> new ProductNotFoundException(id));
    }

    @Transactional(readOnly = true)
    public List<Product> getAll() {
        return productRepository.findAll();
    }

    public Product update(Long id, ProductRequest request) {
        Product product = getById(id);
        modelMapper.map(request, product);
        return productRepository.save(product);
    }

    public void delete(Long id) {
        productRepository.delete(getById(id));
    }

    @Transactional(readOnly = true)
    public List<Product> filterByPrice(BigDecimal minPrice, BigDecimal maxPrice) {
        if (minPrice == null || maxPrice == null
                || minPrice.signum() < 0
                || maxPrice.signum() < 0
                || minPrice.compareTo(maxPrice) > 0) {
            throw new InvalidPriceRangeException();
        }

        return productRepository.findByPriceBetweenOrderByPriceAsc(minPrice, maxPrice);
    }

    @Transactional(readOnly = true)
    public List<Product> getLowStockProducts() {
        return productRepository.findAll().stream()
                .filter(product -> product.getQuantity() != null
                        && product.getThreshold() != null
                        && product.getQuantity() < product.getThreshold())
                .toList();
    }
}