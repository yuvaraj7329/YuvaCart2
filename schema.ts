import {
  decimal,
  index,
  int,
  json,
  mysqlEnum,
  mysqlTable,
  text,
  timestamp,
  uniqueIndex,
  varchar,
} from "drizzle-orm/mysql-core";

/** Core user table backing Manus OAuth. */
export const users = mysqlTable("users", {
  id: int("id").autoincrement().primaryKey(),
  openId: varchar("openId", { length: 64 }).notNull().unique(),
  name: text("name"),
  email: varchar("email", { length: 320 }),
  loginMethod: varchar("loginMethod", { length: 64 }),
  role: mysqlEnum("role", ["user", "admin"]).default("user").notNull(),
  otpVerifiedAt: timestamp("otpVerifiedAt"),
  createdAt: timestamp("createdAt").defaultNow().notNull(),
  updatedAt: timestamp("updatedAt").defaultNow().onUpdateNow().notNull(),
  lastSignedIn: timestamp("lastSignedIn").defaultNow().notNull(),
});

export const products = mysqlTable(
  "products",
  {
    id: int("id").autoincrement().primaryKey(),
    name: varchar("name", { length: 180 }).notNull(),
    slug: varchar("slug", { length: 220 }).notNull().unique(),
    description: text("description").notNull(),
    category: varchar("category", { length: 80 }).notNull(),
    imageUrl: varchar("imageUrl", { length: 500 }).notNull(),
    price: decimal("price", { precision: 12, scale: 2 }).notNull(),
    compareAtPrice: decimal("compareAtPrice", { precision: 12, scale: 2 }),
    currency: varchar("currency", { length: 3 }).default("INR").notNull(),
    stock: int("stock").default(0).notNull(),
    sellerName: varchar("sellerName", { length: 120 }).default("YuvaCart Direct").notNull(),
    rating: decimal("rating", { precision: 3, scale: 2 }).default("4.50").notNull(),
    reviewCount: int("reviewCount").default(0).notNull(),
    isFeatured: int("isFeatured").default(0).notNull(),
    specifications: json("specifications").$type<Record<string, string>>(),
    createdAt: timestamp("createdAt").defaultNow().notNull(),
    updatedAt: timestamp("updatedAt").defaultNow().onUpdateNow().notNull(),
  },
  table => ({
    categoryIdx: index("products_category_idx").on(table.category),
    featuredIdx: index("products_featured_idx").on(table.isFeatured),
  })
);

export const cartItems = mysqlTable(
  "cartItems",
  {
    id: int("id").autoincrement().primaryKey(),
    userId: int("userId").notNull(),
    productId: int("productId").notNull(),
    variantId: int("variantId"),
    quantity: int("quantity").default(1).notNull(),
    createdAt: timestamp("createdAt").defaultNow().notNull(),
    updatedAt: timestamp("updatedAt").defaultNow().onUpdateNow().notNull(),
  },
  table => ({
    userProductVariantIdx: uniqueIndex("cart_items_user_product_variant_idx").on(table.userId, table.productId, table.variantId),
    userIdx: index("cart_items_user_idx").on(table.userId, table.updatedAt),
  })
);

export const orders = mysqlTable(
  "orders",
  {
    id: int("id").autoincrement().primaryKey(),
    orderNumber: varchar("orderNumber", { length: 32 }).notNull().unique(),
    userId: int("userId").notNull(),
    status: mysqlEnum("status", ["PENDING_PAYMENT", "PLACED", "CONFIRMED", "CANCELLED"]).default("PENDING_PAYMENT").notNull(),
    stripeCheckoutSessionId: varchar("stripeCheckoutSessionId", { length: 255 }),
    stripePaymentIntentId: varchar("stripePaymentIntentId", { length: 255 }),
    subtotal: decimal("subtotal", { precision: 12, scale: 2 }).notNull(),
    shippingFee: decimal("shippingFee", { precision: 12, scale: 2 }).notNull(),
    total: decimal("total", { precision: 12, scale: 2 }).notNull(),
    currency: varchar("currency", { length: 3 }).default("INR").notNull(),
    shippingName: varchar("shippingName", { length: 120 }).notNull(),
    shippingPhone: varchar("shippingPhone", { length: 30 }).notNull(),
    shippingLine1: varchar("shippingLine1", { length: 240 }).notNull(),
    shippingCity: varchar("shippingCity", { length: 100 }).notNull(),
    shippingPostalCode: varchar("shippingPostalCode", { length: 20 }).notNull(),
    createdAt: timestamp("createdAt").defaultNow().notNull(),
    updatedAt: timestamp("updatedAt").defaultNow().onUpdateNow().notNull(),
  },
  table => ({
    userIdx: index("orders_user_idx").on(table.userId, table.createdAt),
  })
);

