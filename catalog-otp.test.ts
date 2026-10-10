import { describe, expect, it } from "vitest";
import { appRouter } from "./routers";
import type { TrpcContext } from "./_core/context";

type AuthenticatedUser = NonNullable<TrpcContext["user"]>;

function createContext(user: AuthenticatedUser | null): TrpcContext {
  return {
    user,
    req: { protocol: "https", headers: {} } as TrpcContext["req"],
    res: { clearCookie: () => undefined } as TrpcContext["res"],
  };
}

const user: AuthenticatedUser = {
  id: 42,
  openId: "catalog-test-user",
  email: "seller@example.com",
  name: "Catalog Seller",
  loginMethod: "manus",
  role: "user",
  otpVerifiedAt: null,
  createdAt: new Date(),
  updatedAt: new Date(),
  lastSignedIn: new Date(),
};

describe("products.create", () => {
  it("requires a signed-in user", async () => {
    const caller = appRouter.createCaller(createContext(null));
    await expect(caller.products.create({
      name: "Test product",
      description: "A product description that is long enough.",
      category: "Tech",
      price: 100,
      stock: 2,
    })).rejects.toMatchObject({ code: "UNAUTHORIZED" });
  });

  it("rejects a compare-at price below the server selling price", async () => {
    const caller = appRouter.createCaller(createContext(user));
    await expect(caller.products.create({
      name: "Test product",
      description: "A product description that is long enough.",
      category: "Tech",
      price: 1000,
      compareAtPrice: 999,
      stock: 2,
    })).rejects.toThrow("Compare-at price must be at least the selling price.");
  });
});

describe("otp.verify", () => {
  it("rejects codes that are not six digits", async () => {
    const caller = appRouter.createCaller(createContext(user));
    await expect(caller.otp.verify({ code: "123" })).rejects.toMatchObject({ code: "BAD_REQUEST" });
  });
});
