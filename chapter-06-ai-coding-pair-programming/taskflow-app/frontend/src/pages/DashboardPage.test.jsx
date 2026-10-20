import { screen } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { beforeEach, describe, expect, it } from "vitest";
import { attemptDetail, jsonResponse, mockFetch, renderApp, routeFetch } from "../test/helpers.jsx";

const KEY = "engcoach.session";

function signedIn() {
  window.localStorage.setItem(
    KEY,
    JSON.stringify({ accessToken: "jwt-abc", expiresAt: new Date(Date.now() + 60_000).toISOString() }),
  );
}

beforeEach(() => {
  window.localStorage.clear();
  signedIn();
});

describe("Dashboard (S6b)", () => {
  it("loads the real Part list from GET /parts with the bearer token", async () => {
    const fetchMock = mockFetch(jsonResponse(200, ["Part 1", "Part 5", "Part 7"]));
    renderApp("/dashboard");

    expect(await screen.findByText("Part 5")).toBeInTheDocument();
    expect(screen.getByText("Part 1")).toBeInTheDocument();
    expect(screen.getByText("Part 7")).toBeInTheDocument();

    const [url, init] = fetchMock.mock.calls[0];
    expect(url).toBe("/parts");
    expect(init.headers.Authorization).toBe("Bearer jwt-abc");
  });

  it("shows a loading state while the Part list is being fetched", async () => {
    globalThis.fetch = () => new Promise(() => {}); // never resolves
    renderApp("/dashboard");
    expect(screen.getByRole("status")).toHaveTextContent("Loading Parts…");
  });

  it("empty array (no questions yet) shows the empty state, not an error", async () => {
    mockFetch(jsonResponse(200, []));
    renderApp("/dashboard");

    expect(await screen.findByText(/No practice exercises are available yet/)).toBeInTheDocument();
    expect(screen.queryByRole("alert")).not.toBeInTheDocument();
  });

  it("server error: shows an alert, and Try again reloads the list", async () => {
    const user = userEvent.setup();
    mockFetch(jsonResponse(500, { title: "Server error", status: 500 }), jsonResponse(200, ["Part 2"]));
    renderApp("/dashboard");

    expect(await screen.findByRole("alert")).toHaveTextContent("couldn't load the Part list");

    await user.click(screen.getByRole("button", { name: "Try again" }));

    expect(await screen.findByText("Part 2")).toBeInTheDocument();
    expect(screen.queryByRole("alert")).not.toBeInTheDocument();
  });

  it("network failure: shows the same alert", async () => {
    mockFetch(new TypeError("Failed to fetch"));
    renderApp("/dashboard");
    expect(await screen.findByRole("alert")).toHaveTextContent("couldn't load the Part list");
  });

  it("401 (expired/invalid token): ends the session and returns to Login", async () => {
    mockFetch(jsonResponse(401, { title: "Unauthorized", status: 401 }));
    renderApp("/dashboard");

    expect(await screen.findByRole("heading", { name: "Log in" })).toBeInTheDocument();
    expect(window.localStorage.getItem(KEY)).toBeNull();
  });

  it("stats are shown as unavailable, never as a made-up 0", async () => {
    mockFetch(jsonResponse(200, ["Part 1"]));
    renderApp("/dashboard");

    expect(await screen.findByText(/will appear here once attempt history is available/)).toBeInTheDocument();
    expect(screen.getByText("Average score")).toBeInTheDocument();
    expect(screen.getByText("Attempts")).toBeInTheDocument();
    expect(screen.getAllByText("—")).toHaveLength(2);
    expect(screen.queryByText("0")).not.toBeInTheDocument();
  });

  it("sidebar: Dashboard is the current page; Practice and Progress are disabled (later slices)", async () => {
    mockFetch(jsonResponse(200, []));
    renderApp("/dashboard");

    expect(await screen.findByRole("link", { name: "Dashboard" })).toHaveAttribute("aria-current", "page");
    expect(screen.getByRole("button", { name: "Practice" })).toHaveAttribute("aria-disabled", "true");
    expect(screen.getByRole("button", { name: "Progress" })).toHaveAttribute("aria-disabled", "true");
  });

  it("Start on a Part calls POST /attempts and opens the test screen", async () => {
    const user = userEvent.setup();
    const fetchMock = routeFetch({
      "GET /parts": () => jsonResponse(200, ["Part 5"]),
      "POST /attempts": () => jsonResponse(201, { id: 7, type: "PART_PRACTICE", part: "Part 5", status: "IN_PROGRESS" }),
      "GET /attempts/7": () => jsonResponse(200, attemptDetail()),
    });
    renderApp("/dashboard");

    await user.click(await screen.findByRole("button", { name: "Start Part 5" }));

    expect(await screen.findByRole("heading", { name: "Part 5 practice" })).toBeInTheDocument();
    const post = fetchMock.mock.calls.find(([, i]) => i?.method === "POST");
    expect(JSON.parse(post[1].body)).toEqual({ type: "PART_PRACTICE", part: "Part 5" });
  });

  it("422 on start (Part has no questions) shows a clear message and stays on Dashboard", async () => {
    const user = userEvent.setup();
    routeFetch({
      "GET /parts": () => jsonResponse(200, ["Part 7"]),
      "POST /attempts": () => jsonResponse(422, { title: "Part has no questions", status: 422 }),
    });
    renderApp("/dashboard");

    await user.click(await screen.findByRole("button", { name: "Start Part 7" }));

    expect(await screen.findByRole("alert")).toHaveTextContent("no questions for this Part yet");
    expect(screen.getByRole("button", { name: "Start Part 7" })).toBeEnabled();
  });
});
