package com.yuvacart.api.service;

import com.yuvacart.api.domain.Product;
import com.yuvacart.api.domain.WishlistItem;
import com.yuvacart.api.dto.ProductDtos.ProductResponse;
import com.yuvacart.api.dto.WishlistDtos.WishlistResponse;
import com.yuvacart.api.exception.ResourceNotFoundException;
import com.yuvacart.api.repository.ProductRepository;
import com.yuvacart.api.repository.WishlistRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
public class WishlistService {
  private final WishlistRepository wishlistRepository;
  private final ProductRepository productRepository;
  private final ProductService productService;

  public WishlistService(WishlistRepository wishlistRepository,
                         ProductRepository productRepository,
                         ProductService productService) {
    this.wishlistRepository = wishlistRepository;
    this.productRepository = productRepository;
    this.productService = productService;
  }

  @Transactional(readOnly = true)
  public WishlistResponse getWishlist(Integer userId) {
    List<WishlistItem> items = wishlistRepository.findByUserIdOrderByCreatedAtDesc(userId);
    List<ProductResponse> products = new ArrayList<>();

    for (WishlistItem item : items) {
      productRepository.findById(item.getProductId())
          .filter(Product::getActive)
          .ifPresent(p -> products.add(productService.toResponse(p)));
    }

    return new WishlistResponse(products, products.size());
  }

  @Transactional
  public WishlistResponse toggleWishlist(Integer userId, Integer productId) {
    productRepository.findById(productId)
        .filter(Product::getActive)
        .orElseThrow(() -> new ResourceNotFoundException("Active product not found: " + productId));

    Optional<WishlistItem> existing = wishlistRepository.findByUserIdAndProductId(userId, productId);
    if (existing.isPresent()) {
      wishlistRepository.delete(existing.get());
    } else {
      wishlistRepository.save(new WishlistItem(userId, productId));
    }

    return getWishlist(userId);
  }

  @Transactional
  public WishlistResponse removeFromWishlist(Integer userId, Integer productId) {
    wishlistRepository.deleteByUserIdAndProductId(userId, productId);
    return getWishlist(userId);
  }
}
