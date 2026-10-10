package com.yuvacart.api.repository;

import com.yuvacart.api.domain.Product;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface ProductRepository extends JpaRepository<Product, Integer>, JpaSpecificationExecutor<Product> {
  Optional<Product> findBySlug(String slug);
  Optional<Product> findBySlugAndActiveTrue(String slug);
  List<Product> findByActiveTrue();
  List<Product> findByActiveTrueAndCategoryIgnoreCase(String category);
}
