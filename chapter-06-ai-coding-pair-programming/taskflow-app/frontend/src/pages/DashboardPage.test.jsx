import { screen, waitFor, within } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { beforeEach, describe, expect, it } from "vitest";
import { attemptDetail, jsonResponse, renderApp, routeFetch, signIn } from "../test/helpers.jsx";

beforeEach(() => {
  window.localStorage.clear();
  signIn();
});

const TEST_ONE = {
  id: 21, title: "TOEIC AI Practice Test 01", description: "d",
  parts: [{ part: "Part 1", questionCount: 6, durationSeconds: 180 }, { part: "Part 5", questionCount: 30, durationSeconds: 600 }],
};
const TEST_TWO = { id: 22, title: "TOEIC AI Practice Test 02", parts: [{ part: "Part 1", questionCount: 6, durationSeconds: 180 }] };

// Both requests the Dashboard makes on load; override per test.
const ok = (tests = [TEST_ONE], attempts = []) => ({
  "GET /tests": () => jsonResponse(200, tests),
  "GET /attempts": () => jsonResponse(200, attempts),
});

const STAT_LABELS = { "Attempts completed": "COMPLETED TESTS", "Average score": "AVERAGE SCORE" };
const statCard = (label) => screen.getByText(STAT_LABELS[label]).closest(".home-stat-card");

describe("Dashboard — Test list", () => {
  it("lists each Test as its own card (not one list of Parts) using GET /tests with the bearer token", async () => {
    const fetchMock = routeFetch(ok([TEST_ONE, TEST_TWO]));
    renderApp("/dashboard");

    expect(await screen.findByText("TOEIC AI Practice Test 01")).toBeInTheDocument();
    expect(screen.getByText("TOEIC AI Practice Test 02")).toBeInTheDocument();
    expect(screen.getByText("2 Parts · 36 questions")).toBeInTheDocument();

    const call = fetchMock.mock.calls.find(([url]) => url === "/tests");
    expect(call[1].headers.Authorization).toBe("Bearer jwt-abc");
  });

  it("shows a loading state while the Tests are being fetched", async () => {
    globalThis.fetch = () => new Promise(() => {}); // never resolves
    renderApp("/dashboard");
    expect(screen.getByText("Loading available Tests…")).toBeInTheDocument();
  });

  it("no Tests yet shows the empty state, not an error", async () => {
    routeFetch(ok([]));
    renderApp("/dashboard");

    expect(await screen.findByText(/No Tests are published yet/)).toBeInTheDocument();
    expect(screen.queryByRole("alert")).not.toBeInTheDocument();
  });

  it("server error: shows an alert, and Try again reloads the list", async () => {
    const user = userEvent.setup();
    let calls = 0;
    routeFetch({
      ...ok(),
      "GET /tests": () => (++calls === 1 ? jsonResponse(500, { title: "Server error" }) : jsonResponse(200, [TEST_TWO])),
    });
    renderApp("/dashboard");

    expect(await screen.findByRole("alert")).toHaveTextContent("couldn't load the Tests");
    await user.click(screen.getByRole("button", { name: "Try again" }));

    expect(await screen.findByText("TOEIC AI Practice Test 02")).toBeInTheDocument();
    expect(screen.queryByRole("alert")).not.toBeInTheDocument();
  });

  it("network failure on /tests: shows the same alert", async () => {
    routeFetch({ ...ok(), "GET /tests": () => new TypeError("Failed to fetch") });
    renderApp("/dashboard");
    expect(await screen.findByRole("alert")).toHaveTextContent("couldn't load the Tests");
  });

  it("401 (expired/invalid token): ends the session and returns to Login", async () => {
    routeFetch({ ...ok(), "GET /tests": () => jsonResponse(401, { title: "Unauthorized" }) });
    renderApp("/dashboard");

    expect(await screen.findByRole("heading", { name: "Log in" })).toBeInTheDocument();
    expect(window.localStorage.getItem("engcoach.session")).toBeNull();
  });

  it("top navigation: Home is current, Practice Tests links to the Test catalog", async () => {
    routeFetch(ok([]));
    renderApp("/dashboard");

    expect(await screen.findByRole("link", { name: "Home" })).toHaveAttribute("aria-current", "page");
    expect(screen.getByRole("link", { name: "Practice Tests" })).toHaveAttribute("href", "/practice");
    expect(screen.getByRole("link", { name: "My Progress" })).toHaveAttribute("href", "/progress");
  });

  it("Start full test creates a TEST attempt for every Part of that Test and opens it", async () => {
    const user = userEvent.setup();
    const fetchMock = routeFetch({
      ...ok([TEST_ONE]),
      "POST /attempts": () => jsonResponse(201, { id: 7, type: "TEST", status: "IN_PROGRESS" }),
      "GET /attempts/7": () => jsonResponse(200, attemptDetail()),
    });
    renderApp("/dashboard");

    await user.click(await screen.findByRole("button", { name: "Start full TOEIC AI Practice Test 01" }));

    const post = await waitFor(() => {
      const call = fetchMock.mock.calls.find(([, i]) => i?.method === "POST");
      expect(call).toBeTruthy();
      return call;
    });
    expect(JSON.parse(post[1].body)).toEqual({ type: "TEST", testId: 21, parts: ["Part 1", "Part 5"] });
  });

  it("Choose Parts opens the Test catalog", async () => {
    const user = userEvent.setup();
    routeFetch({ ...ok([TEST_ONE]), "GET /tests": () => jsonResponse(200, [TEST_ONE]) });
    renderApp("/dashboard");

    await user.click(await screen.findByRole("link", { name: "Choose Parts of TOEIC AI Practice Test 01" }));
    expect(await screen.findByRole("heading", { name: "Practice Tests" })).toBeInTheDocument();
  });

  it("422 on start shows a clear message and stays on Dashboard", async () => {
    const user = userEvent.setup();
    routeFetch({
      ...ok([TEST_ONE]),
      "POST /attempts": () => jsonResponse(422, { title: "no questions", status: 422 }),
    });
    renderApp("/dashboard");

    await user.click(await screen.findByRole("button", { name: "Start full TOEIC AI Practice Test 01" }));

    expect(await screen.findByRole("alert")).toHaveTextContent("no questions for this Test yet");
    expect(screen.getByRole("button", { name: "Start full TOEIC AI Practice Test 01" })).toBeEnabled();
  });
});

