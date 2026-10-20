import { screen } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { beforeEach, describe, expect, it } from "vitest";
import { mockFetch, renderApp } from "../test/helpers.jsx";

const KEY = "engcoach.session";

beforeEach(() => {
  window.localStorage.clear();
  mockFetch(); // Dashboard loads /parts; never hit the real network
});

describe("Session handling (S5b)", () => {
  it("unauthenticated visit to /dashboard is redirected to Login", () => {
    renderApp("/dashboard");
    expect(screen.getByRole("heading", { name: "Log in" })).toBeInTheDocument();
  });

  it("a valid stored session survives a reload: /login redirects to Dashboard", () => {
    window.localStorage.setItem(
      KEY,
      JSON.stringify({ accessToken: "t", expiresAt: new Date(Date.now() + 60_000).toISOString() }),
    );
    renderApp("/login");
    expect(screen.getByRole("heading", { name: "Welcome back" })).toBeInTheDocument();
  });

  it("an expired stored session (ADR-003, 3h) is discarded", () => {
    window.localStorage.setItem(
      KEY,
      JSON.stringify({ accessToken: "t", expiresAt: new Date(Date.now() - 1000).toISOString() }),
    );
    renderApp("/dashboard");
    expect(screen.getByRole("heading", { name: "Log in" })).toBeInTheDocument();
    expect(window.localStorage.getItem(KEY)).toBeNull();
  });

  it("log out clears the stored token and returns to Login", async () => {
    window.localStorage.setItem(
      KEY,
      JSON.stringify({ accessToken: "t", expiresAt: new Date(Date.now() + 60_000).toISOString() }),
    );
    const user = userEvent.setup();
    renderApp("/dashboard");
    await user.click(screen.getByRole("button", { name: "Log out" }));
    expect(screen.getByRole("heading", { name: "Log in" })).toBeInTheDocument();
    expect(window.localStorage.getItem(KEY)).toBeNull();
  });
});
