package com.yuvacart.api.repository;

import com.yuvacart.api.domain.ProductVariant;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface ProductVariantRepository extends JpaRepository<ProductVariant, Integer> {
  List<ProductVariant> findByProductIdOrderByPriceAsc(Integer productId);
}
