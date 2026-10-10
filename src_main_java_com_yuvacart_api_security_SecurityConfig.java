package com.yuvacart.api.security;

import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Arrays;
import java.util.List;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

  @Value("${app.cors.allowed-origins:http://localhost:3000,http://localhost:5173,http://localhost:8080,https://yuvacart-pbgxuorf.manus.space}")
  private String allowedOrigins;

  @Bean
  public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
    http
        .cors(cors -> cors.configurationSource(corsConfigurationSource()))
        .csrf(csrf -> csrf.disable()) // Disabled for REST APIs with SameSite cookie / header protection
        .sessionManagement(session -> session
            .sessionCreationPolicy(SessionCreationPolicy.IF_REQUIRED)
        )
        .exceptionHandling(ex -> ex
            .authenticationEntryPoint((request, response, authException) -> {
              response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
              response.setContentType("application/json");
              response.getWriter().write("{\"success\":false,\"error\":\"Authentication required. Please sign in with phone OTP.\",\"status\":401}");
            })
        )
        .authorizeHttpRequests(auth -> auth
            // Public static assets & root
            .requestMatchers("/", "/index.html", "/assets/**", "/static/**", "/*.css", "/*.js", "/*.ico", "/*.png", "/*.svg", "/*.json", "/error").permitAll()
            // Public Authentication endpoints
            .requestMatchers("/api/auth/**").permitAll()
            // Public Catalog and discovery
            .requestMatchers(HttpMethod.GET, "/api/products/**", "/api/v1/products/**", "/api/categories/**", "/api/v1/categories/**").permitAll()
            // Public health checks
            .requestMatchers("/api/health", "/health", "/actuator/**").permitAll()
            // Protected Customer Actions
            .requestMatchers("/api/cart/**", "/api/v1/cart/**").authenticated()
            .requestMatchers("/api/wishlist/**").authenticated()
            .requestMatchers("/api/orders/**").authenticated()
            .requestMatchers("/api/checkout/**").authenticated()
            // All other requests require authentication
            .anyRequest().authenticated()
        );

    return http.build();
  }

  @Bean
  public CorsConfigurationSource corsConfigurationSource() {
    CorsConfiguration config = new CorsConfiguration();
    List<String> origins = Arrays.stream(allowedOrigins.split(","))
        .map(String::trim)
        .filter(s -> !s.isEmpty())
        .toList();

    if (origins.contains("*")) {
      config.addAllowedOriginPattern("*");
    } else {
      config.setAllowedOrigins(origins);
    }

    config.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
    config.setAllowedHeaders(List.of("Authorization", "Content-Type", "X-Requested-With", "Accept", "Origin", "X-User-Id"));
    config.setExposedHeaders(List.of("Set-Cookie"));
    config.setAllowCredentials(true);
    config.setMaxAge(3600L);

    UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
    source.registerCorsConfiguration("/**", config);
    return source;
  }
}
