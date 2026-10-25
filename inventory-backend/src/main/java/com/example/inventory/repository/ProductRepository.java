package com.example.inventory.repository;

import com.example.inventory.entity.Product;
import java.math.BigDecimal;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProductRepository extends JpaRepository<Product, Long> {
    List<Product> findByPriceBetweenOrderByPriceAsc(BigDecimal minPrice, BigDecimal maxPrice);
    List<Product> findByQuantityLessThan(Integer quantity);
    List<Product> findByQuantityLessThanEqual(Integer quantity);
}
