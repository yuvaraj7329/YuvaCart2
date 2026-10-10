import Stripe from "stripe";
import type { Request, Response } from "express";
import {
  attachStripeCheckoutSession,
  cancelPendingOrder,
  completePaidOrder,
  getOrderByStripeCheckoutSession,
  type CheckoutShipping,
  createPendingOrderFromCart,
} from "./db";

function getStripe() {
  const secretKey = process.env.STRIPE_SECRET_KEY;
  if (!secretKey) throw new Error("Stripe is not configured. Add the payment keys in Settings → Payment.");
  return new Stripe(secretKey);
}

function getRequestOrigin(req: Request) {
  const origin = req.headers.origin;
  if (typeof origin === "string" && /^https?:\/\//.test(origin)) return origin;
  const forwardedProto = typeof req.headers["x-forwarded-proto"] === "string" ? req.headers["x-forwarded-proto"].split(",")[0] : req.protocol;
  const forwardedHost = typeof req.headers["x-forwarded-host"] === "string" ? req.headers["x-forwarded-host"].split(",")[0] : req.get("host");
  if (!forwardedHost) throw new Error("Unable to determine the checkout return URL.");
  return `${forwardedProto}://${forwardedHost}`;
}

export async function createStripeCheckout(user: { id: number; email: string | null; name: string | null }, shipping: CheckoutShipping, req: Request) {
  const pending = await createPendingOrderFromCart(user.id, shipping);
  const stripe = getStripe();
  const origin = getRequestOrigin(req);
  const lineItems: Stripe.Checkout.SessionCreateParams.LineItem[] = pending.items.map(item => ({
    quantity: item.quantity,
    price_data: {
      currency: "inr",
      unit_amount: Math.round(Number(item.unitPrice) * 100),
      product_data: {
        name: item.variantLabel ? `${item.productName} · ${item.variantLabel}` : item.productName,
        images: item.imageUrl ? [item.imageUrl] : undefined,
      },
    },
  }));
  if (Number(pending.order.shippingFee) > 0) {
    lineItems.push({
      quantity: 1,
      price_data: {
        currency: "inr",
        unit_amount: Math.round(Number(pending.order.shippingFee) * 100),
        product_data: { name: "Delivery" },
      },
    });
  }

  try {
    const session = await stripe.checkout.sessions.create({
      mode: "payment",
      line_items: lineItems,
      customer_email: user.email ?? undefined,
      client_reference_id: String(user.id),
      allow_promotion_codes: true,
      success_url: `${origin}/?payment=success&session_id={CHECKOUT_SESSION_ID}`,
      cancel_url: `${origin}/?payment=cancelled`,
      metadata: {
        order_id: String(pending.order.id),
        user_id: String(user.id),
        customer_email: user.email ?? "",
        customer_name: user.name ?? "",
      },
      payment_intent_data: {
        metadata: {
          order_id: String(pending.order.id),
          user_id: String(user.id),
        },
      },
    });
    if (!session.url) throw new Error("Stripe did not return a checkout URL.");
    await attachStripeCheckoutSession(pending.order.id, user.id, session.id);
    return { checkoutUrl: session.url, orderNumber: pending.order.orderNumber };
  } catch (error) {
    await cancelPendingOrder(pending.order.id, user.id).catch(cancelError => console.error("[Stripe] Could not cancel failed pending order", cancelError));
    throw error;
  }
}

export async function handleStripeWebhook(req: Request, res: Response) {
  const signature = req.headers["stripe-signature"];
  const webhookSecret = process.env.STRIPE_WEBHOOK_SECRET;
  if (typeof signature !== "string" || !webhookSecret) {
    return res.status(400).json({ error: "Stripe webhook is not configured." });
  }

  let event: Stripe.Event;
  try {
    event = getStripe().webhooks.constructEvent(req.body as Buffer, signature, webhookSecret);
  } catch (error) {
    console.error("[Stripe webhook] Signature verification failed", error instanceof Error ? error.message : error);
    return res.status(400).json({ error: "Invalid webhook signature." });
  }

  if (event.id.startsWith("evt_test_")) {
    console.log("[Webhook] Test event detected, returning verification response");
    return res.json({ verified: true });
  }

  if (event.type === "checkout.session.completed" || event.type === "checkout.session.async_payment_succeeded") {
    const session = event.data.object as Stripe.Checkout.Session;
    const orderId = Number(session.metadata?.order_id);
    const paymentIntentId = typeof session.payment_intent === "string" ? session.payment_intent : session.payment_intent?.id ?? null;
    if (Number.isInteger(orderId) && orderId > 0) {
      await completePaidOrder(orderId, paymentIntentId);
      console.log(`[Stripe webhook] ${event.type} received for order ${orderId}`);
    } else if (session.id) {
      const order = await getOrderByStripeCheckoutSession(session.id);
      if (order) await completePaidOrder(order.id, paymentIntentId);
    }
  }

  return res.json({ received: true });
}

export type { CheckoutShipping };
