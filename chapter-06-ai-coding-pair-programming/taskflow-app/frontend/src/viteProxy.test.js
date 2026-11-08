// @vitest-environment node
import { describe, expect, it } from "vitest";
import config from "../vite.config.js";

// Every API path the frontend calls must be proxied in dev, otherwise Vite answers with the SPA page and the
// screen sees no data (this happened with /tests).
describe("Vite dev proxy", () => {
  it("forwards every API path used by the app to the backend", () => {
    const proxied = Object.keys(config.server.proxy);
    for (const path of ["/auth", "/parts", "/attempts", "/tests"]) {
      expect(proxied).toContain(path);
    }
  });
});
