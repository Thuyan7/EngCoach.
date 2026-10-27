import { screen } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { beforeEach, describe, expect, it } from "vitest";
import { attemptDetail, jsonResponse, renderApp, routeFetch, signIn } from "../test/helpers.jsx";

beforeEach(() => {
  window.localStorage.clear();
  signIn();
});

const submitted = (overrides = {}) => attemptDetail({ status: "SUBMITTED", ...overrides });

// Questions 101..105 (fixture). Result: 101 correct, 102 wrong+explained,
// 103 wrong+explanation unavailable, 104 correct, 105 not answered.
const gradedResult = () => ({
  id: 7,
  status: "SUBMITTED",
  score: 2,
  totalQuestions: 5,
  answers: [
    { questionId: 101, chosenOption: "to submit", isCorrect: true, explanationStatus: null, explanationText: null },
    { questionId: 102, chosenOption: "nearly", isCorrect: false, explanationStatus: "OK", explanationText: "Use the preposition 'near' here." },
    { questionId: 103, chosenOption: "leaves", isCorrect: false, explanationStatus: "UNAVAILABLE", explanationText: null },
    { questionId: 104, chosenOption: "because", isCorrect: true, explanationStatus: null, explanationText: null },
  ],
});

describe("Result screen (S9)", () => {
  it("shows the real score, counts and percentage from the submit response", async () => {
    routeFetch({ "GET /attempts/7": () => jsonResponse(200, submitted()) });
    renderApp("/result/7", { result: gradedResult() });

    expect(await screen.findByRole("heading", { name: "Part 5 result" })).toBeInTheDocument();
    expect(screen.getByText("2 / 5")).toBeInTheDocument();
    expect(screen.getByText("40% correct")).toBeInTheDocument();
    expect(screen.getByText("Correct").nextSibling).toHaveTextContent("2");
    expect(screen.getByText("Incorrect").nextSibling).toHaveTextContent("3");
  });

  it("arriving from the test screen does not grade again", async () => {
    const fetchMock = routeFetch({ "GET /attempts/7": () => jsonResponse(200, submitted()) });
    renderApp("/result/7", { result: gradedResult() });

    await screen.findByText("2 / 5");
    expect(fetchMock.mock.calls.some(([, i]) => i?.method === "POST")).toBe(false);
  });

  it("lists every mistake with the question, what was chosen, and the explanation", async () => {
    routeFetch({ "GET /attempts/7": () => jsonResponse(200, submitted()) });
    renderApp("/result/7", { result: gradedResult() });

    expect(await screen.findByText("Question 2 — Incorrect (you chose B. nearly)")).toBeInTheDocument();
    expect(screen.getByText(/Our office is located/)).toBeInTheDocument();
    expect(screen.getByText("Use the preposition 'near' here.")).toBeInTheDocument();
    // correct answers are not listed
    expect(screen.queryByText(/Question 1 —/)).not.toBeInTheDocument();
    expect(screen.queryByText(/Question 4 —/)).not.toBeInTheDocument();
  });

  it("an unanswered question is listed as 'Not answered' with no explanation or retry", async () => {
    routeFetch({ "GET /attempts/7": () => jsonResponse(200, submitted()) });
    renderApp("/result/7", { result: gradedResult() });

    const card = (await screen.findByText("Question 5 — Not answered")).closest(".explain-card");
    expect(card).not.toHaveTextContent("Retry");
  });

  it("an UNAVAILABLE explanation offers Retry, which calls the retry endpoint and fills it in", async () => {
    const user = userEvent.setup();
    const fetchMock = routeFetch({
      "GET /attempts/7": () => jsonResponse(200, submitted()),
      "POST /attempts/7/answers/103/retry-explanation": () =>
        jsonResponse(200, { questionId: 103, explanationStatus: "OK", explanationText: "The verb must be in base form." }),
    });
    renderApp("/result/7", { result: gradedResult() });

    await user.click(await screen.findByRole("button", { name: "Retry" }));

    expect(await screen.findByText("The verb must be in base form.")).toBeInTheDocument();
    expect(screen.queryByRole("button", { name: "Retry" })).not.toBeInTheDocument();
    expect(screen.getByText("2 / 5")).toBeInTheDocument(); // score untouched
    expect(fetchMock.mock.calls.some(([url]) => url === "/attempts/7/submit")).toBe(false);
  });

  it("a retry that is still UNAVAILABLE says so and keeps the Retry button", async () => {
    const user = userEvent.setup();
    routeFetch({
      "GET /attempts/7": () => jsonResponse(200, submitted()),
      "POST /attempts/7/answers/103/retry-explanation": () =>
        jsonResponse(200, { questionId: 103, explanationStatus: "UNAVAILABLE", explanationText: null }),
    });
    renderApp("/result/7", { result: gradedResult() });

    await user.click(await screen.findByRole("button", { name: "Retry" }));

    expect(await screen.findByRole("alert")).toHaveTextContent("Still unavailable");
    expect(screen.getByRole("button", { name: "Retry" })).toBeEnabled();
  });

  it("a failed retry request shows the same message", async () => {
    const user = userEvent.setup();
    routeFetch({
      "GET /attempts/7": () => jsonResponse(200, submitted()),
      "POST /attempts/7/answers/103/retry-explanation": () => jsonResponse(500, { title: "x" }),
    });
    renderApp("/result/7", { result: gradedResult() });

    await user.click(await screen.findByRole("button", { name: "Retry" }));
    expect(await screen.findByRole("alert")).toHaveTextContent("Still unavailable");
  });

  it("after a refresh (no navigation state) it rebuilds the result from the idempotent submit endpoint", async () => {
    const fetchMock = routeFetch({
      "GET /attempts/7": () => jsonResponse(200, submitted()),
      "POST /attempts/7/submit": () => jsonResponse(200, gradedResult()),
    });
    renderApp("/result/7");

    expect(await screen.findByText("2 / 5")).toBeInTheDocument();
    expect(fetchMock.mock.calls.filter(([url]) => url === "/attempts/7/submit")).toHaveLength(1);
  });

  it("an attempt that is not submitted is sent back to the test screen and is never graded", async () => {
    const fetchMock = routeFetch({ "GET /attempts/7": () => jsonResponse(200, attemptDetail()) });
    renderApp("/result/7");

    expect(await screen.findByRole("heading", { name: "Part 5 practice" })).toBeInTheDocument();
    expect(fetchMock.mock.calls.some(([url]) => url === "/attempts/7/submit")).toBe(false);
  });

  it("a perfect score shows a no-mistakes message", async () => {
    routeFetch({ "GET /attempts/7": () => jsonResponse(200, submitted()) });
    renderApp("/result/7", {
      result: {
        id: 7,
        status: "SUBMITTED",
        score: 5,
        totalQuestions: 5,
        answers: [101, 102, 103, 104, 105].map((id) => ({
          questionId: id, chosenOption: "x", isCorrect: true, explanationStatus: null, explanationText: null,
        })),
      },
    });

    expect(await screen.findByText("No mistakes — every answer was correct.")).toBeInTheDocument();
  });

  it("a mock test result has the mock-test title", async () => {
    routeFetch({ "GET /attempts/7": () => jsonResponse(200, submitted({ type: "MOCK_TEST", part: null })) });
    renderApp("/result/7", { result: gradedResult() });
    expect(await screen.findByRole("heading", { name: "Mock test result" })).toBeInTheDocument();
  });

  it.each([403, 404])("%i: shows 'not available' with a way back", async (status) => {
    routeFetch({ "GET /attempts/7": () => jsonResponse(status, { title: "x" }) });
    renderApp("/result/7");

    expect(await screen.findByText("This result isn't available.")).toBeInTheDocument();
    expect(screen.getByRole("link", { name: "Back to Dashboard" })).toBeInTheDocument();
  });

  it("load failure: alert and Try again", async () => {
    const user = userEvent.setup();
    let calls = 0;
    routeFetch({
      "GET /attempts/7": () => (++calls === 1 ? jsonResponse(500, { title: "x" }) : jsonResponse(200, submitted())),
      "POST /attempts/7/submit": () => jsonResponse(200, gradedResult()),
    });
    renderApp("/result/7");

    expect(await screen.findByRole("alert")).toHaveTextContent("couldn't load your result");
    await user.click(screen.getByRole("button", { name: "Try again" }));
    expect(await screen.findByText("2 / 5")).toBeInTheDocument();
  });

  it("401 ends the session", async () => {
    routeFetch({ "GET /attempts/7": () => jsonResponse(401, { title: "x" }) });
    renderApp("/result/7");
    expect(await screen.findByRole("heading", { name: "Log in" })).toBeInTheDocument();
    expect(window.localStorage.getItem("engcoach.session")).toBeNull();
  });
});
