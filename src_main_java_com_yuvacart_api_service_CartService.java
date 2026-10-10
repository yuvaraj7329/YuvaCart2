package com.yuvacart.api.service;

import com.yuvacart.api.domain.CartItem;
import com.yuvacart.api.domain.Product;
import com.yuvacart.api.domain.ProductVariant;
import com.yuvacart.api.dto.CartDtos.*;
import com.yuvacart.api.exception.BadRequestException;
import com.yuvacart.api.exception.ResourceNotFoundException;
import com.yuvacart.api.repository.CartItemRepository;
import com.yuvacart.api.repository.ProductRepository;
import com.yuvacart.api.repository.ProductVariantRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Service
public class CartService {
  private final CartItemRepository cartRepository;
  private final ProductRepository productRepository;
  private final ProductVariantRepository variantRepository;
  private final ProductService productService;

  public CartService(CartItemRepository cartRepository,
                     ProductRepository productRepository,
                     ProductVariantRepository variantRepository,
                     ProductService productService) {
    this.cartRepository = cartRepository;
    this.productRepository = productRepository;
    this.variantRepository = variantRepository;
    this.productService = productService;
  }

  @Transactional(readOnly = true)
  public CartResponse getCart(Integer userId) {
    List<CartItem> items = cartRepository.findByUserIdOrderByUpdatedAtDesc(userId);
    List<CartLineDto> lines = new ArrayList<>();
    BigDecimal subtotal = BigDecimal.ZERO;
    int totalCount = 0;

    for (CartItem item : items) {
      Product product = productRepository.findById(item.getProductId()).orElse(null);
      if (product == null || !product.getActive()) {
        continue;
      }
      ProductVariant variant = item.getVariantId() != null
          ? variantRepository.findById(item.getVariantId()).orElse(null)
          : null;

      BigDecimal price = variant != null ? variant.getPrice() : product.getPrice();
      BigDecimal lineTotal = price.multiply(BigDecimal.valueOf(item.getQuantity()));

      lines.add(new CartLineDto(
          item.getId(),
          productService.toResponse(product),
          variant,
          item.getQuantity(),
          price,
          lineTotal
      ));

      subtotal = subtotal.add(lineTotal);
      totalCount += item.getQuantity();
    }

    return new CartResponse(lines, subtotal, totalCount, "INR");
  }

  @Transactional
  public CartResponse addToCart(Integer userId, CartItemRequest request) {
    Product product = productRepository.findById(request.productId())
        .filter(Product::getActive)
        .orElseThrow(() -> new ResourceNotFoundException("Active product not found with ID: " + request.productId()));

    ProductVariant variant = null;
    if (request.variantId() != null) {
      variant = variantRepository.findById(request.variantId())
          .orElseThrow(() -> new ResourceNotFoundException("Product variant not found: " + request.variantId()));
    }

    // Server-side stock verification
    int availableStock = variant != null ? variant.getStock() : product.getStockQuantity();
    if (availableStock < request.quantity()) {
      throw new BadRequestException("Requested quantity (" + request.quantity() + ") exceeds available stock (" + availableStock + ")");
    }

    CartItem existingItem = (request.variantId() != null)
        ? cartRepository.findByUserIdAndProductIdAndVariantId(userId, request.productId(), request.variantId()).orElse(null)
        : cartRepository.findByUserIdAndProductIdAndVariantIdIsNull(userId, request.productId()).orElse(null);

    if (existingItem != null) {
      int newQty = existingItem.getQuantity() + request.quantity();
      if (newQty > 99) newQty = 99;
      existingItem.setQuantity(newQty);
      cartRepository.save(existingItem);
    } else {
      CartItem newItem = new CartItem(userId, request.productId(), request.variantId(), request.quantity());
      cartRepository.save(newItem);
    }

    return getCart(userId);
  }

  @Transactional
  public CartResponse updateCartItem(Integer userId, CartItemRequest request) {
    CartItem existingItem = (request.variantId() != null)
        ? cartRepository.findByUserIdAndProductIdAndVariantId(userId, request.productId(), request.variantId())
            .orElseThrow(() -> new ResourceNotFoundException("Item not found in cart"))
        : cartRepository.findByUserIdAndProductIdAndVariantIdIsNull(userId, request.productId())
            .orElseThrow(() -> new ResourceNotFoundException("Item not found in cart"));

    if (request.quantity() <= 0) {
      cartRepository.delete(existingItem);
    } else {
      existingItem.setQuantity(Math.min(99, request.quantity()));
      cartRepository.save(existingItem);
    }

    return getCart(userId);
  }

  @Transactional
  public CartResponse removeCartItem(Integer userId, Integer productId, Integer variantId) {
    if (variantId != null) {
      cartRepository.findByUserIdAndProductIdAndVariantId(userId, productId, variantId)
          .ifPresent(cartRepository::delete);
    } else {
      cartRepository.findByUserIdAndProductIdAndVariantIdIsNull(userId, productId)
          .ifPresent(cartRepository::delete);
    }
    return getCart(userId);
  }

  @Transactional
  public void clearCart(Integer userId) {
    cartRepository.deleteByUserId(userId);
  }
}
