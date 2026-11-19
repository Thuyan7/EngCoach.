// @vitest-environment node
import { describe, expect, it } from "vitest";
import config from "../vite.config.js";

// Every API path the frontend calls must be proxied in dev, otherwise Vite answers with the SPA page and the
// screen sees no data (this happened with /tests).
describe("Vite dev proxy", () => {
  it("forwards every API path used by the app to the backend", () => {
    const proxied = Object.keys(config.server.proxy);
    for (const path of ["/auth", "/parts", "/attempts", "/tests", "/streak"]) {
      expect(proxied).toContain(path);
    }
    expect(proxied.some((key) => key.startsWith("^/vocab"))).toBe(true);
  });

  it("proxies the vocab API but not the /vocabulary pages", () => {
    const key = Object.keys(config.server.proxy).find((k) => k.startsWith("^/vocab"));
    const matches = (url) => new RegExp(key).test(url);
    for (const api of ["/vocab", "/vocab/3", "/vocab/review", "/vocab?x=1", "/vocab/lookup?word=a"]) {
      expect(matches(api)).toBe(true);
    }
    for (const page of ["/vocabulary", "/vocabulary/review"]) {
      expect(matches(page)).toBe(false);
    }
  });
});
