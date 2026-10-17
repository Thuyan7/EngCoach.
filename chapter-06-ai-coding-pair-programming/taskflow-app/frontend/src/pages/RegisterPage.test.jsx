import { screen } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { beforeEach, describe, expect, it } from "vitest";
import { AUTH_OK, jsonResponse, mockFetch, renderApp } from "../test/helpers.jsx";

beforeEach(() => window.localStorage.clear());

async function fillAndSubmit(user, email, password) {
  await user.type(screen.getByLabelText("Email"), email);
  await user.type(screen.getByLabelText("Password"), password);
  await user.click(screen.getByRole("button", { name: "Create account" }));
}

describe("Register screen (S5b)", () => {
  it("201: calls POST /auth/register, auto-logs-in, lands on Dashboard", async () => {
    const user = userEvent.setup();
    const fetchMock = mockFetch(jsonResponse(201, AUTH_OK));
    renderApp("/register");

    await fillAndSubmit(user, "new@example.com", "correct-horse-battery");

    expect(await screen.findByRole("heading", { name: "Dashboard" })).toBeInTheDocument();
    expect(fetchMock.mock.calls[0][0]).toBe("/auth/register");
    expect(window.localStorage.getItem("engcoach.session")).toContain("jwt-from-server");
  });

  it("409: marks the email field with an 'already exists' error and stays on Register", async () => {
    const user = userEvent.setup();
    mockFetch(jsonResponse(409, { title: "Email already registered", status: 409 }));
    renderApp("/register");

    await fillAndSubmit(user, "taken@example.com", "correct-horse-battery");

    expect(await screen.findByText("An account with this email already exists.")).toBeInTheDocument();
    expect(screen.getByLabelText("Email")).toHaveAttribute("aria-invalid", "true");
    expect(window.localStorage.getItem("engcoach.session")).toBeNull();
  });

  it("422: shows the API's per-field messages inline, linked to their inputs", async () => {
    const user = userEvent.setup();
    mockFetch(
      jsonResponse(422, {
        title: "Validation failed",
        status: 422,
        fieldErrors: {
          email: "email must be a valid email address",
          password: "password must be at least 8 characters",
        },
      }),
    );
    renderApp("/register");

    await fillAndSubmit(user, "not-an-email", "short");

    expect(await screen.findByText("email must be a valid email address")).toBeInTheDocument();
    expect(screen.getByText("password must be at least 8 characters")).toBeInTheDocument();
    expect(screen.getByLabelText("Password")).toHaveAccessibleDescription(
      "password must be at least 8 characters",
    );
  });

  it("server unreachable: shows a form-level alert", async () => {
    const user = userEvent.setup();
    mockFetch(new TypeError("Failed to fetch"));
    renderApp("/register");

    await fillAndSubmit(user, "new@example.com", "correct-horse-battery");

    expect(await screen.findByRole("alert")).toHaveTextContent("couldn't reach the server");
  });

  it("links between Login and Register work", async () => {
    const user = userEvent.setup();
    renderApp("/login");
    await user.click(screen.getByRole("link", { name: "Create an account" }));
    expect(screen.getByRole("heading", { name: "Create your account" })).toBeInTheDocument();
    await user.click(screen.getByRole("link", { name: "Log in" }));
    expect(screen.getByRole("heading", { name: "Log in" })).toBeInTheDocument();
  });
});
