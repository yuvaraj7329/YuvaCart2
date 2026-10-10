import { and, desc, eq, gte, isNull, like, or, sql } from "drizzle-orm";
import { drizzle } from "drizzle-orm/mysql2";
import {
  cartItems,
  InsertOtpVerification,
  InsertOrder,
  InsertOrderItem,
  InsertProduct,
  InsertUser,
  orderItems,
  orders,
  otpVerifications,
  productVariants,
  products,
  users,
} from "../drizzle/schema";
import { ENV } from "./_core/env";

let _db: ReturnType<typeof drizzle> | null = null;

export async function getDb() {
  if (!_db && process.env.DATABASE_URL) {
    try {
      _db = drizzle(process.env.DATABASE_URL);
    } catch (error) {
      console.warn("[Database] Failed to connect:", error);
      _db = null;
    }
  }
  return _db;
}

export async function upsertUser(user: InsertUser): Promise<void> {
  if (!user.openId) throw new Error("User openId is required for upsert");
  const db = await getDb();
  if (!db) {
    console.warn("[Database] Cannot upsert user: database not available");
    return;
  }

  const values: InsertUser = { openId: user.openId };
  const updateSet: Record<string, unknown> = {};
  const textFields = ["name", "email", "loginMethod"] as const;
  type TextField = (typeof textFields)[number];

  const assignNullable = (field: TextField) => {
    const value = user[field];
    if (value === undefined) return;
    const normalized = value ?? null;
    values[field] = normalized;
    updateSet[field] = normalized;
  };
  textFields.forEach(assignNullable);
  if (user.lastSignedIn !== undefined) {
    values.lastSignedIn = user.lastSignedIn;
    updateSet.lastSignedIn = user.lastSignedIn;
  }
  if (user.role !== undefined) {
    values.role = user.role;
    updateSet.role = user.role;
  } else if (user.openId === ENV.ownerOpenId) {
    values.role = "admin";
    updateSet.role = "admin";
  }
  if (!values.lastSignedIn) values.lastSignedIn = new Date();
  if (Object.keys(updateSet).length === 0) updateSet.lastSignedIn = new Date();
  await db.insert(users).values(values).onDuplicateKeyUpdate({ set: updateSet });
}

export async function getUserByOpenId(openId: string) {
  const db = await getDb();
  if (!db) return undefined;
  const result = await db.select().from(users).where(eq(users.openId, openId)).limit(1);
  return result[0];
}

export async function listProducts(input?: { query?: string; category?: string }) {
  const db = await getDb();
  if (!db) return [];
  const filters = [];
  if (input?.category && input.category !== "All") filters.push(eq(products.category, input.category));
  if (input?.query?.trim()) {
    const query = `%${input.query.trim()}%`;
    filters.push(or(like(products.name, query), like(products.description, query)));
  }
  return db.select().from(products).where(filters.length ? and(...filters) : undefined).orderBy(desc(products.isFeatured), desc(products.createdAt));
}

export async function getProductBySlug(slug: string) {
  const db = await getDb();
  if (!db) return undefined;
  const productRows = await db.select().from(products).where(eq(products.slug, slug)).limit(1);
  const product = productRows[0];
  if (!product) return undefined;
  const variants = await db.select().from(productVariants).where(eq(productVariants.productId, product.id)).orderBy(productVariants.price);
  return { ...product, variants };
}

export async function createProduct(product: InsertProduct) {
  const db = await getDb();
  if (!db) throw new Error("Database is not available");
  const result = await db.insert(products).values(product);
  const created = await db.select().from(products).where(eq(products.id, Number(result[0].insertId))).limit(1);
  return created[0];
}

export async function getCart(userId: number) {
  const db = await getDb();
  if (!db) return { items: [], subtotal: 0, count: 0, currency: "INR" };
  const rows = await db
    .select({ cartItem: cartItems, product: products, variant: productVariants })
    .from(cartItems)
    .innerJoin(products, eq(cartItems.productId, products.id))
    .leftJoin(productVariants, eq(cartItems.variantId, productVariants.id))
    .where(eq(cartItems.userId, userId))
    .orderBy(desc(cartItems.updatedAt));
  const items = rows.map(({ cartItem, product, variant }) => ({
    ...cartItem,
    product,
    variant,
    unitPrice: Number(variant?.price ?? product.price).toFixed(2),
    lineTotal: (Number(variant?.price ?? product.price) * cartItem.quantity).toFixed(2),
  }));
  return {
    items,
    subtotal: items.reduce((sum, item) => sum + Number(item.lineTotal), 0).toFixed(2),
    count: items.reduce((sum, item) => sum + item.quantity, 0),
    currency: "INR",
  };
}

