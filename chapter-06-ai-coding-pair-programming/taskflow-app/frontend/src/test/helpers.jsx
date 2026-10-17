import { render } from "@testing-library/react";
import { MemoryRouter } from "react-router-dom";
import { vi } from "vitest";
import App from "../App.jsx";
import { AuthProvider } from "../auth/AuthContext.jsx";

export function renderApp(path = "/login") {
  return render(
    <MemoryRouter initialEntries={[path]} future={{ v7_startTransition: true, v7_relativeSplatPath: true }}>
      <AuthProvider>
        <App />
      </AuthProvider>
    </MemoryRouter>,
  );
}

export function jsonResponse(status, body) {
  return new Response(body === undefined ? null : JSON.stringify(body), {
    status,
    headers: { "Content-Type": status >= 400 ? "application/problem+json" : "application/json" },
  });
}

// Queue of canned responses; each fetch call consumes the next one.
export function mockFetch(...responses) {
  const fn = vi.fn();
  responses.forEach((r) =>
    r instanceof Error ? fn.mockRejectedValueOnce(r) : fn.mockResolvedValueOnce(r),
  );
  globalThis.fetch = fn;
  return fn;
}

export const AUTH_OK = {
  accessToken: "jwt-from-server",
  tokenType: "Bearer",
  expiresAt: new Date(Date.now() + 3 * 60 * 60 * 1000).toISOString(),
};
