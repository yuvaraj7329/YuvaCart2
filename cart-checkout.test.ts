import { describe, expect, it } from "vitest";
import { appRouter } from "./routers";
import type { TrpcContext } from "./_core/context";

type AuthenticatedUser = NonNullable<TrpcContext["user"]>;

const user: AuthenticatedUser = {
  id: 42,
  openId: "cart-test-user",
  email: "buyer@example.com",
  name: "Cart Buyer",
  loginMethod: "manus",
  role: "user",
  otpVerifiedAt: null,
  createdAt: new Date(),
  updatedAt: new Date(),
  lastSignedIn: new Date(),
};

function context(currentUser: AuthenticatedUser | null): TrpcContext {
  return {
    user: currentUser,
    req: { protocol: "https", headers: {} } as TrpcContext["req"],
    res: { clearCookie: () => undefined } as TrpcContext["res"],
  };
}

describe("cart and checkout", () => {
  it("requires authentication to read the persistent cart", async () => {
    const caller = appRouter.createCaller(context(null));
    await expect(caller.cart.get()).rejects.toMatchObject({ code: "UNAUTHORIZED" });
  });

  it("requires authentication to add a product", async () => {
    const caller = appRouter.createCaller(context(null));
    await expect(caller.cart.add({ productId: 1, quantity: 1 })).rejects.toMatchObject({ code: "UNAUTHORIZED" });
  });

  it("requires authentication to start secure payment", async () => {
    const caller = appRouter.createCaller(context(null));
    await expect(caller.checkout.createPayment({
      shippingName: "Cart Buyer",
      shippingPhone: "+919876543210",
      shippingLine1: "12 Main Street",
      shippingCity: "Mumbai",
      shippingPostalCode: "400001",
    })).rejects.toMatchObject({ code: "UNAUTHORIZED" });
  });

  it("requires authentication to read order history", async () => {
    const caller = appRouter.createCaller(context(null));
    await expect(caller.orders.list()).rejects.toMatchObject({ code: "UNAUTHORIZED" });
  });

  it("validates delivery details before attempting checkout", async () => {
    const caller = appRouter.createCaller(context(user));
    await expect(caller.checkout.place({
      shippingName: "Buyer",
      shippingPhone: "123",
      shippingLine1: "12 Main Street",
      shippingCity: "Mumbai",
      shippingPostalCode: "400001",
    })).rejects.toMatchObject({ code: "BAD_REQUEST" });
  });
});