describe("Dashboard — stats from GET /attempts (S10a)", () => {
  it("shows the real completed-attempt count and the average percentage", async () => {
    const fetchMock = routeFetch(
      ok([TEST_ONE], [
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
    routeFetch(ok([TEST_ONE], []));
    renderApp("/dashboard");

    expect(await within(statCard("Attempts completed")).findByText("0")).toBeInTheDocument();
    expect(within(statCard("Average score")).getByText("—")).toBeInTheDocument();
  });

  it("shows a placeholder while the stats load", async () => {
    globalThis.fetch = () => new Promise(() => {});
    renderApp("/dashboard");

    expect(within(statCard("Attempts completed")).getByText("…")).toBeInTheDocument();
    expect(within(statCard("Average score")).getByText("…")).toBeInTheDocument();
  });

  it("stats failure shows an alert with Try again, and the Test list still works", async () => {
    const user = userEvent.setup();
    let calls = 0;
    routeFetch({
      ...ok(),
      "GET /attempts": () =>
        ++calls === 1
          ? jsonResponse(500, { title: "Server error" })
          : jsonResponse(200, [{ id: 1, status: "SUBMITTED", score: 4, totalQuestions: 5 }]),
    });
    renderApp("/dashboard");

    expect(await screen.findByRole("alert")).toHaveTextContent("couldn't load your stats");
    expect(screen.getByText("TOEIC AI Practice Test 01")).toBeInTheDocument(); // list unaffected
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
