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
// After the queue is used up, further calls (e.g. the Dashboard loading
// /parts after a login) get an empty 200 list instead of undefined.
export function mockFetch(...responses) {
  const fn = vi.fn(async () => jsonResponse(200, []));
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

// Route-based fetch mock: { "GET /attempts/7": () => jsonResponse(...) }.
// Handlers are functions so every call gets a fresh Response (a body can only
// be read once). An unmocked route fails the test loudly.
export function routeFetch(routes) {
  const fn = vi.fn(async (url, init = {}) => {
    const key = `${init.method ?? "GET"} ${url}`;
    const handler = routes[key];
    if (!handler) throw new Error(`Unmocked request: ${key}`);
    const result = handler(init);
    if (result instanceof Error) throw result;
    return result;
  });
  globalThis.fetch = fn;
  return fn;
}

export function signIn() {
  window.localStorage.setItem(
    "engcoach.session",
    JSON.stringify({ accessToken: "jwt-abc", expiresAt: new Date(Date.now() + 60_000).toISOString() }),
  );
}

// Test fixture in the shape GET /attempts/{id} returns for `questions`.
export const questionsFixture = () => [
  { id: 101, prompt: "The manager asked everyone ____ the report.", options: ["submit", "to submit", "submitting", "submitted"] },
  { id: 102, prompt: "Our office is located ____ the station.", options: ["near", "nearly", "nearness", "nearer to"] },
  { id: 103, prompt: "Please ____ your badge at the desk.", options: ["leaves", "leaving", "leave", "left"] },
  { id: 104, prompt: "The meeting was postponed ____ the director was ill.", options: ["because", "despite", "so that", "although"] },
  { id: 105, prompt: "____ employees must attend the briefing.", options: ["Every", "All", "Each", "Another"] },
];

export const attemptDetail = (overrides = {}) => ({
  id: 7,
  type: "PART_PRACTICE",
  part: "Part 5",
  status: "IN_PROGRESS",
  startedAt: "2026-10-17T08:00:00Z",
  remainingSeconds: null,
  answers: [],
  questions: questionsFixture(),
  ...overrides,
});
