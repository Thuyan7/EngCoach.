import { screen, waitFor } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { beforeEach, describe, expect, it } from "vitest";
import { attemptDetail, jsonResponse, renderApp, routeFetch, signIn } from "../test/helpers.jsx";

const Q1 = 101; // ids from the questions fixture (test/helpers.jsx)
const Q2 = 102;

beforeEach(() => {
  window.localStorage.clear();
  signIn();
});

function putBodies(fetchMock) {
  return fetchMock.mock.calls
    .filter(([, init]) => init?.method === "PUT")
    .map(([url, init]) => [url, JSON.parse(init.body)]);
}

describe("Test screen (S7b)", () => {
  it("loads the attempt and shows the first question returned by the API", async () => {
    routeFetch({ "GET /attempts/7": () => jsonResponse(200, attemptDetail()) });
    renderApp("/practice/7");

    expect(await screen.findByRole("heading", { name: "Part 5 practice" })).toBeInTheDocument();
    expect(screen.getByText("Question 1 of 5")).toBeInTheDocument();
    expect(screen.getByText("0 of 5 answered")).toBeInTheDocument();
    expect(screen.getByText(/The manager asked everyone/)).toBeInTheDocument();
    expect(screen.getAllByRole("radio")).toHaveLength(4);
    expect(screen.queryByText(/Sample questions/)).not.toBeInTheDocument();
  });

  it("renders an optional reading passage, image, and audio without exposing an answer key", async () => {
    const detail = attemptDetail({
      questions: [{
        id: 101,
        prompt: "What is the notice about?",
        options: ["An office renovation", "A holiday schedule"],
        passage: "NOTICE TO STAFF\nThe second-floor offices will be renovated.",
        imageUrl: "https://example.com/office.jpg",
        audioUrl: "https://example.com/notice.mp3",
      }],
    });
    routeFetch({ "GET /attempts/7": () => jsonResponse(200, detail) });
    const { container } = renderApp("/practice/7");

    expect(await screen.findByText(/The second-floor offices will be renovated/)).toBeInTheDocument();
    expect(screen.getByRole("img", { name: "Illustration for question 1" })).toHaveAttribute(
      "src", "https://example.com/office.jpg",
    );
    expect(container.querySelector("audio")).toHaveAttribute("src", "https://example.com/notice.mp3");
    expect(screen.queryByText(/correctOption/i)).not.toBeInTheDocument();
  });

  it("answering calls PUT /attempts/{id}/answers/{questionId} with the chosen option and shows Saved", async () => {
    const user = userEvent.setup();
    const fetchMock = routeFetch({
      "GET /attempts/7": () => jsonResponse(200, attemptDetail()),
      [`PUT /attempts/7/answers/${Q1}`]: () => jsonResponse(200, { questionId: Q1, chosenOption: "to submit" }),
    });
    renderApp("/practice/7");

    await user.click(await screen.findByRole("radio", { name: /B\. to submit/ }));

    expect(await screen.findByText("Saved")).toBeInTheDocument();
    expect(putBodies(fetchMock)).toEqual([[`/attempts/7/answers/${Q1}`, { chosenOption: "to submit" }]]);
    const putInit = fetchMock.mock.calls.find(([, i]) => i?.method === "PUT")[1];
    expect(putInit.headers.Authorization).toBe("Bearer jwt-abc");
    expect(screen.getByText("1 of 5 answered")).toBeInTheDocument();
  });

  it("restores saved answers after a refresh and resumes at the first unanswered question", async () => {
    routeFetch({
      "GET /attempts/7": () =>
        jsonResponse(
          200,
          attemptDetail({
            answers: [
              { questionId: Q1, chosenOption: "to submit" },
              { questionId: Q2, chosenOption: "near" },
            ],
          }),
        ),
    });
    const user = userEvent.setup();
    renderApp("/practice/7");

    expect(await screen.findByText("Question 3 of 5")).toBeInTheDocument();
    expect(screen.getByText("2 of 5 answered")).toBeInTheDocument();
    expect(screen.getByRole("button", { name: "Question 1, answered" })).toBeInTheDocument();

    await user.click(screen.getByRole("button", { name: "Question 1, answered" }));
    expect(screen.getByRole("radio", { name: /B\. to submit/ })).toBeChecked();
  });

  it("re-answering sends the PUTs in the order the user made them", async () => {
    const user = userEvent.setup();
    const fetchMock = routeFetch({
      "GET /attempts/7": () => jsonResponse(200, attemptDetail()),
      [`PUT /attempts/7/answers/${Q1}`]: (init) => jsonResponse(200, JSON.parse(init.body)),
    });
    renderApp("/practice/7");

    await user.click(await screen.findByRole("radio", { name: /A\. submit/ }));
    await user.click(screen.getByRole("radio", { name: /C\. submitting/ }));

    await waitFor(() => expect(putBodies(fetchMock)).toHaveLength(2));
    expect(putBodies(fetchMock).map(([, body]) => body.chosenOption)).toEqual(["submit", "submitting"]);
    expect(screen.getByRole("radio", { name: /C\. submitting/ })).toBeChecked();
  });

  it("a failed save keeps the selection, shows an error, and Retry saves it", async () => {
    const user = userEvent.setup();
    let calls = 0;
    const fetchMock = routeFetch({
      "GET /attempts/7": () => jsonResponse(200, attemptDetail()),
      [`PUT /attempts/7/answers/${Q1}`]: () =>
        ++calls === 1 ? jsonResponse(500, { title: "Server error" }) : jsonResponse(200, { questionId: Q1, chosenOption: "submitted" }),
    });
    renderApp("/practice/7");

    await user.click(await screen.findByRole("radio", { name: /D\. submitted/ }));
    expect(await screen.findByText(/Couldn't save this answer/)).toBeInTheDocument();
    expect(screen.getByRole("radio", { name: /D\. submitted/ })).toBeChecked();

    await user.click(screen.getByRole("button", { name: "Retry" }));
    expect(await screen.findByText("Saved")).toBeInTheDocument();
    expect(putBodies(fetchMock)).toHaveLength(2);
  });

  it("Previous / Next move between questions and are disabled at the ends", async () => {
    const user = userEvent.setup();
    routeFetch({ "GET /attempts/7": () => jsonResponse(200, attemptDetail()) });
    renderApp("/practice/7");

    await screen.findByText("Question 1 of 5");
    expect(screen.getByRole("button", { name: "Previous" })).toBeDisabled();

    await user.click(screen.getByRole("button", { name: "Next" }));
    expect(screen.getByText("Question 2 of 5")).toBeInTheDocument();

    for (let i = 0; i < 3; i++) await user.click(screen.getByRole("button", { name: "Next" }));
    expect(screen.getByText("Question 5 of 5")).toBeInTheDocument();
    expect(screen.getByRole("button", { name: "Next" })).toBeDisabled();
  });

  it("a submitted attempt is read-only", async () => {
    routeFetch({
      "GET /attempts/7": () =>
        jsonResponse(200, attemptDetail({ status: "SUBMITTED", answers: [{ questionId: Q1, chosenOption: "submit" }] })),
    });
    renderApp("/practice/7");

    expect(await screen.findByText(/already been submitted/)).toBeInTheDocument();
    screen.getAllByRole("radio").forEach((r) => expect(r).toBeDisabled());
  });

  it.each([403, 404])("%i: shows 'not available' with a way back", async (status) => {
    routeFetch({ "GET /attempts/7": () => jsonResponse(status, { title: "x", status }) });
    renderApp("/practice/7");

    expect(await screen.findByText("This practice isn't available.")).toBeInTheDocument();
    expect(screen.getByRole("link", { name: "Back to Dashboard" })).toHaveAttribute("href", "/dashboard");
  });

  it("load failure: alert and Try again", async () => {
    const user = userEvent.setup();
    let calls = 0;
    routeFetch({
      "GET /attempts/7": () => (++calls === 1 ? jsonResponse(500, { title: "x" }) : jsonResponse(200, attemptDetail())),
    });
    renderApp("/practice/7");

    expect(await screen.findByRole("alert")).toHaveTextContent("couldn't load your practice");
    await user.click(screen.getByRole("button", { name: "Try again" }));
    expect(await screen.findByText("Question 1 of 5")).toBeInTheDocument();
  });

  it("401: ends the session and returns to Login", async () => {
    routeFetch({ "GET /attempts/7": () => jsonResponse(401, { title: "Unauthorized" }) });
    renderApp("/practice/7");

    expect(await screen.findByRole("heading", { name: "Log in" })).toBeInTheDocument();
    expect(window.localStorage.getItem("engcoach.session")).toBeNull();
  });

  it("uses the question ids and option strings from the API (nothing is hard-coded)", async () => {
    const user = userEvent.setup();
    const fetchMock = routeFetch({
      "GET /attempts/7": () =>
        jsonResponse(
          200,
          attemptDetail({ questions: [{ id: 4242, prompt: "Pick one.", options: ["alpha", "beta"] }] }),
        ),
      "PUT /attempts/7/answers/4242": () => jsonResponse(200, { questionId: 4242, chosenOption: "beta" }),
    });
    renderApp("/practice/7");

    await user.click(await screen.findByRole("radio", { name: /B\. beta/ }));

    expect(await screen.findByText("Saved")).toBeInTheDocument();
    expect(putBodies(fetchMock)).toEqual([["/attempts/7/answers/4242", { chosenOption: "beta" }]]);
  });

  it("an attempt with no questions (e.g. a mock test before S11) shows an empty state", async () => {
    routeFetch({ "GET /attempts/7": () => jsonResponse(200, attemptDetail({ type: "MOCK_TEST", part: null, questions: [] })) });
    renderApp("/practice/7");

    expect(await screen.findByText(/no questions in this practice yet/)).toBeInTheDocument();
    expect(screen.getByRole("link", { name: "Back to Dashboard" })).toBeInTheDocument();
  });

  describe("submitting (S9)", () => {
    const allAnswered = () =>
      [101, 102, 103, 104, 105].map((id, i) => ({ questionId: id, chosenOption: i === 0 ? "to submit" : "near" }));
    const graded = { id: 7, status: "SUBMITTED", score: 3, totalQuestions: 5, answers: [] };

    // The server reports SUBMITTED from the moment grading has happened, which
    // is what the Result screen checks before showing anything.
    function server({ saved = allAnswered(), submit = () => jsonResponse(200, graded), extra = {} } = {}) {
      let submitted = false;
      return routeFetch({
        "GET /attempts/7": () =>
          jsonResponse(200, attemptDetail({ answers: saved, status: submitted ? "SUBMITTED" : "IN_PROGRESS" })),
        "POST /attempts/7/submit": (init) => {
          const response = submit(init);
          if (response instanceof Response && response.ok) submitted = true;
          return response;
        },
        ...extra,
      });
    }

    it("with every question answered, submits and shows the Result screen", async () => {
      const user = userEvent.setup();
      const fetchMock = server();
      renderApp("/practice/7");

      await user.click(await screen.findByRole("button", { name: "Submit for grading" }));

      expect(await screen.findByRole("heading", { name: "Part 5 result" })).toBeInTheDocument();
      expect(screen.getByText("3 / 5")).toBeInTheDocument();
      expect(fetchMock.mock.calls.filter(([url]) => url === "/attempts/7/submit")).toHaveLength(1);
    });

    it("with unanswered questions, asks for confirmation first; Keep working cancels", async () => {
      const user = userEvent.setup();
      const fetchMock = routeFetch({
        "GET /attempts/7": () => jsonResponse(200, attemptDetail({ answers: allAnswered().slice(0, 2) })),
      });
      renderApp("/practice/7");

      await user.click(await screen.findByRole("button", { name: "Submit for grading" }));

      expect(screen.getByRole("group", { name: "Confirm submission" })).toHaveTextContent(
        "3 unanswered questions",
      );
      await user.click(screen.getByRole("button", { name: "Keep working" }));
      expect(screen.queryByRole("group", { name: "Confirm submission" })).not.toBeInTheDocument();
      expect(fetchMock.mock.calls.some(([url]) => url === "/attempts/7/submit")).toBe(false);
    });

    it("Submit anyway grades with unanswered questions", async () => {
      const user = userEvent.setup();
      server({ saved: allAnswered().slice(0, 2) });
      renderApp("/practice/7");

      await user.click(await screen.findByRole("button", { name: "Submit for grading" }));
      await user.click(screen.getByRole("button", { name: "Submit anyway" }));

      expect(await screen.findByRole("heading", { name: "Part 5 result" })).toBeInTheDocument();
    });

    it("shows a grading state and disables the button while the server grades", async () => {
      const user = userEvent.setup();
      server({ submit: () => new Promise(() => {}) }); // the server never answers
      renderApp("/practice/7");

      await user.click(await screen.findByRole("button", { name: "Submit for grading" }));

      expect(await screen.findByRole("button", { name: "Grading…" })).toBeDisabled();
      expect(screen.getByText(/Grading your answers/)).toBeInTheDocument();
    });

    it("a grading failure keeps the answers, shows an error, and lets the user try again", async () => {
      const user = userEvent.setup();
      let calls = 0;
      server({ submit: () => (++calls === 1 ? jsonResponse(500, { title: "x" }) : jsonResponse(200, graded)) });
      renderApp("/practice/7");

      await user.click(await screen.findByRole("button", { name: "Submit for grading" }));
      expect(await screen.findByRole("alert")).toHaveTextContent("couldn't grade your attempt");

      await user.click(screen.getByRole("button", { name: "Submit for grading" }));
      expect(await screen.findByRole("heading", { name: "Part 5 result" })).toBeInTheDocument();
    });

    it("waits for an in-flight answer save before grading", async () => {
      const user = userEvent.setup();
      let releasePut;
      const order = [];
      server({
        saved: allAnswered().slice(1),
        submit: () => {
          order.push("SUBMIT");
          return jsonResponse(200, graded);
        },
        extra: {
          "PUT /attempts/7/answers/101": () => {
            order.push("PUT");
            return new Promise((resolve) => (releasePut = () => resolve(jsonResponse(200, { questionId: 101 }))));
          },
        },
      });
      renderApp("/practice/7");

      await user.click(await screen.findByRole("radio", { name: /B\. to submit/ })); // 5th answer, save pending
      await user.click(screen.getByRole("button", { name: "Submit for grading" }));

      expect(order).toEqual(["PUT"]); // not graded yet
      releasePut();
      expect(await screen.findByRole("heading", { name: "Part 5 result" })).toBeInTheDocument();
      expect(order).toEqual(["PUT", "SUBMIT"]);
    });

    it("an answer that failed to save blocks submission and says why", async () => {
      const user = userEvent.setup();
      const fetchMock = routeFetch({
        "GET /attempts/7": () => jsonResponse(200, attemptDetail({ answers: allAnswered().slice(1) })),
        "PUT /attempts/7/answers/101": () => jsonResponse(500, { title: "x" }),
      });
      renderApp("/practice/7");

      await user.click(await screen.findByRole("radio", { name: /B\. to submit/ }));
      await screen.findByText(/Couldn't save this answer/);
      await user.click(screen.getByRole("button", { name: "Submit for grading" }));

      expect(await screen.findByRole("alert")).toHaveTextContent("Some answers couldn't be saved yet");
      expect(fetchMock.mock.calls.some(([url]) => url === "/attempts/7/submit")).toBe(false);
    });

    it("an already-submitted attempt has no Submit button, only a link to its result", async () => {
      routeFetch({ "GET /attempts/7": () => jsonResponse(200, attemptDetail({ status: "SUBMITTED" })) });
      renderApp("/practice/7");

      expect(await screen.findByRole("link", { name: "View result" })).toHaveAttribute("href", "/result/7");
      expect(screen.queryByRole("button", { name: "Submit for grading" })).not.toBeInTheDocument();
    });
  });

  describe("mock-test timer (S12)", () => {
    it("shows the server-computed countdown for a mock test", async () => {
      routeFetch({
        "GET /attempts/7": () =>
          jsonResponse(200, attemptDetail({ type: "MOCK_TEST", part: null, remainingSeconds: 3661 })),
      });
      renderApp("/practice/7");

      expect(await screen.findByRole("timer", { name: "Time remaining" })).toHaveTextContent("Time remaining: 61:01");
      expect(screen.getByRole("timer", { name: "Time remaining" })).toHaveAttribute("data-time-state", "normal");
      expect(screen.getByRole("heading", { name: "Mock test" })).toBeInTheDocument();
    });

    it.each([[299, "warning"], [59, "critical"]])(
      "applies the approved %s-second urgency state",
      async (remainingSeconds, expectedState) => {
        routeFetch({
          "GET /attempts/7": () =>
            jsonResponse(200, attemptDetail({ type: "MOCK_TEST", part: null, remainingSeconds })),
        });
        renderApp("/practice/7");

        expect(await screen.findByRole("timer", { name: "Time remaining" })).toHaveAttribute(
          "data-time-state",
          expectedState,
        );
      },
    );

    it("automatically submits a mock test when the server-computed time reaches zero", async () => {
      let submitted = false;
      const fetchMock = routeFetch({
        "GET /attempts/7": () =>
          jsonResponse(200, attemptDetail({
            type: "MOCK_TEST",
            part: null,
            remainingSeconds: 0,
            status: submitted ? "SUBMITTED" : "IN_PROGRESS",
          })),
        "POST /attempts/7/submit": () => {
          submitted = true;
          return jsonResponse(200, { id: 7, status: "SUBMITTED", score: 3, totalQuestions: 5, answers: [] });
        },
      });
      renderApp("/practice/7");

      expect(await screen.findByText("3 / 5")).toBeInTheDocument();
      expect(fetchMock.mock.calls.filter(([url, init]) => url === "/attempts/7/submit" && init?.method === "POST")).toHaveLength(1);
    });
  });
});
