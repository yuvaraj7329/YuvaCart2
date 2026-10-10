# YuvaCart — Modern E-Commerce Platform

YuvaCart is a full-featured e-commerce web platform inspired by Amazon and Flipkart, updated with a **Java 21 / Spring Boot 3.4.x / Spring Security / Spring Data JPA / MySQL 8+** backend and a **real phone-number OTP authentication system**.

- **GitHub Repository**: [https://github.com/yuvaraj7329/YuvaCart](https://github.com/yuvaraj7329/YuvaCart)
- **Live Preview Site**: [https://yuvacart-pbgxuorf.manus.space/](https://yuvacart-pbgxuorf.manus.space/)

---

## 🌟 Key Updates & Specifications Implemented

### 1. Specific Products Removed
As specified in the prompt brief:
- **`VoltGo 20,000 mAh Power Bank`** and **`Botanical Care Starter Set`** have been completely removed from customer-facing listings across:
  - Homepage featured listings
  - Product catalog grids
  - Category filters
  - Search queries
  - Seed product fixtures (`data.sql`, `seed-products.ts`)
- The database schema supports an `active` status column, and any deactivated products are safely excluded from customer queries while preserving historical cart/order data integrity.

### 2. Pure Phone-Number OTP Authentication (No Google / Email)
- Completely eliminated Google OAuth and email login interfaces from the customer authentication modal.
- **Initial Interface**: Displays the purple YuvaCart logo, phone number input with country code selector (default `+91 (India)`), a "Send 6-Digit OTP" button, and a toggle between "Sign In" and "New Account".
- **Real OTP Generation & Delivery**: Generates a cryptographically secure 6-digit random code, calculates a SHA-256 hash, and dispatches it via SMS gateway (Twilio or Indian SMS gateway like Fast2SMS / MSG91).
- **Security Rules**:
  - 5-minute expiry countdown timer.
  - 60-second resend cooldown timer.
  - Maximum 5 verification attempts per OTP challenge to thwart brute-force attacks.
  - Rate limiting (max 10 requests per hour per phone).
  - Challenges are invalidated once used or upon requesting a new code.
  - Passwords and OTP plaintext are **never** logged or returned in API responses.
- **Login Flow**:
  - If phone number is not registered in MySQL, shows a clear message with a 1-click option to switch to registration.
  - Upon server-side OTP verification, retrieves username from MySQL, displays a welcome toast (`"Welcome back, {username}!"`), and restores the user's session with an HttpOnly `JSESSIONID` cookie.
- **New User Registration Flow**:
  - Allows entering username and phone number.
  - Enforces phone number uniqueness in MySQL.
  - Verifies ownership of the phone number via OTP before saving the new user account into MySQL.

### 3. MySQL 8+ Database Integration
Built with **Java 21**, **Spring Boot 3.4.5**, **Spring Data JPA**, **Hibernate**, **MySQL 8+**, and **Maven**:
- Connected to database `yuvacart`.
- Normalized `users` table (`id`, `username`, `phone_number`, `role`, `created_at`, `updated_at`) with unique index on `phone_number`.
- Complete `products` table (`id`, `name`, `slug`, `description`, `price`, `discount`, `compare_at_price`, `stock_quantity`, `image_url`, `category_id`, `category`, `currency`, `seller_name`, `rating`, `review_count`, `is_featured`, `specifications`, `active`).
- Complete relational tables: `categories`, `product_variants`, `cart_items`, `wishlist`, `orders`, `order_items`, and `otp_verifications`.
- Safe schema migration script: `yuvacart-schema.sql` and `src/main/resources/schema.sql`.

### 4. Security & Architecture
- **Spring Security** configuration with session management and authenticated private endpoints (`/api/cart/**`, `/api/wishlist/**`, `/api/orders/**`).
- User isolation: prevents users from accessing another user's cart or orders by taking user identity exclusively from the authenticated session.
- Server-side price and stock validation: client totals are never trusted; order lines and subtotal amounts are calculated directly from verified database records.
- CORS configuration support for local development and production origins.

### 5. Preserved Front-End Shopping Experience
- Preserves the purple/cream visual design (`#6d28d9`), Plus Jakarta Sans typography, responsive layout, search bar, category chips, wishlist toggle, cart drawer, checkout modal, and order history.
- Served directly by Spring Boot from `src/main/resources/static/` for zero-configuration, single-port deployment.

---

## 📁 Project Structure

```
YuvaCart/
├── pom.xml                               # Maven Project Descriptor (Java 21, Spring Boot 3.4.5)
├── yuvacart-schema.sql                   # MySQL 8+ DDL Schema & Initialization Script
├── .env.example                          # Environment Variables Configuration Template
├── .gitignore                            # Standard Git Ignore Rules
├── README.md                             # Comprehensive Project Documentation
│
├── src/
│   ├── main/
│   │   ├── java/com/yuvacart/api/
│   │   │   ├── YuvaCartApplication.java   # Spring Boot Application Entry Point
│   │   │   ├── config/                   # WebMvc & Static Resource Routing Config
│   │   │   ├── controller/               # REST Controllers (Auth, Product, Cart, Wishlist, Order, Health)
│   │   │   ├── domain/                   # JPA Entity Classes (User, Product, CartItem, Order, etc.)
│   │   │   ├── dto/                      # Strongly Typed Request & Response Records
│   │   │   ├── exception/                # Global Exception Handler & Custom Errors
│   │   │   ├── repository/               # Spring Data JPA Repository Interfaces
│   │   │   ├── security/                 # Spring Security Configuration & CORS
│   │   │   └── service/                  # Business Logic Services (Auth, OTP, SMS, Products, Cart, Orders)
│   │   │
│   │   └── resources/
│   │       ├── application.properties    # Environment-backed MySQL & SMS Properties
│   │       ├── schema.sql                # Safe DDL Table Migrations
│   │       ├── data.sql                  # Seed Catalog Data (31 active products)
│   │       └── static/                   # Production Single-Page Storefront Application
│   │           ├── index.html            # Responsive Storefront UI with Phone OTP Modal
│   │           ├── app.js                # Frontend REST API Integration & State Logic
│   │           └── style.css             # YuvaCart Theme & Animations
│   │
│   └── test/
│       ├── java/com/yuvacart/api/
│       │   ├── YuvaCartApplicationTests.java # Context Load Verification
│       │   ├── ProductCatalogTests.java      # Removal of VoltGo and Botanical Care
│       │   ├── AuthAndOtpTests.java          # Phone OTP Lifecycle & Security
│       │   └── CartAndOrderTests.java        # Cart Calculation & Checkout Validation
│       └── resources/
│           └── application.properties        # In-Memory H2 MySQL Mode for Automated Tests
│
└── client/                               # Frontend Source & TypeScript Components
    ├── package.json
    └── src/
        └── useAuth.ts                    # Reusable React Hook for Phone OTP Auth
```

---

## 🚀 Setup & Run Instructions

### Prerequisites
- **Java 21** or higher (`java -version`)
- **Apache Maven 3.9+** (`mvn -version`)
- **MySQL 8.0+** (or use Docker / Local Service)

### 1. Configure MySQL Database
Create the MySQL database:
```sql
CREATE DATABASE yuvacart CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
```
Import schema:
```bash
mysql -u root -p yuvacart < yuvacart-schema.sql
```

### 2. Configure Environment Variables
Copy `.env.example` to `.env` or set environment variables in your terminal:
```bash
# Windows PowerShell:
$env:SPRING_DATASOURCE_URL="jdbc:mysql://localhost:3306/yuvacart?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC"
$env:SPRING_DATASOURCE_USERNAME="root"
$env:SPRING_DATASOURCE_PASSWORD="your_password"

# Optional SMS Gateway (Twilio):
$env:SMS_PROVIDER="twilio"
$env:TWILIO_ACCOUNT_SID="ACXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXX"
$env:TWILIO_AUTH_TOKEN="your_auth_token"
$env:TWILIO_FROM_NUMBER="+1234567890"

# Set to true for local testing without spending SMS credits:
$env:SMS_DEV_MODE="true"
```

### 3. Run Automated Tests
Execute the comprehensive test suite (all 11 integration tests):
```bash
mvn clean test
```

### 4. Start the Application
Run via Spring Boot:
```bash
mvn spring-boot:run
```
Or build a production executable JAR:
```bash
mvn clean package -DskipTests
java -jar target/yuvacart-api-1.0.0.jar
```

Once started, open your browser at:
👉 **[http://localhost:8080](http://localhost:8080)**

---

## 📲 SMS Gateway Configuration

### Option A: Twilio (Global SMS Delivery)
1. Sign up at [https://www.twilio.com/](https://www.twilio.com/).
2. Obtain your **Account SID**, **Auth Token**, and a phone number or Messaging Service SID.
3. Configure:
   ```properties
   SMS_PROVIDER=twilio
   TWILIO_ACCOUNT_SID=ACXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXX
   TWILIO_AUTH_TOKEN=your_auth_token_here
   TWILIO_FROM_NUMBER=+12015550123
   SMS_DEV_MODE=false
   ```

### Option B: Indian SMS Gateway (Fast2SMS / MSG91)
1. Sign up at [https://www.fast2sms.com/](https://www.fast2sms.com/) or [https://msg91.com/](https://msg91.com/).
2. Generate an API Key and Sender ID.
3. Configure:
   ```properties
   SMS_PROVIDER=fast2sms
   SMS_GATEWAY_API_KEY=your_fast2sms_api_key_here
   SMS_GATEWAY_SENDER_ID=YUVACART
   SMS_DEV_MODE=false
   ```

### Option C: Development & Automated Testing Mode (`SMS_DEV_MODE=true`)
When `SMS_DEV_MODE=true`, OTPs are safely logged to the server terminal (`[DEV OTP NOTICE] Generated OTP for +91••••••3210: 123456`) without incurring carrier SMS costs, while completely satisfying the OTP workflow. In production with `SMS_DEV_MODE=false`, credentials are strictly enforced.

---

## 📡 REST API Reference & Testing Examples

### 1. Request Login OTP
```bash
curl -X POST http://localhost:8080/api/auth/send-otp \
  -H "Content-Type: application/json" \
  -d '{"phoneNumber": "+919876543210", "purpose": "LOGIN"}'
```
**Response (200 OK):**
```json
{
  "success": true,
  "registered": true,
  "cooldownSeconds": 60,
  "expiresInSeconds": 300,
  "message": "A 6-digit verification code was sent to +91••••••3210"
}
```

### 2. Verify OTP & Authenticate Session
```bash
curl -X POST http://localhost:8080/api/auth/verify-otp \
  -H "Content-Type: application/json" \
  -c cookies.txt \
  -d '{"phoneNumber": "+919876543210", "otp": "123456"}'
```
**Response (200 OK):**
```json
{
  "success": true,
  "user": {
    "id": 1,
    "username": "Yuvaraj",
    "phoneNumber": "+919876543210",
    "role": "USER"
  },
  "message": "Welcome back, Yuvaraj!"
}
```

### 3. Register New User (After Verifying OTP)
```bash
curl -X POST http://localhost:8080/api/auth/register \
  -H "Content-Type: application/json" \
  -c cookies.txt \
  -d '{"username": "Yuvaraj", "phoneNumber": "+919876543210", "otp": "123456"}'
```

### 4. Fetch Current Authenticated User Session
```bash
curl -X GET http://localhost:8080/api/auth/me \
  -b cookies.txt
```

### 5. Browse Products (VoltGo and Botanical Care Excluded)
```bash
curl -X GET "http://localhost:8080/api/products?category=Audio"
```

### 6. Add Item to Cart
```bash
curl -X POST http://localhost:8080/api/cart/items \
  -H "Content-Type: application/json" \
  -b cookies.txt \
  -d '{"productId": 1, "quantity": 2}'
```

### 7. Place Order
```bash
curl -X POST http://localhost:8080/api/orders \
  -H "Content-Type: application/json" \
  -b cookies.txt \
  -d '{
    "shippingName": "Yuvaraj",
    "shippingPhone": "+919876543210",
    "shippingAddress": "123 MG Road, Indiranagar",
    "shippingCity": "Bengaluru",
    "shippingPostalCode": "560038",
    "paymentMethod": "COD"
  }'
```

### 8. Logout
```bash
curl -X POST http://localhost:8080/api/auth/logout \
  -b cookies.txt
```

---

## ✅ Completed Deliverables Checklist

- [x] **Product Removal**: `VoltGo 20,000 mAh Power Bank` and `Botanical Care Starter Set` removed from all catalog queries, homepage, search, and seed data.
- [x] **Authentication Replacement**: Google sign-in and email login removed; replaced with pure Phone Number OTP and registration.
- [x] **Real SMS OTP System**: Cryptographic 6-digit generation, Twilio / Indian SMS Gateway support, 5-minute expiry, 60s resend cooldown, max 5 attempts tracking, SHA-256 secure hash storage.
- [x] **MySQL Database Integration**: Complete MySQL 8+ schema (`yuvacart-schema.sql` and `schema.sql`) with safe migrations and preserved tables.
- [x] **Spring Boot 3.4.5 & Java 21**: Maven project structure with Spring Security, Spring Data JPA, Hibernate, and Actuator.
- [x] **Shopping Features Preserved**: Responsive UI, product search, category chips, cart drawer, wishlist drawer, checkout, and order history.
- [x] **Configuration Files**: `pom.xml`, `application.properties`, `.env.example`, `.gitignore`, `yuvacart-schema.sql`, `README.md`.
- [x] **Automated Testing Suite**: 11 unit and integration tests passing (`mvn test`).

---

## 📝 Remaining Limitations & Production Notes

1. **Carrier Sender ID Approval**: Indian telecom operators (DLT regulation) require approved Entity IDs and Template IDs for production SMS delivery. When deploying in India, register your header on TRAI DLT portals before disabling development mode.
2. **HTTPS / Secure Cookies**: In production deployment behind an SSL reverse proxy (e.g., Nginx or Cloudflare), enable `server.servlet.session.cookie.secure=true` in `application.properties`.
3. **Database Secrets**: Always provide `SPRING_DATASOURCE_PASSWORD` and `TWILIO_AUTH_TOKEN` via environment variables or secret managers (e.g., AWS Secrets Manager, HashiCorp Vault) rather than hardcoded configuration files.
