package com.yuvacart.api.repository;

import com.yuvacart.api.domain.WishlistItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface WishlistRepository extends JpaRepository<WishlistItem, Integer> {
  List<WishlistItem> findByUserIdOrderByCreatedAtDesc(Integer userId);
  Optional<WishlistItem> findByUserIdAndProductId(Integer userId, Integer productId);
  void deleteByUserIdAndProductId(Integer userId, Integer productId);
}
