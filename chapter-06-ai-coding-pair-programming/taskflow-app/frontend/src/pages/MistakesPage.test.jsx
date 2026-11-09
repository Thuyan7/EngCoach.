import { beforeEach, describe, expect, it } from "vitest";
import { screen } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { jsonResponse, renderApp, routeFetch, signIn } from "../test/helpers.jsx";

beforeEach(() => {
  window.localStorage.clear();
  signIn();
});

const mistake = (id, part, prompt, extra = {}) => ({
  attemptId: 3,
  answeredAt: "2026-11-01T08:00:00Z",
  part,
  chosenOption: "nearly",
  explanationText: "Use the preposition near.",
  question: { id, prompt, options: ["near", "nearly", "nearness", "nearer to"], correctOption: "near", ...extra },
});

const everything = {
  total: 2,
  partCounts: [{ part: "Part 5", count: 1 }, { part: "Part 7", count: 1 }],
  mistakes: [mistake(1, "Part 5", "Our office is ____ the station."), mistake(2, "Part 7", "What is the notice about?", { groupContent: "Notice: the lift is closed." })],
};

describe("Review your mistakes", () => {
  it("lists wrong questions with the correct answer, your answer and the explanation", async () => {
    routeFetch({ "GET /attempts/mistakes": () => jsonResponse(200, everything) });
    renderApp("/mistakes");

    expect(await screen.findByText("Our office is ____ the station.")).toBeInTheDocument();
    expect(screen.getAllByText(/A\. near \(correct answer\)/).length).toBe(2);
    expect(screen.getAllByText(/B\. nearly \(your answer\)/).length).toBe(2);
    expect(screen.getAllByText("Use the preposition near.").length).toBe(2);
    expect(screen.getByText("Notice: the lift is closed.")).toBeInTheDocument();
  });

  it("filters by Part through the API", async () => {
    const fetchMock = routeFetch({
      "GET /attempts/mistakes": () => jsonResponse(200, everything),
      "GET /attempts/mistakes?part=Part%205": () => jsonResponse(200, { ...everything, mistakes: [everything.mistakes[0]] }),
    });
    renderApp("/mistakes");

    await userEvent.click(await screen.findByRole("button", { name: "Part 5 (1)" }));
    expect(await screen.findByRole("button", { name: "Part 5 (1)" })).toHaveAttribute("aria-pressed", "true");
    expect(screen.queryByText("What is the notice about?")).not.toBeInTheDocument();
    expect(fetchMock.mock.calls.some(([url]) => url === "/attempts/mistakes?part=Part%205")).toBe(true);
  });

  it("shows an empty state when there are no mistakes", async () => {
    routeFetch({ "GET /attempts/mistakes": () => jsonResponse(200, { total: 0, partCounts: [], mistakes: [] }) });
    renderApp("/mistakes");
    expect(await screen.findByText("You have no mistakes to review.")).toBeInTheDocument();
  });

  it("shows an error with a retry button when loading fails", async () => {
    routeFetch({ "GET /attempts/mistakes": () => jsonResponse(500, { title: "boom" }) });
    renderApp("/mistakes");
    expect(await screen.findByRole("alert")).toHaveTextContent("couldn't load your mistakes");
    expect(screen.getByRole("button", { name: "Try again" })).toBeInTheDocument();
  });
});