export async function addCartItem(userId: number, productId: number, quantity: number, variantId?: number) {
  const db = await getDb();
  if (!db) throw new Error("Database is not available");
  const product = await db.select().from(products).where(eq(products.id, productId)).limit(1);
  if (!product[0]) throw new Error("Product not found");
  const variant = variantId === undefined ? undefined : (await db.select().from(productVariants).where(and(eq(productVariants.id, variantId), eq(productVariants.productId, productId))).limit(1))[0];
  if (variantId !== undefined && !variant) throw new Error("This product variant is no longer available");
  const availableStock = variant?.stock ?? product[0].stock;
  if (quantity > availableStock) throw new Error("Not enough stock for this product");
  const variantFilter = variantId === undefined ? isNull(cartItems.variantId) : eq(cartItems.variantId, variantId);
  const existing = await db.select().from(cartItems).where(and(eq(cartItems.userId, userId), eq(cartItems.productId, productId), variantFilter)).limit(1);
  if (existing[0]) {
    const nextQuantity = existing[0].quantity + quantity;
    if (nextQuantity > availableStock) throw new Error("Not enough stock for this product");
    await db.update(cartItems).set({ quantity: nextQuantity }).where(eq(cartItems.id, existing[0].id));
  } else {
    await db.insert(cartItems).values({ userId, productId, variantId: variantId ?? null, quantity });
  }
  return getCart(userId);
}

export async function updateCartItem(userId: number, productId: number, quantity: number, variantId?: number) {
  const db = await getDb();
  if (!db) throw new Error("Database is not available");
  const product = await db.select().from(products).where(eq(products.id, productId)).limit(1);
  if (!product[0]) throw new Error("Product not found");
  const variant = variantId === undefined ? undefined : (await db.select().from(productVariants).where(and(eq(productVariants.id, variantId), eq(productVariants.productId, productId))).limit(1))[0];
  if (variantId !== undefined && !variant) throw new Error("This product variant is no longer available");
  if (quantity > (variant?.stock ?? product[0].stock)) throw new Error("Not enough stock for this product");
  const variantFilter = variantId === undefined ? isNull(cartItems.variantId) : eq(cartItems.variantId, variantId);
  await db.update(cartItems).set({ quantity }).where(and(eq(cartItems.userId, userId), eq(cartItems.productId, productId), variantFilter));
  return getCart(userId);
}

export async function removeCartItem(userId: number, productId: number, variantId?: number) {
  const db = await getDb();
  if (!db) throw new Error("Database is not available");
  const variantFilter = variantId === undefined ? isNull(cartItems.variantId) : eq(cartItems.variantId, variantId);
  await db.delete(cartItems).where(and(eq(cartItems.userId, userId), eq(cartItems.productId, productId), variantFilter));
  return getCart(userId);
}

