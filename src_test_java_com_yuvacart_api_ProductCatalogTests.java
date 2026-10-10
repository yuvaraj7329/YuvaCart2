package com.yuvacart.api;

import com.yuvacart.api.domain.Product;
import com.yuvacart.api.dto.ProductDtos.ProductFilter;
import com.yuvacart.api.dto.ProductDtos.ProductResponse;
import com.yuvacart.api.repository.ProductRepository;
import com.yuvacart.api.service.ProductService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
public class ProductCatalogTests {

  @Autowired
  private ProductRepository productRepository;

  @Autowired
  private ProductService productService;

  @BeforeEach
  void setUp() {
    productRepository.deleteAll();

    // Seed test products
    Product p1 = new Product();
    p1.setName("AeroSound Wireless Headphones");
    p1.setSlug("aerosound-wireless-headphones");
    p1.setDescription("Crisp sound and deep bass.");
    p1.setPrice(new BigDecimal("2499.00"));
    p1.setStockQuantity(10);
    p1.setImageUrl("https://images.unsplash.com/photo-1505740420928");
    p1.setCategory("Audio");
    p1.setActive(true);
    productRepository.save(p1);

    Product p2 = new Product();
    p2.setName("VoltGo 20,000 mAh Power Bank");
    p2.setSlug("voltgo-20000-mah-power-bank");
    p2.setDescription("High capacity fast charging portable battery.");
    p2.setPrice(new BigDecimal("1999.00"));
    p2.setStockQuantity(5);
    p2.setImageUrl("https://images.unsplash.com/photo-voltgo");
    p2.setCategory("Tech");
    p2.setActive(false); // Deactivated
    productRepository.save(p2);

    Product p3 = new Product();
    p3.setName("Botanical Care Starter Set");
    p3.setSlug("botanical-care-starter-set");
    p3.setDescription("Gentle soothing skincare ritual.");
    p3.setPrice(new BigDecimal("1299.00"));
    p3.setStockQuantity(8);
    p3.setImageUrl("https://images.unsplash.com/photo-botanical");
    p3.setCategory("Beauty");
    p3.setActive(false); // Deactivated
    productRepository.save(p3);

    Product p4 = new Product();
    p4.setName("Luma Ceramic Table Lamp");
    p4.setSlug("luma-ceramic-table-lamp");
    p4.setDescription("Warm ambient glow.");
    p4.setPrice(new BigDecimal("1890.00"));
    p4.setStockQuantity(12);
    p4.setImageUrl("https://images.unsplash.com/photo-luma");
    p4.setCategory("Home");
    p4.setActive(true);
    productRepository.save(p4);
  }

  @Test
  @DisplayName("Verify that VoltGo and Botanical Care are excluded from customer-facing listings")
  void testExcludedProductsAreNotReturned() {
    List<ProductResponse> products = productService.listProducts(null);

    assertNotNull(products);
    assertFalse(products.isEmpty());

    for (ProductResponse product : products) {
      assertNotEquals("VoltGo 20,000 mAh Power Bank", product.name(), "VoltGo Power Bank must not be in customer listings");
      assertNotEquals("Botanical Care Starter Set", product.name(), "Botanical Care Starter Set must not be in customer listings");
    }

    assertTrue(products.stream().anyMatch(p -> p.name().equals("AeroSound Wireless Headphones")));
    assertTrue(products.stream().anyMatch(p -> p.name().equals("Luma Ceramic Table Lamp")));
  }

  @Test
  @DisplayName("Verify category filtering works properly")
  void testCategoryFiltering() {
    List<ProductResponse> audioProducts = productService.listProducts(new ProductFilter(null, "Audio", null, null));
    assertEquals(1, audioProducts.size());
    assertEquals("AeroSound Wireless Headphones", audioProducts.get(0).name());
  }

  @Test
  @DisplayName("Verify text search matches product name and description")
  void testQuerySearch() {
    List<ProductResponse> searchResults = productService.listProducts(new ProductFilter("ambient", null, null, null));
    assertEquals(1, searchResults.size());
    assertEquals("Luma Ceramic Table Lamp", searchResults.get(0).name());
  }
}