export const orderItems = mysqlTable(
  "orderItems",
  {
    id: int("id").autoincrement().primaryKey(),
    orderId: int("orderId").notNull(),
    productId: int("productId").notNull(),
    productName: varchar("productName", { length: 180 }).notNull(),
    imageUrl: varchar("imageUrl", { length: 500 }).notNull(),
    variantId: int("variantId"),
    variantLabel: varchar("variantLabel", { length: 180 }),
    unitPrice: decimal("unitPrice", { precision: 12, scale: 2 }).notNull(),
    quantity: int("quantity").notNull(),
    lineTotal: decimal("lineTotal", { precision: 12, scale: 2 }).notNull(),
    createdAt: timestamp("createdAt").defaultNow().notNull(),
  },
  table => ({
    orderIdx: index("order_items_order_idx").on(table.orderId),
  })
);

export const productVariants = mysqlTable(
  "productVariants",
  {
    id: int("id").autoincrement().primaryKey(),
    productId: int("productId").notNull(),
    label: varchar("label", { length: 180 }).notNull(),
    storage: varchar("storage", { length: 80 }),
    color: varchar("color", { length: 80 }),
    price: decimal("price", { precision: 12, scale: 2 }).notNull(),
    compareAtPrice: decimal("compareAtPrice", { precision: 12, scale: 2 }),
    stock: int("stock").default(0).notNull(),
    sku: varchar("sku", { length: 80 }).notNull().unique(),
    createdAt: timestamp("createdAt").defaultNow().notNull(),
    updatedAt: timestamp("updatedAt").defaultNow().onUpdateNow().notNull(),
  },
  table => ({
    productIdx: index("product_variants_product_idx").on(table.productId),
  })
);

export const otpVerifications = mysqlTable(
  "otpVerifications",
  {
    id: int("id").autoincrement().primaryKey(),
    userId: int("userId").notNull(),
    destination: varchar("destination", { length: 320 }).notNull(),
    codeHash: varchar("codeHash", { length: 128 }).notNull(),
    expiresAt: timestamp("expiresAt").notNull(),
    consumedAt: timestamp("consumedAt"),
    attempts: int("attempts").default(0).notNull(),
    createdAt: timestamp("createdAt").defaultNow().notNull(),
  },
  table => ({
    userIdx: index("otp_verifications_user_idx").on(table.userId, table.createdAt),
    expiryIdx: index("otp_verifications_expiry_idx").on(table.expiresAt),
  })
);

export type User = typeof users.$inferSelect;
export type InsertUser = typeof users.$inferInsert;
export type Product = typeof products.$inferSelect;
export type InsertProduct = typeof products.$inferInsert;
export type ProductVariant = typeof productVariants.$inferSelect;
export type InsertProductVariant = typeof productVariants.$inferInsert;
export type CartItem = typeof cartItems.$inferSelect;
export type InsertCartItem = typeof cartItems.$inferInsert;
export type Order = typeof orders.$inferSelect;
export type InsertOrder = typeof orders.$inferInsert;
export type OrderItem = typeof orderItems.$inferSelect;
export type InsertOrderItem = typeof orderItems.$inferInsert;
export type OtpVerification = typeof otpVerifications.$inferSelect;
export type InsertOtpVerification = typeof otpVerifications.$inferInsert;