export async function createOrderFromCart(userId: number, shipping: Omit<InsertOrder, "id" | "orderNumber" | "userId" | "subtotal" | "shippingFee" | "total" | "status" | "currency" | "createdAt" | "updatedAt">) {
  const db = await getDb();
  if (!db) throw new Error("Database is not available");
  return db.transaction(async tx => {
    const rows = await tx.select({ cartItem: cartItems, product: products }).from(cartItems).innerJoin(products, eq(cartItems.productId, products.id)).where(eq(cartItems.userId, userId));
    if (!rows.length) throw new Error("Your cart is empty");
    const unavailable = rows.find(({ cartItem, product }) => product.stock < cartItem.quantity);
    if (unavailable) throw new Error(`${unavailable.product.name} does not have enough stock.`);

    const subtotal = rows.reduce((sum, { cartItem, product }) => sum + Number(product.price) * cartItem.quantity, 0);
    const shippingFee = subtotal >= 999 ? 0 : 79;
    const total = subtotal + shippingFee;
    const orderNumber = `YC-${new Date().toISOString().slice(0, 10).replace(/-/g, "")}-${Math.random().toString(36).slice(2, 8).toUpperCase()}`;
    const orderValues: InsertOrder = {
      ...shipping,
      userId,
      orderNumber,
      status: "PLACED",
      subtotal: subtotal.toFixed(2),
      shippingFee: shippingFee.toFixed(2),
      total: total.toFixed(2),
      currency: "INR",
    };
    const result = await tx.insert(orders).values(orderValues);
    const orderId = Number(result[0].insertId);
    const itemValues: InsertOrderItem[] = [];
    for (const { cartItem, product } of rows) {
      const updated = await tx.update(products).set({ stock: sql`${products.stock} - ${cartItem.quantity}` }).where(and(eq(products.id, product.id), gte(products.stock, cartItem.quantity)));
      if (updated[0].affectedRows !== 1) throw new Error(`${product.name} just sold out. Please refresh your cart.`);
      itemValues.push({ orderId, productId: product.id, productName: product.name, imageUrl: product.imageUrl, unitPrice: Number(product.price).toFixed(2), quantity: cartItem.quantity, lineTotal: (Number(product.price) * cartItem.quantity).toFixed(2) });
    }
    await tx.insert(orderItems).values(itemValues);
    await tx.delete(cartItems).where(eq(cartItems.userId, userId));
    const order = await tx.select().from(orders).where(eq(orders.id, orderId)).limit(1);
    return { order: order[0], items: itemValues };
  });
}

export type CheckoutShipping = Omit<InsertOrder, "id" | "orderNumber" | "userId" | "subtotal" | "shippingFee" | "total" | "status" | "currency" | "createdAt" | "updatedAt" | "stripeCheckoutSessionId" | "stripePaymentIntentId">;

export async function createPendingOrderFromCart(userId: number, shipping: CheckoutShipping) {
  const db = await getDb();
  if (!db) throw new Error("Database is not available");
  return db.transaction(async tx => {
    const rows = await tx.select({ cartItem: cartItems, product: products, variant: productVariants }).from(cartItems).innerJoin(products, eq(cartItems.productId, products.id)).leftJoin(productVariants, eq(cartItems.variantId, productVariants.id)).where(eq(cartItems.userId, userId));
    if (!rows.length) throw new Error("Your cart is empty");
    const unavailable = rows.find(({ cartItem, product, variant }) => (variant?.stock ?? product.stock) < cartItem.quantity);
    if (unavailable) throw new Error(`${unavailable.product.name} does not have enough stock.`);

    const subtotal = rows.reduce((sum, { cartItem, product, variant }) => sum + Number(variant?.price ?? product.price) * cartItem.quantity, 0);
    const shippingFee = subtotal >= 999 ? 0 : 79;
    const total = subtotal + shippingFee;
    const orderNumber = `YC-${new Date().toISOString().slice(0, 10).replace(/-/g, "")}-${Math.random().toString(36).slice(2, 8).toUpperCase()}`;
    const result = await tx.insert(orders).values({
      ...shipping,
      userId,
      orderNumber,
      status: "PENDING_PAYMENT",
      subtotal: subtotal.toFixed(2),
      shippingFee: shippingFee.toFixed(2),
      total: total.toFixed(2),
      currency: "INR",
    });
    const orderId = Number(result[0].insertId);
    const itemValues: InsertOrderItem[] = rows.map(({ cartItem, product, variant }) => ({
      orderId,
      productId: product.id,
      productName: product.name,
      imageUrl: product.imageUrl,
      variantId: cartItem.variantId,
      variantLabel: variant?.label ?? null,
      unitPrice: Number(variant?.price ?? product.price).toFixed(2),
      quantity: cartItem.quantity,
      lineTotal: (Number(variant?.price ?? product.price) * cartItem.quantity).toFixed(2),
    }));
    await tx.insert(orderItems).values(itemValues);
    const order = await tx.select().from(orders).where(eq(orders.id, orderId)).limit(1);
    return { order: order[0], items: itemValues };
  });
}

