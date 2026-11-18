import { beforeEach, describe, expect, it } from "vitest";
import { screen } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { jsonResponse, renderApp, routeFetch, signIn } from "../test/helpers.jsx";

beforeEach(() => {
  window.localStorage.clear();
  signIn();
});

// The API helper sends the learner's time zone, so the mocked URLs carry it too.
const ZONE = encodeURIComponent(Intl.DateTimeFormat().resolvedOptions().timeZone);
const STREAK_URL = `/streak?timeZone=${ZONE}`;
const monthUrl = (month) => `/streak?timeZone=${ZONE}&month=${month}`;

const november = (extra = {}) => ({
  currentStreak: 3,
  longestStreak: 5,
  studiedToday: true,
  today: "2026-11-18",
  month: "2026-11",
  studyDays: ["2026-11-16", "2026-11-17", "2026-11-18"],
  ...extra,
});

// The Home page also loads Tests, Parts, attempts and mistakes; empty answers are enough for these tests.
const home = (routes) => ({
  "GET /tests": () => jsonResponse(200, []),
  "GET /parts": () => jsonResponse(200, []),
  "GET /attempts": () => jsonResponse(200, []),
  "GET /attempts/mistakes": () => jsonResponse(200, { total: 0, partCounts: [], mistakes: [] }),
  ...routes,
});

describe("Study streak calendar", () => {
  it("shows the streak and marks the studied days and today on the calendar", async () => {
    routeFetch(home({ [`GET ${STREAK_URL}`]: () => jsonResponse(200, november()) }));
    renderApp("/dashboard");

    expect(await screen.findByText("3 days")).toBeInTheDocument();
    expect(screen.getByText("Longest: 5 days")).toBeInTheDocument();
    expect(screen.getByText("You studied today. Keep it going tomorrow.")).toBeInTheDocument();
    expect(screen.getByText("November 2026")).toBeInTheDocument();

    expect(screen.getByLabelText("16 November 2026, studied")).toHaveClass("studied");
    expect(screen.getByLabelText("17 November 2026, studied")).toHaveClass("studied");
    const today = screen.getByLabelText("18 November 2026, studied, today");
    expect(today).toHaveClass("studied", "today");
    expect(screen.getByLabelText("15 November 2026")).not.toHaveClass("studied");
    expect(screen.getAllByRole("listitem").filter((li) => li.classList.contains("cal-day")).length).toBe(30);
  });

  it("reminds the learner to keep a streak alive, or to start one", async () => {
    routeFetch(home({ [`GET ${STREAK_URL}`]: () => jsonResponse(200, november({ studiedToday: false, studyDays: ["2026-11-16", "2026-11-17"], currentStreak: 2 })) }));
    const first = renderApp("/dashboard");
    expect(await screen.findByText("Answer a question today to keep your streak alive.")).toBeInTheDocument();
    first.unmount();

    routeFetch(home({ [`GET ${STREAK_URL}`]: () => jsonResponse(200, november({ studiedToday: false, studyDays: [], currentStreak: 0, longestStreak: 0 })) }));
    renderApp("/dashboard");
    expect(await screen.findByText("Answer a question today to start a streak.")).toBeInTheDocument();
    expect(screen.getByText("0 days")).toBeInTheDocument();
  });

  it("browses to the previous month and back; the next arrow is off on the current month", async () => {
    const user = userEvent.setup();
    routeFetch(home({
      [`GET ${STREAK_URL}`]: () => jsonResponse(200, november()),
      [`GET ${monthUrl("2026-10")}`]: () => jsonResponse(200, november({ month: "2026-10", studyDays: ["2026-10-30", "2026-10-31"] })),
      [`GET ${monthUrl("2026-11")}`]: () => jsonResponse(200, november()),
    }));
    renderApp("/dashboard");

    expect(await screen.findByText("November 2026")).toBeInTheDocument();
    expect(screen.getByRole("button", { name: "Next month" })).toBeDisabled();

    await user.click(screen.getByRole("button", { name: "Previous month" }));
    expect(await screen.findByText("October 2026")).toBeInTheDocument();
    expect(screen.getByLabelText("31 October 2026, studied")).toHaveClass("studied");
    expect(screen.getByRole("button", { name: "Next month" })).toBeEnabled();

    await user.click(screen.getByRole("button", { name: "Next month" }));
    expect(await screen.findByText("November 2026")).toBeInTheDocument();
    expect(screen.getByRole("button", { name: "Next month" })).toBeDisabled();
  });

  it("hides the card when the streak cannot be loaded", async () => {
    routeFetch(home({ [`GET ${STREAK_URL}`]: () => new TypeError("Failed to fetch") }));
    renderApp("/dashboard");

    expect(await screen.findByText("Quick access")).toBeInTheDocument();
    expect(screen.queryByText("Study streak")).not.toBeInTheDocument();
  });
});
