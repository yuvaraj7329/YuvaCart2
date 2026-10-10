package com.yuvacart.api.repository;

import com.yuvacart.api.domain.CartItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface CartItemRepository extends JpaRepository<CartItem, Integer> {
  List<CartItem> findByUserIdOrderByUpdatedAtDesc(Integer userId);
  Optional<CartItem> findByUserIdAndProductIdAndVariantId(Integer userId, Integer productId, Integer variantId);
  Optional<CartItem> findByUserIdAndProductIdAndVariantIdIsNull(Integer userId, Integer productId);
  void deleteByUserId(Integer userId);
}
