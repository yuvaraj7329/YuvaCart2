package com.yuvacart.api;

import com.yuvacart.api.domain.Product;
import com.yuvacart.api.domain.User;
import com.yuvacart.api.dto.CartDtos.CartItemRequest;
import com.yuvacart.api.dto.CartDtos.CartResponse;
import com.yuvacart.api.dto.OrderDtos.CheckoutRequest;
import com.yuvacart.api.dto.OrderDtos.OrderResponse;
import com.yuvacart.api.dto.WishlistDtos.WishlistResponse;
import com.yuvacart.api.repository.CartItemRepository;
import com.yuvacart.api.repository.OrderRepository;
import com.yuvacart.api.repository.ProductRepository;
import com.yuvacart.api.repository.UserRepository;
import com.yuvacart.api.service.CartService;
import com.yuvacart.api.service.OrderService;
import com.yuvacart.api.service.WishlistService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
public class CartAndOrderTests {

  @Autowired
  private UserRepository userRepository;

  @Autowired
  private ProductRepository productRepository;

  @Autowired
  private CartItemRepository cartItemRepository;

  @Autowired
  private OrderRepository orderRepository;

  @Autowired
  private CartService cartService;

  @Autowired
  private WishlistService wishlistService;

  @Autowired
  private OrderService orderService;

  private User testUser;
  private Product testProduct;

  @BeforeEach
  void setUp() {
    cartItemRepository.deleteAll();
    orderRepository.deleteAll();
    productRepository.deleteAll();
    userRepository.deleteAll();

    testUser = new User("CartTester", "+919876500000");
    testUser = userRepository.save(testUser);

    testProduct = new Product();
    testProduct.setName("AeroSound Wireless Headphones");
    testProduct.setSlug("aerosound-test");
    testProduct.setDescription("High fidelity wireless sound");
    testProduct.setPrice(new BigDecimal("2499.00"));
    testProduct.setStockQuantity(20);
    testProduct.setImageUrl("https://images.unsplash.com/photo-1505740420928");
    testProduct.setCategory("Audio");
    testProduct.setActive(true);
    testProduct = productRepository.save(testProduct);
  }

  @Test
  @DisplayName("Cart and Wishlist lifecycle: add, update, calculate subtotal, and toggle wishlist")
  void testCartAndWishlistLifecycle() {
    // 1. Add to cart
    CartResponse cart = cartService.addToCart(testUser.getId(), new CartItemRequest(testProduct.getId(), null, 2));
    assertNotNull(cart);
    assertEquals(1, cart.items().size());
    assertEquals(2, cart.count());
    assertEquals(new BigDecimal("4998.00"), cart.subtotal());

    // 2. Toggle wishlist
    WishlistResponse wishlist = wishlistService.toggleWishlist(testUser.getId(), testProduct.getId());
    assertEquals(1, wishlist.count());

    // 3. Toggle again removes from wishlist
    WishlistResponse emptyWishlist = wishlistService.toggleWishlist(testUser.getId(), testProduct.getId());
    assertEquals(0, emptyWishlist.count());
  }

  @Test
  @DisplayName("Order creation: verifies prices from DB, decrements stock, and clears cart")
  void testCheckoutAndOrderCreation() {
    // Add 3 items to cart
    cartService.addToCart(testUser.getId(), new CartItemRequest(testProduct.getId(), null, 3));

    CheckoutRequest checkoutReq = new CheckoutRequest(
        "CartTester",
        "+919876500000",
        "123 MG Road, Indiranagar",
        "Bengaluru",
        "560038",
        "COD"
    );

    // Place order
    OrderResponse order = orderService.createOrderFromCart(testUser.getId(), checkoutReq);
    assertNotNull(order);
    assertTrue(order.orderNumber().startsWith("YC-"));
    assertEquals(new BigDecimal("7497.00"), order.totalAmount());
    assertEquals("CONFIRMED", order.status());
    assertEquals(1, order.items().size());
    assertEquals(3, order.items().get(0).quantity());

    // Verify stock decreased from 20 to 17 in database
    Product updatedProduct = productRepository.findById(testProduct.getId()).orElseThrow();
    assertEquals(17, updatedProduct.getStockQuantity());

    // Verify cart is now empty
    CartResponse cartAfterOrder = cartService.getCart(testUser.getId());
    assertEquals(0, cartAfterOrder.count());
  }
}
