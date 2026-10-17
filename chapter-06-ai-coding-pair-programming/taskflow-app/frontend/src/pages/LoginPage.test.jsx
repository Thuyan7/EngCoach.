import { screen, waitFor } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { beforeEach, describe, expect, it } from "vitest";
import { AUTH_OK, jsonResponse, mockFetch, renderApp } from "../test/helpers.jsx";

beforeEach(() => window.localStorage.clear());

async function fillAndSubmit(user, email, password) {
  await user.type(screen.getByLabelText("Email"), email);
  await user.type(screen.getByLabelText("Password"), password);
  await user.click(screen.getByRole("button", { name: "Log in" }));
}

describe("Login screen (S5b)", () => {
  it("fields have real associated labels", () => {
    renderApp("/login");
    expect(screen.getByLabelText("Email")).toHaveAttribute("type", "email");
    expect(screen.getByLabelText("Password")).toHaveAttribute("type", "password");
  });

  it("valid credentials: calls POST /auth/login, stores the JWT, lands on Dashboard", async () => {
    const user = userEvent.setup();
    const fetchMock = mockFetch(jsonResponse(200, AUTH_OK));
    renderApp("/login");

    await fillAndSubmit(user, "learner@example.com", "correct-horse-battery");

    expect(await screen.findByRole("heading", { name: "Dashboard" })).toBeInTheDocument();
    const [url, init] = fetchMock.mock.calls[0];
    expect(url).toBe("/auth/login");
    expect(init.method).toBe("POST");
    expect(JSON.parse(init.body)).toEqual({
      email: "learner@example.com",
      password: "correct-horse-battery",
    });
    expect(JSON.parse(window.localStorage.getItem("engcoach.session")).accessToken).toBe(
      "jwt-from-server",
    );
  });

  it("wrong credentials (401): shows the generic message, no token stored, stays on Login", async () => {
    const user = userEvent.setup();
    mockFetch(
      jsonResponse(401, { title: "Invalid credentials", status: 401, detail: "Invalid email or password." }),
    );
    renderApp("/login");

    await fillAndSubmit(user, "learner@example.com", "wrong-password");

    expect(await screen.findByRole("alert")).toHaveTextContent("Email or password is incorrect.");
    expect(screen.getByRole("heading", { name: "Log in" })).toBeInTheDocument();
    expect(window.localStorage.getItem("engcoach.session")).toBeNull();
  });

  it("any 4xx collapses to the same message (no field disclosure)", async () => {
    const user = userEvent.setup();
    mockFetch(jsonResponse(422, { fieldErrors: { email: "email must be a valid email address" } }));
    renderApp("/login");

    await fillAndSubmit(user, "nope", "x");

    expect(await screen.findByRole("alert")).toHaveTextContent("Email or password is incorrect.");
    expect(screen.queryByText(/valid email address/)).not.toBeInTheDocument();
  });

  it("server unreachable: shows a retry message instead of the credentials error", async () => {
    const user = userEvent.setup();
    mockFetch(new TypeError("Failed to fetch"));
    renderApp("/login");

    await fillAndSubmit(user, "learner@example.com", "correct-horse-battery");

    expect(await screen.findByRole("alert")).toHaveTextContent("couldn't reach the server");
  });

  it("loading state: button is disabled and relabelled while the request is pending", async () => {
    const user = userEvent.setup();
    let release;
    globalThis.fetch = () => new Promise((resolve) => (release = resolve));
    renderApp("/login");

    await fillAndSubmit(user, "learner@example.com", "correct-horse-battery");

    const busy = await screen.findByRole("button", { name: "Logging in…" });
    expect(busy).toBeDisabled();

    release(jsonResponse(200, AUTH_OK));
    await waitFor(() => expect(screen.getByRole("heading", { name: "Dashboard" })).toBeInTheDocument());
  });

  it("form can be submitted with the keyboard (Enter)", async () => {
    const user = userEvent.setup();
    mockFetch(jsonResponse(200, AUTH_OK));
    renderApp("/login");

    await user.type(screen.getByLabelText("Email"), "learner@example.com");
    await user.type(screen.getByLabelText("Password"), "correct-horse-battery{Enter}");

    expect(await screen.findByRole("heading", { name: "Dashboard" })).toBeInTheDocument();
  });
});
