import { beforeEach, describe, expect, it } from "vitest";
import { screen, within } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { jsonResponse, renderApp, routeFetch, signIn } from "../test/helpers.jsx";

beforeEach(() => {
  window.localStorage.clear();
  signIn();
});

const attempts = [
  { id: 12, type: "PART_PRACTICE", part: "Part 5", status: "SUBMITTED", score: 8, totalQuestions: 10, startedAt: "2026-10-20T08:00:00Z" },
  { id: 11, type: "MOCK_TEST", part: null, status: "SUBMITTED", score: 150, totalQuestions: 200, startedAt: "2026-10-19T08:00:00Z" },
];

describe("Progress screen (S10b)", () => {
  it("loads real attempt history and renders the summary, chart, and rows", async () => {
    const fetchMock = routeFetch({ "GET /attempts": () => jsonResponse(200, attempts) });
    renderApp("/progress");

    expect(await screen.findByRole("heading", { name: "Your progress" })).toBeInTheDocument();
    expect(screen.getByText("2")).toBeInTheDocument();
    expect(screen.getByText("78%")).toBeInTheDocument(); // mean of 80% and 75% is 77.5%, rounded
    expect(screen.getByText("Part 5")).toBeInTheDocument();
    expect(screen.getByText("Full mock test")).toBeInTheDocument();
    expect(screen.getByRole("img", { name: /Accuracy trend/ })).toBeInTheDocument();
    expect(fetchMock.mock.calls[0][1].headers.Authorization).toBe("Bearer jwt-abc");
  });

  it("shows a helpful empty state when there are no completed attempts", async () => {
    routeFetch({ "GET /attempts": () => jsonResponse(200, []) });
    renderApp("/progress");

    expect(await screen.findByText(/You haven't completed a practice attempt yet/)).toBeInTheDocument();
    expect(screen.getByRole("link", { name: "Start your first practice" })).toHaveAttribute("href", "/dashboard");
  });

  it("shows an error and retries loading history", async () => {
    const user = userEvent.setup();
    let calls = 0;
    routeFetch({
      "GET /attempts": () => ++calls === 1 ? jsonResponse(500, { title: "Server error" }) : jsonResponse(200, attempts),
    });
    renderApp("/progress");

    expect(await screen.findByRole("alert")).toHaveTextContent("couldn't load your attempt history");
    await user.click(screen.getByRole("button", { name: "Try again" }));
    expect(await screen.findByText("Part 5")).toBeInTheDocument();
  });

  it("deletes only after confirmation, then removes the row and updates the summary", async () => {
    const user = userEvent.setup();
    const fetchMock = routeFetch({
      "GET /attempts": () => jsonResponse(200, attempts),
      "DELETE /attempts/12": () => jsonResponse(204),
    });
    renderApp("/progress");

    await user.click(await screen.findByRole("button", { name: /Delete attempt: Part 5/ }));
    expect(screen.getByRole("dialog", { name: "Delete this attempt?" })).toBeInTheDocument();
    expect(fetchMock.mock.calls.some(([, init]) => init?.method === "DELETE")).toBe(false);

    await user.click(screen.getByRole("button", { name: "Delete attempt" }));
    expect(await screen.findByText("Full mock test")).toBeInTheDocument();
    expect(screen.queryByRole("button", { name: /Delete attempt: Part 5/ })).not.toBeInTheDocument();
    expect(screen.getByText("1 attempt")).toBeInTheDocument();
    expect(fetchMock.mock.calls.find(([, init]) => init?.method === "DELETE")[0]).toBe("/attempts/12");
  });

  it("delete failure keeps the row and lets the user retry", async () => {
    const user = userEvent.setup();
    routeFetch({
      "GET /attempts": () => jsonResponse(200, attempts),
      "DELETE /attempts/12": () => jsonResponse(500, { title: "Server error" }),
    });
    renderApp("/progress");

    await user.click(await screen.findByRole("button", { name: /Delete attempt: Part 5/ }));
    await user.click(screen.getByRole("button", { name: "Delete attempt" }));
    expect(await screen.findByRole("alert")).toHaveTextContent("couldn't delete this attempt");
    expect(screen.getByText("Part 5")).toBeInTheDocument();
    expect(screen.getByRole("button", { name: "Delete attempt" })).toBeEnabled();
  });

  it("401 ends the session", async () => {
    routeFetch({ "GET /attempts": () => jsonResponse(401, { title: "Unauthorized" }) });
    renderApp("/progress");

    expect(await screen.findByRole("heading", { name: "Log in" })).toBeInTheDocument();
    expect(window.localStorage.getItem("engcoach.session")).toBeNull();
  });

  it("the sidebar Progress link is active and keyboard navigable", async () => {
    routeFetch({ "GET /attempts": () => jsonResponse(200, []) });
    renderApp("/progress");

    expect(await screen.findByRole("link", { name: "Progress" })).toHaveAttribute("aria-current", "page");
  });
});
