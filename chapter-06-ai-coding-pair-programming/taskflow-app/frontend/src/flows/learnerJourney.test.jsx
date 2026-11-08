import { screen } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { beforeEach, describe, expect, it } from "vitest";
import { AUTH_OK, jsonResponse, renderApp, routeFetch } from "../test/helpers.jsx";

beforeEach(() => {
  window.localStorage.clear();
});

describe("learner journey integration", () => {
  it("logs in, starts Part practice, saves an answer, submits, and reviews the correct answer", async () => {
    const user = userEvent.setup();
    let submitted = false;
    const question = {
      id: 501,
      prompt: "The manager asked everyone ____ the report.",
      options: ["submit", "to submit", "submitting", "submitted"],
    };
    const fetchMock = routeFetch({
      "POST /auth/login": () => jsonResponse(200, AUTH_OK),
      "GET /parts": () => jsonResponse(200, ["Part 5"]),
      "GET /attempts": () => jsonResponse(200, []),
      "POST /attempts": () => jsonResponse(201, {
        id: 77,
        type: "PART_PRACTICE",
        part: "Part 5",
        status: "IN_PROGRESS",
        startedAt: "2026-11-06T08:00:00Z",
      }),
      "GET /attempts/77": () => jsonResponse(200, {
        id: 77,
        type: "PART_PRACTICE",
        part: "Part 5",
        status: submitted ? "SUBMITTED" : "IN_PROGRESS",
        startedAt: "2026-11-06T08:00:00Z",
        remainingSeconds: null,
        answers: submitted ? [{ questionId: 501, chosenOption: "to submit" }] : [],
        questions: [{
          ...question,
          ...(submitted ? { correctOption: "to submit" } : {}),
        }],
      }),
      "PUT /attempts/77/answers/501": () =>
        jsonResponse(200, { questionId: 501, chosenOption: "to submit" }),
      "POST /attempts/77/submit": () => {
        submitted = true;
        return jsonResponse(200, {
          id: 77,
          status: "SUBMITTED",
          score: 1,
          totalQuestions: 1,
          answers: [{
            questionId: 501,
            chosenOption: "to submit",
            isCorrect: true,
            explanationStatus: "OK",
            explanationText: "Use the infinitive after ask someone.",
          }],
        });
      },
    });

    renderApp("/login");

    await user.type(screen.getByLabelText("Email"), "learner@example.com");
    await user.type(screen.getByLabelText("Password"), "correct-horse-battery");
    await user.click(screen.getByRole("button", { name: "Log in" }));

    expect(await screen.findByRole("heading", { name: /Make every practice/ })).toBeInTheDocument();
    expect(await screen.findByRole("button", { name: /^Part 5/ })).toBeInTheDocument();

    await user.click(screen.getByRole("button", { name: /^Part 5/ }));
    expect(await screen.findByRole("heading", { name: "Part 5 practice" })).toBeInTheDocument();

    await user.click(screen.getByRole("radio", { name: /to submit/ }));
    expect(await screen.findByText("Saved")).toBeInTheDocument();

    await user.click(screen.getByRole("button", { name: "Submit for grading" }));
    expect(await screen.findByRole("heading", { name: "Part 5 result" })).toBeInTheDocument();

    expect(screen.getByText("1 / 1")).toBeInTheDocument();
    expect(screen.getByText(/Correct answer:/).parentElement).toHaveTextContent("Correct answer: B. to submit");
    expect(screen.getByText(/Your answer:/).parentElement).toHaveTextContent("Your answer: B. to submit");
    expect(fetchMock.mock.calls.some(([url, init]) => url === "/attempts/77/submit" && init?.method === "POST")).toBe(true);
    expect(JSON.parse(window.localStorage.getItem("engcoach.session")).accessToken).toBe(
      "jwt-from-server",
    );
  });
});
