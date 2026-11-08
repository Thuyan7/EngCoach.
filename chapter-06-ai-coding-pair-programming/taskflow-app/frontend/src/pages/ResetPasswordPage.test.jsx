import { screen } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { beforeEach, describe, expect, it } from "vitest";
import { jsonResponse, mockFetch, renderApp } from "../test/helpers.jsx";

beforeEach(() => window.localStorage.clear());

async function enterPasswords(user, password, confirmation = password) {
  await user.type(screen.getByLabelText("New password"), password);
  await user.type(screen.getByLabelText("Confirm new password"), confirmation);
}

describe("Reset password screen", () => {
  it("requires a token and does not call the API when the link has none", async () => {
    const user = userEvent.setup();
    const fetchMock = mockFetch(jsonResponse(200, {}));
    renderApp("/reset-password");

    await enterPasswords(user, "new-password-123");
    await user.click(screen.getByRole("button", { name: "Update password" }));

    expect(screen.getByRole("alert")).toHaveTextContent("missing its token");
    expect(fetchMock).not.toHaveBeenCalled();
  });

  it("rejects passwords shorter than 8 characters", async () => {
    const user = userEvent.setup();
    const fetchMock = mockFetch(jsonResponse(200, {}));
    renderApp("/reset-password?token=valid-token");

    await enterPasswords(user, "short", "short");
    await user.click(screen.getByRole("button", { name: "Update password" }));

    expect(screen.getByRole("alert")).toHaveTextContent("at least 8 characters");
    expect(fetchMock).not.toHaveBeenCalled();
  });

  it("requires the confirmation to match", async () => {
    const user = userEvent.setup();
    const fetchMock = mockFetch(jsonResponse(200, {}));
    renderApp("/reset-password?token=valid-token");

    await enterPasswords(user, "new-password-123", "different-password");
    await user.click(screen.getByRole("button", { name: "Update password" }));

    expect(screen.getByRole("alert")).toHaveTextContent("passwords do not match");
    expect(fetchMock).not.toHaveBeenCalled();
  });

  it("sends token and new password, then confirms success", async () => {
    const user = userEvent.setup();
    const fetchMock = mockFetch(jsonResponse(200, { message: "Password updated" }));
    renderApp("/reset-password?token=valid-token");

    await enterPasswords(user, "new-password-123");
    await user.click(screen.getByRole("button", { name: "Update password" }));

    expect(await screen.findByRole("status")).toHaveTextContent("Your password has been updated.");
    expect(screen.getByRole("link", { name: "log in" })).toHaveAttribute("href", "/login");
    expect(fetchMock).toHaveBeenCalledTimes(1);
    expect(fetchMock.mock.calls[0][0]).toBe("/auth/reset-password");
    expect(fetchMock.mock.calls[0][1].method).toBe("POST");
    expect(JSON.parse(fetchMock.mock.calls[0][1].body)).toEqual({
      token: "valid-token",
      newPassword: "new-password-123",
    });
    expect(screen.queryByRole("button", { name: "Update password" })).not.toBeInTheDocument();
  });

  it.each([
    [410, "expired or has already been used"],
    [400, "could not be used"],
    [401, "could not be used"],
  ])("handles an invalid or unavailable reset token (%i)", async (status, message) => {
    const user = userEvent.setup();
    mockFetch(jsonResponse(status, { detail: "Sensitive server detail" }));
    renderApp("/reset-password?token=bad-token");

    await enterPasswords(user, "new-password-123");
    await user.click(screen.getByRole("button", { name: "Update password" }));

    expect(await screen.findByRole("alert")).toHaveTextContent(message);
    expect(screen.queryByText("Sensitive server detail")).not.toBeInTheDocument();
  });

  it("shows the API validation detail for a rejected new password", async () => {
    const user = userEvent.setup();
    mockFetch(jsonResponse(422, { detail: "The password cannot contain your email address." }));
    renderApp("/reset-password?token=valid-token");

    await enterPasswords(user, "new-password-123");
    await user.click(screen.getByRole("button", { name: "Update password" }));

    expect(await screen.findByRole("alert")).toHaveTextContent(
      "The password cannot contain your email address.",
    );
  });

  it("shows a retry message when the server cannot be reached", async () => {
    const user = userEvent.setup();
    mockFetch(new TypeError("Failed to fetch"));
    renderApp("/reset-password?token=valid-token");

    await enterPasswords(user, "new-password-123");
    await user.click(screen.getByRole("button", { name: "Update password" }));

    expect(await screen.findByRole("alert")).toHaveTextContent("could not be used");
  });

  it("disables the submit button while the reset request is pending", async () => {
    const user = userEvent.setup();
    let release;
    globalThis.fetch = () => new Promise((resolve) => { release = resolve; });
    renderApp("/reset-password?token=valid-token");

    await enterPasswords(user, "new-password-123");
    await user.click(screen.getByRole("button", { name: "Update password" }));

    const busy = await screen.findByRole("button", { name: "Updating…" });
    expect(busy).toBeDisabled();
    release(jsonResponse(200, {}));
    expect(await screen.findByRole("status")).toBeInTheDocument();
  });
});
