import { screen } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { beforeEach, describe, expect, it } from "vitest";
import { jsonResponse, mockFetch, renderApp } from "../test/helpers.jsx";

beforeEach(() => window.localStorage.clear());

describe("Forgot password screen", () => {
  it("shows a validation message and does not call the API when email is blank", async () => {
    const user = userEvent.setup();
    const fetchMock = mockFetch(jsonResponse(200, {}));
    renderApp("/forgot-password");

    await user.click(screen.getByRole("button", { name: "Send reset instructions" }));

    expect(screen.getByRole("alert")).toHaveTextContent("Enter the email address associated with your account.");
    expect(fetchMock).not.toHaveBeenCalled();
  });

  it("rejects an invalid email before making a request", async () => {
    const user = userEvent.setup();
    const fetchMock = mockFetch(jsonResponse(200, {}));
    renderApp("/forgot-password");

    await user.type(screen.getByLabelText("Email"), "not-an-email");
    await user.click(screen.getByRole("button", { name: "Send reset instructions" }));

    expect(screen.getByRole("alert")).toHaveTextContent("Enter a valid email address.");
    expect(fetchMock).not.toHaveBeenCalled();
  });

  it("submits the trimmed email and shows a privacy-preserving confirmation", async () => {
    const user = userEvent.setup();
    const fetchMock = mockFetch(jsonResponse(200, {}));
    renderApp("/forgot-password");

    await user.type(screen.getByLabelText("Email"), "  learner@example.com  ");
    await user.click(screen.getByRole("button", { name: "Send reset instructions" }));

    expect(await screen.findByRole("status")).toHaveTextContent(
      "If an account matches that email, password-reset instructions will be provided.",
    );
    expect(fetchMock).toHaveBeenCalledTimes(1);
    expect(fetchMock.mock.calls[0][0]).toBe("/auth/forgot-password");
    expect(fetchMock.mock.calls[0][1].method).toBe("POST");
    expect(JSON.parse(fetchMock.mock.calls[0][1].body)).toEqual({ email: "learner@example.com" });
    expect(screen.queryByRole("button", { name: "Send reset instructions" })).not.toBeInTheDocument();
  });

  it("shows a generic message for server errors without revealing whether an account exists", async () => {
    const user = userEvent.setup();
    mockFetch(jsonResponse(500, { detail: "Database unavailable" }));
    renderApp("/forgot-password");

    await user.type(screen.getByLabelText("Email"), "learner@example.com");
    await user.click(screen.getByRole("button", { name: "Send reset instructions" }));

    expect(await screen.findByRole("alert")).toHaveTextContent("couldn't reach the server");
    expect(screen.queryByText(/Database unavailable/)).not.toBeInTheDocument();
  });

  it("shows a retry message when the network is unavailable", async () => {
    const user = userEvent.setup();
    mockFetch(new TypeError("Failed to fetch"));
    renderApp("/forgot-password");

    await user.type(screen.getByLabelText("Email"), "learner@example.com");
    await user.click(screen.getByRole("button", { name: "Send reset instructions" }));

    expect(await screen.findByRole("alert")).toHaveTextContent("couldn't reach the server");
  });

  it("disables the submit button while the request is pending", async () => {
    const user = userEvent.setup();
    let release;
    globalThis.fetch = () => new Promise((resolve) => { release = resolve; });
    renderApp("/forgot-password");

    await user.type(screen.getByLabelText("Email"), "learner@example.com");
    await user.click(screen.getByRole("button", { name: "Send reset instructions" }));

    const busy = await screen.findByRole("button", { name: "Sending…" });
    expect(busy).toBeDisabled();
    release(jsonResponse(200, {}));
    expect(await screen.findByRole("status")).toBeInTheDocument();
  });
});
