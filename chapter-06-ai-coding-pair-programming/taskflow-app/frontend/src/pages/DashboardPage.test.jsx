import { screen, within } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { beforeEach, describe, expect, it } from "vitest";
import { attemptDetail, jsonResponse, renderApp, routeFetch, signIn } from "../test/helpers.jsx";

beforeEach(() => {
  window.localStorage.clear();
  signIn();
});

// Both requests the Dashboard makes on load; override per test.
const ok = (parts = ["Part 1"], attempts = []) => ({
  "GET /parts": () => jsonResponse(200, parts),
  "GET /attempts": () => jsonResponse(200, attempts),
});

const statCard = (label) => screen.getByText(label).closest(".stat");

describe("Dashboard — Part list (S6b)", () => {
  it("loads the Part list from GET /parts with the bearer token", async () => {
    const fetchMock = routeFetch(ok(["Part 1", "Part 5", "Part 7"]));
    renderApp("/dashboard");

    expect(await screen.findByText("Part 5")).toBeInTheDocument();
    expect(screen.getByText("Part 1")).toBeInTheDocument();
    expect(screen.getByText("Part 7")).toBeInTheDocument();

    const call = fetchMock.mock.calls.find(([url]) => url === "/parts");
    expect(call[1].headers.Authorization).toBe("Bearer jwt-abc");
  });

  it("shows a loading state while the Part list is being fetched", async () => {
    globalThis.fetch = () => new Promise(() => {}); // never resolves
    renderApp("/dashboard");
    expect(screen.getByRole("status")).toHaveTextContent("Loading Parts…");
  });

  it("empty array (no questions yet) shows the empty state, not an error", async () => {
    routeFetch(ok([]));
    renderApp("/dashboard");

    expect(await screen.findByText(/No practice exercises are available yet/)).toBeInTheDocument();
    expect(screen.queryByRole("alert")).not.toBeInTheDocument();
  });

  it("server error: shows an alert, and Try again reloads the list", async () => {
    const user = userEvent.setup();
    let calls = 0;
    routeFetch({
      ...ok(),
      "GET /parts": () => (++calls === 1 ? jsonResponse(500, { title: "Server error" }) : jsonResponse(200, ["Part 2"])),
    });
    renderApp("/dashboard");

    expect(await screen.findByRole("alert")).toHaveTextContent("couldn't load the Part list");
    await user.click(screen.getByRole("button", { name: "Try again" }));

    expect(await screen.findByText("Part 2")).toBeInTheDocument();
    expect(screen.queryByRole("alert")).not.toBeInTheDocument();
  });

  it("network failure on /parts: shows the same alert", async () => {
    routeFetch({ ...ok(), "GET /parts": () => new TypeError("Failed to fetch") });
    renderApp("/dashboard");
    expect(await screen.findByRole("alert")).toHaveTextContent("couldn't load the Part list");
  });

  it("401 (expired/invalid token): ends the session and returns to Login", async () => {
    routeFetch({ ...ok(), "GET /parts": () => jsonResponse(401, { title: "Unauthorized" }) });
    renderApp("/dashboard");

    expect(await screen.findByRole("heading", { name: "Log in" })).toBeInTheDocument();
    expect(window.localStorage.getItem("engcoach.session")).toBeNull();
  });

  it("sidebar: Dashboard is current; Practice and Progress are disabled (later slices)", async () => {
    routeFetch(ok([]));
    renderApp("/dashboard");

    expect(await screen.findByRole("link", { name: "Dashboard" })).toHaveAttribute("aria-current", "page");
    expect(screen.getByRole("button", { name: "Practice" })).toHaveAttribute("aria-disabled", "true");
    expect(screen.getByRole("button", { name: "Progress" })).toHaveAttribute("aria-disabled", "true");
  });

  it("Start on a Part calls POST /attempts and opens the test screen", async () => {
    const user = userEvent.setup();
    const fetchMock = routeFetch({
      ...ok(["Part 5"]),
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
      ...ok(["Part 7"]),
      "POST /attempts": () => jsonResponse(422, { title: "Part has no questions", status: 422 }),
    });
    renderApp("/dashboard");

    await user.click(await screen.findByRole("button", { name: "Start Part 7" }));

    expect(await screen.findByRole("alert")).toHaveTextContent("no questions for this Part yet");
    expect(screen.getByRole("button", { name: "Start Part 7" })).toBeEnabled();
  });
});

describe("Dashboard — stats from GET /attempts (S10a)", () => {
  it("shows the real completed-attempt count and the average percentage", async () => {
    const fetchMock = routeFetch(
      ok(["Part 1"], [
        { id: 3, type: "PART_PRACTICE", part: "Part 1", status: "SUBMITTED", score: 9, totalQuestions: 12 },
        { id: 2, type: "PART_PRACTICE", part: "Part 1", status: "SUBMITTED", score: 5, totalQuestions: 10 },
        { id: 1, type: "PART_PRACTICE", part: "Part 1", status: "SUBMITTED", score: 7, totalQuestions: 10 },
      ]),
    );
    renderApp("/dashboard");

    expect(await within(statCard("Attempts completed")).findByText("3")).toBeInTheDocument();
    expect(within(statCard("Average score")).getByText("65%")).toBeInTheDocument();
    const call = fetchMock.mock.calls.find(([url]) => url === "/attempts");
    expect(call[1].headers.Authorization).toBe("Bearer jwt-abc");
  });

  it("no completed attempts: count is a real 0 and the average is a dash with a hint", async () => {
    routeFetch(ok(["Part 1"], []));
    renderApp("/dashboard");

    expect(await within(statCard("Attempts completed")).findByText("0")).toBeInTheDocument();
    expect(within(statCard("Average score")).getByText("—")).toBeInTheDocument();
    expect(screen.getByText("Complete a practice to see your average score.")).toBeInTheDocument();
  });

  it("shows a placeholder while the stats load", async () => {
    globalThis.fetch = () => new Promise(() => {});
    renderApp("/dashboard");

    expect(within(statCard("Attempts completed")).getByText("…")).toBeInTheDocument();
    expect(within(statCard("Average score")).getByText("…")).toBeInTheDocument();
  });

  it("stats failure shows an alert with Try again, and the Part list still works", async () => {
    const user = userEvent.setup();
    let calls = 0;
    routeFetch({
      ...ok(["Part 4"]),
      "GET /attempts": () =>
        ++calls === 1
          ? jsonResponse(500, { title: "Server error" })
          : jsonResponse(200, [{ id: 1, status: "SUBMITTED", score: 4, totalQuestions: 5 }]),
    });
    renderApp("/dashboard");

    expect(await screen.findByRole("alert")).toHaveTextContent("couldn't load your stats");
    expect(screen.getByText("Part 4")).toBeInTheDocument(); // list unaffected
    expect(within(statCard("Average score")).getByText("—")).toBeInTheDocument();

    await user.click(screen.getByRole("button", { name: "Try again" }));

    expect(await within(statCard("Average score")).findByText("80%")).toBeInTheDocument();
    expect(screen.queryByRole("alert")).not.toBeInTheDocument();
  });

  it("401 from /attempts ends the session", async () => {
    routeFetch({ ...ok(), "GET /attempts": () => jsonResponse(401, { title: "Unauthorized" }) });
    renderApp("/dashboard");

    expect(await screen.findByRole("heading", { name: "Log in" })).toBeInTheDocument();
    expect(window.localStorage.getItem("engcoach.session")).toBeNull();
  });
});
