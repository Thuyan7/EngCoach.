import { beforeEach, describe, expect, it } from "vitest";
import { screen } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { jsonResponse, renderApp, routeFetch, signIn } from "../test/helpers.jsx";

beforeEach(() => {
  window.localStorage.clear();
  signIn();
});

// The API helper sends the learner's time zone, so the mocked URL carries it too.
const GOALS_URL = `/goals?timeZone=${encodeURIComponent(Intl.DateTimeFormat().resolvedOptions().timeZone)}`;

const goals = (extra = {}) => ({
  targetScore: 700,
  dailyQuestionGoal: 20,
  answeredToday: 12,
  dailyGoalMet: false,
  currentStreak: 3,
  longestStreak: 5,
  estimatedScore: 620,
  ...extra,
});

// The Home page also loads Tests, Parts and attempts; empty answers are enough for these tests.
const home = (goalsHandler) => ({
  "GET /tests": () => jsonResponse(200, []),
  "GET /parts": () => jsonResponse(200, []),
  "GET /attempts": () => jsonResponse(200, []),
  "GET /attempts/mistakes": () => jsonResponse(200, { total: 0, partCounts: [], mistakes: [] }),
  [`GET ${GOALS_URL}`]: goalsHandler,
});

describe("Study goals card", () => {
  it("shows today's progress, the streak and the distance to the target score", async () => {
    routeFetch(home(() => jsonResponse(200, goals())));
    renderApp("/dashboard");

    expect(await screen.findByText("12 / 20")).toBeInTheDocument();
    expect(screen.getByText("3 days")).toBeInTheDocument();
    expect(screen.getByText("Longest: 5 days")).toBeInTheDocument();
    expect(screen.getByText("Estimated 620, 80 points to your target of 700.")).toBeInTheDocument();
  });

  it("says so when the daily goal is reached and when the target is reached", async () => {
    routeFetch(home(() => jsonResponse(200, goals({ answeredToday: 20, dailyGoalMet: true, estimatedScore: 720, currentStreak: 1 }))));
    renderApp("/dashboard");

    expect(await screen.findByText("Daily goal reached.")).toBeInTheDocument();
    expect(screen.getByText("1 day")).toBeInTheDocument();
    expect(screen.getByText("Estimated 720, you have reached your target of 700.")).toBeInTheDocument();
  });

  it("explains how to get an estimate, and invites to set a target when there is none", async () => {
    routeFetch(home(() => jsonResponse(200, goals({ estimatedScore: null }))));
    const first = renderApp("/dashboard");
    expect(await screen.findByText(/Finish a Test with Listening and Reading/)).toBeInTheDocument();
    first.unmount();

    routeFetch(home(() => jsonResponse(200, goals({ targetScore: null, estimatedScore: null }))));
    renderApp("/dashboard");
    expect(await screen.findByText("Not set")).toBeInTheDocument();
    expect(screen.getByText(/Set one to track your progress/)).toBeInTheDocument();
  });

  it("saves edited goals and shows the new values", async () => {
    const user = userEvent.setup();
    const fetchMock = routeFetch({
      ...home(() => jsonResponse(200, goals())),
      [`PUT ${GOALS_URL}`]: () => jsonResponse(200, goals({ targetScore: 800, dailyQuestionGoal: 30, answeredToday: 12 })),
    });
    renderApp("/dashboard");

    await user.click(await screen.findByRole("button", { name: "Edit goals" }));
    const target = screen.getByLabelText(/Target TOEIC score/);
    const daily = screen.getByLabelText("Questions per day");
    await user.clear(target);
    await user.type(target, "800");
    await user.clear(daily);
    await user.type(daily, "30");
    await user.click(screen.getByRole("button", { name: "Save goals" }));

    expect(await screen.findByText("12 / 30")).toBeInTheDocument();
    expect(screen.queryByRole("button", { name: "Save goals" })).not.toBeInTheDocument();
    const put = fetchMock.mock.calls.find(([url, init]) => String(url) === GOALS_URL && init?.method === "PUT");
    expect(JSON.parse(put[1].body)).toEqual({ targetScore: 800, dailyQuestionGoal: 30 });
  });

  it("rejects invalid values without calling the server, and an empty target clears it", async () => {
    const user = userEvent.setup();
    const fetchMock = routeFetch({
      ...home(() => jsonResponse(200, goals())),
      [`PUT ${GOALS_URL}`]: () => jsonResponse(200, goals({ targetScore: null })),
    });
    renderApp("/dashboard");

    await user.click(await screen.findByRole("button", { name: "Edit goals" }));
    await user.clear(screen.getByLabelText("Questions per day"));
    await user.type(screen.getByLabelText("Questions per day"), "0");
    await user.click(screen.getByRole("button", { name: "Save goals" }));
    expect(await screen.findByRole("alert")).toHaveTextContent("from 1 to 500");
    expect(fetchMock.mock.calls.some(([url, init]) => String(url) === GOALS_URL && init?.method === "PUT")).toBe(false);

    await user.clear(screen.getByLabelText("Questions per day"));
    await user.type(screen.getByLabelText("Questions per day"), "20");
    await user.clear(screen.getByLabelText(/Target TOEIC score/));
    await user.click(screen.getByRole("button", { name: "Save goals" }));
    expect(await screen.findByText("Not set")).toBeInTheDocument();
    const put = fetchMock.mock.calls.find(([url, init]) => String(url) === GOALS_URL && init?.method === "PUT");
    expect(JSON.parse(put[1].body)).toEqual({ targetScore: null, dailyQuestionGoal: 20 });
  });

  it("keeps the form open with an error when saving fails", async () => {
    const user = userEvent.setup();
    routeFetch({ ...home(() => jsonResponse(200, goals())), [`PUT ${GOALS_URL}`]: () => jsonResponse(500, { title: "boom" }) });
    renderApp("/dashboard");

    await user.click(await screen.findByRole("button", { name: "Edit goals" }));
    await user.click(screen.getByRole("button", { name: "Save goals" }));
    expect(await screen.findByRole("alert")).toHaveTextContent("couldn't save your goals");
    expect(screen.getByRole("button", { name: "Save goals" })).toBeInTheDocument();
  });

  it("hides the card when the goals cannot be loaded", async () => {
    routeFetch(home(() => new TypeError("Failed to fetch")));
    renderApp("/dashboard");

    expect(await screen.findByText("Quick access")).toBeInTheDocument();
    expect(screen.queryByText("Your study goals")).not.toBeInTheDocument();
  });
});