export async function attachStripeCheckoutSession(orderId: number, userId: number, stripeCheckoutSessionId: string) {
  const db = await getDb();
  if (!db) throw new Error("Database is not available");
  await db.update(orders).set({ stripeCheckoutSessionId }).where(and(eq(orders.id, orderId), eq(orders.userId, userId), eq(orders.status, "PENDING_PAYMENT")));
}

export async function getOrderByStripeCheckoutSession(stripeCheckoutSessionId: string) {
  const db = await getDb();
  if (!db) throw new Error("Database is not available");
  const result = await db.select().from(orders).where(eq(orders.stripeCheckoutSessionId, stripeCheckoutSessionId)).limit(1);
  return result[0];
}

export async function cancelPendingOrder(orderId: number, userId: number) {
  const db = await getDb();
  if (!db) return;
  await db.update(orders).set({ status: "CANCELLED" }).where(and(eq(orders.id, orderId), eq(orders.userId, userId), eq(orders.status, "PENDING_PAYMENT")));
}

export async function completePaidOrder(orderId: number, stripePaymentIntentId: string | null) {
  const db = await getDb();
  if (!db) throw new Error("Database is not available");
  return db.transaction(async tx => {
    const orderRows = await tx.select().from(orders).where(eq(orders.id, orderId)).limit(1);
    const order = orderRows[0];
    if (!order) throw new Error("Order not found");
    if (order.status !== "PENDING_PAYMENT") return order;
    const items = await tx.select().from(orderItems).where(eq(orderItems.orderId, orderId));
    for (const item of items) {
      const updated = item.variantId
        ? await tx.update(productVariants).set({ stock: sql`${productVariants.stock} - ${item.quantity}` }).where(and(eq(productVariants.id, item.variantId), gte(productVariants.stock, item.quantity)))
        : await tx.update(products).set({ stock: sql`${products.stock} - ${item.quantity}` }).where(and(eq(products.id, item.productId), gte(products.stock, item.quantity)));
      if (updated[0].affectedRows !== 1) throw new Error(`${item.productName} just sold out. Please contact support for a refund.`);
      const variantFilter = item.variantId ? eq(cartItems.variantId, item.variantId) : isNull(cartItems.variantId);
      await tx.delete(cartItems).where(and(eq(cartItems.userId, order.userId), eq(cartItems.productId, item.productId), variantFilter));
    }
    await tx.update(orders).set({ status: "CONFIRMED", stripePaymentIntentId }).where(eq(orders.id, orderId));
    const updatedOrder = await tx.select().from(orders).where(eq(orders.id, orderId)).limit(1);
    return updatedOrder[0];
  });
}

export async function listOrdersForUser(userId: number) {
  const db = await getDb();
  if (!db) return [];
  const rows = await db.select().from(orders).where(eq(orders.userId, userId)).orderBy(desc(orders.createdAt));
  return Promise.all(rows.map(async order => ({
    ...order,
    items: await db.select().from(orderItems).where(eq(orderItems.orderId, order.id)),
  })));
}

export async function createOtpVerification(input: InsertOtpVerification) {
  const db = await getDb();
  if (!db) throw new Error("Database is not available");
  await db.insert(otpVerifications).values(input);
}

export async function getLatestOtpVerification(userId: number, destination: string) {
  const db = await getDb();
  if (!db) return undefined;
  const result = await db.select().from(otpVerifications).where(and(eq(otpVerifications.userId, userId), eq(otpVerifications.destination, destination))).orderBy(desc(otpVerifications.createdAt)).limit(1);
  return result[0];
}

export async function incrementOtpAttempt(id: number) {
  const db = await getDb();
  if (!db) throw new Error("Database is not available");
  const existing = await db.select().from(otpVerifications).where(eq(otpVerifications.id, id)).limit(1);
  if (!existing[0]) return;
  await db.update(otpVerifications).set({ attempts: existing[0].attempts + 1 }).where(eq(otpVerifications.id, id));
}

export async function consumeOtp(id: number, userId: number) {
  const db = await getDb();
  if (!db) throw new Error("Database is not available");
  await db.update(otpVerifications).set({ consumedAt: new Date() }).where(eq(otpVerifications.id, id));
  await db.update(users).set({ otpVerifiedAt: new Date() }).where(eq(users.id, userId));
}
