import { COOKIE_NAME } from "@shared/const";
import { randomInt, createHash, timingSafeEqual } from "node:crypto";
import { TRPCError } from "@trpc/server";
import { z } from "zod";
import { getSessionCookieOptions } from "./_core/cookies";
import { ENV } from "./_core/env";
import { systemRouter } from "./_core/systemRouter";
import { protectedProcedure, publicProcedure, router } from "./_core/trpc";
import {
  addCartItem,
  cancelPendingOrder,
  consumeOtp,
  listOrdersForUser,
  createOtpVerification,
  createProduct,
  getCart,
  getProductBySlug,
  getLatestOtpVerification,
  incrementOtpAttempt,
  listProducts,
  removeCartItem,
  updateCartItem,
} from "./db";
import { createStripeCheckout } from "./stripe";

const productInput = z.object({
  name: z.string().trim().min(3).max(180),
  description: z.string().trim().min(12).max(3000),
  category: z.string().trim().min(2).max(80),
  price: z.coerce.number().finite().positive().max(10_000_000),
  compareAtPrice: z.coerce.number().finite().positive().max(10_000_000).optional(),
  stock: z.coerce.number().int().min(0).max(1_000_000),
  imageUrl: z.string().url().max(500).optional(),
});

const maskEmail = (email: string) => {
  const [local, domain] = email.split("@");
  if (!local || !domain) return "your verified email";
  return `${local.slice(0, 2)}${"•".repeat(Math.max(2, local.length - 2))}@${domain}`;
};
const hashOtp = (code: string) => createHash("sha256").update(code).digest("hex");
const cartItemInput = z.object({ productId: z.number().int().positive(), variantId: z.number().int().positive().optional(), quantity: z.number().int().min(1).max(99) });
const shippingInput = z.object({
  shippingName: z.string().trim().min(2).max(120),
  shippingPhone: z.string().trim().min(8).max(30),
  shippingLine1: z.string().trim().min(5).max(240),
  shippingCity: z.string().trim().min(2).max(100),
  shippingPostalCode: z.string().trim().min(4).max(20),
});

export const appRouter = router({
  system: systemRouter,
  auth: router({
    me: publicProcedure.query(opts => opts.ctx.user),
    logout: publicProcedure.mutation(({ ctx }) => {
      const cookieOptions = getSessionCookieOptions(ctx.req);
      ctx.res.clearCookie(COOKIE_NAME, { ...cookieOptions, maxAge: -1 });
      return { success: true } as const;
    }),
  }),

  products: router({
    list: publicProcedure.input(z.object({ query: z.string().optional(), category: z.string().optional() }).optional()).query(({ input }) => listProducts(input)),
    get: publicProcedure.input(z.object({ slug: z.string().trim().min(1).max(220) })).query(({ input }) => getProductBySlug(input.slug)),
    create: protectedProcedure.input(productInput).mutation(async ({ ctx, input }) => {
      const price = Number(input.price.toFixed(2));
      const compareAtPrice = input.compareAtPrice === undefined ? null : Number(input.compareAtPrice.toFixed(2));
      if (compareAtPrice !== null && compareAtPrice < price) throw new TRPCError({ code: "BAD_REQUEST", message: "Compare-at price must be at least the selling price." });
      return createProduct({
        name: input.name,
        slug: `${input.name.toLowerCase().replace(/[^a-z0-9]+/g, "-").replace(/(^-|-$)/g, "")}-${Date.now()}`,
        description: input.description,
        category: input.category,
        imageUrl: input.imageUrl ?? "https://images.unsplash.com/photo-1523275335684-37898b6baf30?auto=format&fit=crop&w=900&q=85",
        price: price.toFixed(2),
        compareAtPrice: compareAtPrice === null ? null : compareAtPrice.toFixed(2),
        currency: "INR",
        stock: input.stock,
        sellerName: ctx.user.name ?? "YuvaCart Community Seller",
        rating: "4.50",
        reviewCount: 0,
        isFeatured: 0,
      });
    }),
  }),

  cart: router({
    get: protectedProcedure.query(({ ctx }) => getCart(ctx.user.id)),
    add: protectedProcedure.input(cartItemInput).mutation(({ ctx, input }) => addCartItem(ctx.user.id, input.productId, input.quantity, input.variantId)),
    update: protectedProcedure.input(cartItemInput).mutation(({ ctx, input }) => updateCartItem(ctx.user.id, input.productId, input.quantity, input.variantId)),
    remove: protectedProcedure.input(z.object({ productId: z.number().int().positive(), variantId: z.number().int().positive().optional() })).mutation(({ ctx, input }) => removeCartItem(ctx.user.id, input.productId, input.variantId)),
  }),

  checkout: router({
    createPayment: protectedProcedure.input(shippingInput).mutation(async ({ ctx, input }) => {
      try {
        return await createStripeCheckout(ctx.user, input, ctx.req);
      } catch (error) {
        throw new TRPCError({ code: "BAD_REQUEST", message: error instanceof Error ? error.message : "We could not start secure checkout." });
      }
    }),
    /** Legacy contract kept for existing clients; paid orders must use createPayment. */
    place: protectedProcedure.input(shippingInput).mutation(async () => {
      throw new TRPCError({ code: "BAD_REQUEST", message: "Payment is required to place an order. Continue to secure checkout instead." });
    }),
  }),

  orders: router({
    list: protectedProcedure.query(({ ctx }) => listOrdersForUser(ctx.user.id)),
  }),

  otp: router({
    send: protectedProcedure.mutation(async ({ ctx }) => {
      const destination = ctx.user.email?.trim().toLowerCase();
      if (!destination) throw new TRPCError({ code: "BAD_REQUEST", message: "Your account needs a verified email before OTP can be sent." });
      const code = String(randomInt(100000, 1000000));
      const expiresAt = new Date(Date.now() + 10 * 60 * 1000);
      await createOtpVerification({ userId: ctx.user.id, destination, codeHash: hashOtp(code), expiresAt, attempts: 0 });
      return { accepted: true, destination: maskEmail(destination), expiresAt, ...(ENV.isProduction ? {} : { demoCode: code }) };
    }),
    verify: protectedProcedure.input(z.object({ code: z.string().regex(/^\d{6}$/, "Enter the 6-digit OTP.") })).mutation(async ({ ctx, input }) => {
      const destination = ctx.user.email?.trim().toLowerCase();
      if (!destination) throw new TRPCError({ code: "BAD_REQUEST", message: "No verified email is available for this account." });
      const record = await getLatestOtpVerification(ctx.user.id, destination);
      if (!record || record.consumedAt || record.expiresAt.getTime() < Date.now()) throw new TRPCError({ code: "BAD_REQUEST", message: "That OTP is expired. Request a new code." });
      if (record.attempts >= 5) throw new TRPCError({ code: "TOO_MANY_REQUESTS", message: "Too many attempts. Request a new code." });
      const expected = Buffer.from(record.codeHash, "utf8");
      const actual = Buffer.from(hashOtp(input.code), "utf8");
      if (expected.length !== actual.length || !timingSafeEqual(expected, actual)) {
        await incrementOtpAttempt(record.id);
        throw new TRPCError({ code: "BAD_REQUEST", message: "That OTP does not match." });
      }
      await consumeOtp(record.id, ctx.user.id);
      return { verified: true } as const;
    }),
  }),
});

export type AppRouter = typeof appRouter;
