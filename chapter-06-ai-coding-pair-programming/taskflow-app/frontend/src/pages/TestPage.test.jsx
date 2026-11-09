import { screen, waitFor } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { beforeEach, describe, expect, it, vi } from "vitest";
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

  it("offers browser speech playback when a listening script is supplied", async () => {
    const detail = attemptDetail({
      questions: [{
        id: 101,
        prompt: "Choose the best response.",
        options: ["Before noon.", "At the front desk.", "The blue folder."],
        passage: null,
        imageUrl: null,
        audioUrl: null,
        audioText: "When will the shipment arrive?",
      }],
    });
    routeFetch({ "GET /attempts/7": () => jsonResponse(200, detail) });
    renderApp("/practice/7");

    expect(await screen.findByRole("button", { name: "Play listening audio" })).toBeInTheDocument();
  });

  it("uses shared listening-group content when the individual question has no audio script", async () => {
    const detail = attemptDetail({
      type: "TEST",
      testId: 12,
      selectedParts: ["Part 3"],
      timeLimitSeconds: 1020,
      remainingSeconds: 1010,
      questions: [{
        id: 101,
        prompt: "What does the woman ask?",
        options: ["A delivery time", "A new invoice"],
        audioUrl: null,
        audioText: null,
        groupId: 88,
        groupType: "CONVERSATION",
        groupContent: "W: Could you tell me when the order will arrive?",
      }],
    });
    routeFetch({ "GET /attempts/7": () => jsonResponse(200, detail) });
    renderApp("/practice/7");

    expect(await screen.findByRole("heading", { name: "TOEIC Test" })).toBeInTheDocument();
    expect(screen.getByRole("timer", { name: "Time remaining" })).toHaveTextContent("16:50");
    expect(screen.getByRole("button", { name: "Play listening audio for conversation" })).toBeInTheDocument();
  });

  it("shows the selected Parts of a Test attempt", async () => {
    routeFetch({
      "GET /attempts/7": () => jsonResponse(200, attemptDetail({
        type: "TEST",
        testId: 12,
        selectedParts: ["Part 1", "Part 3"],
        timeLimitSeconds: 1200,
        remainingSeconds: 600,
      })),
    });
    renderApp("/practice/7");

    expect(await screen.findByRole("heading", { name: "TOEIC Test" })).toBeInTheDocument();
    expect(screen.getByLabelText("Selected Test Parts")).toHaveTextContent("Selected: Part 1, Part 3");
  });

  it("automatically submits a selected-Part Test once when its time reaches zero", async () => {
    let submitted = false;
    const fetchMock = routeFetch({
      "GET /attempts/7": () => jsonResponse(200, attemptDetail({
        type: "TEST",
        testId: 12,
        selectedParts: ["Part 1", "Part 3"],
        timeLimitSeconds: 1200,
        remainingSeconds: 0,
        status: submitted ? "SUBMITTED" : "IN_PROGRESS",
      })),
      "POST /attempts/7/submit": () => {
        submitted = true;
        return jsonResponse(200, { id: 7, status: "SUBMITTED", score: 2, totalQuestions: 8, answers: [] });
      },
    });
    renderApp("/practice/7");

    // Expiry submits and moves to the result page straight away, so wait for the result, not for the test heading.
    expect(await screen.findByText("2 / 8")).toBeInTheDocument();
    expect(fetchMock.mock.calls.filter(([url, init]) => url === "/attempts/7/submit" && init?.method === "POST")).toHaveLength(1);
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
      const ring = screen.getByTestId("mock-timer-ring-value");
      expect(ring).toHaveAttribute("stroke-dasharray");
      expect(ring).toHaveAttribute("stroke-dashoffset");
      expect(screen.getByText("61:01")).toBeInTheDocument();
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
        expect(screen.getByTestId("mock-timer-ring-value").getAttribute("class")).toContain(
          expectedState === "warning" ? "timer-ring-value--warning" : "timer-ring-value--danger",
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

describe("Test screen: Part 2 letters and grouped Part 3-4", () => {
  const conversation = (firstId, groupId, groupType = "CONVERSATION") => [0, 1, 2].map((n) => ({
    id: firstId + n,
    prompt: `Prompt ${firstId + n}?`,
    options: ["Alpha", "Bravo", "Charlie", "Delta"],
    audioUrl: `/audio/test-01/g${groupId}.mp3`,
    audioText: "Shared script",
    groupId,
    groupType,
    groupContent: "Shared script",
  }));

  it("Part 2 shows only the letters A, B, C and hides the question and answer text", async () => {
    const user = userEvent.setup();
    const fetchMock = routeFetch({
      "GET /attempts/7": () => jsonResponse(200, attemptDetail({
        type: "TEST", testId: 12, selectedParts: ["Part 2"], timeLimitSeconds: 540, remainingSeconds: 500,
        questions: [{
          id: 201, prompt: "Where should I put the paper?", options: ["On the desk.", "At noon.", "Yes, he is."],
          audioUrl: "/audio/test-01/q7.mp3", audioText: "Number 7.", groupId: 5, groupType: "LISTENING_ITEM", groupContent: "x",
        }],
      })),
      "PUT /attempts/7/answers/201": () => jsonResponse(200, { questionId: 201, chosenOption: "At noon." }),
    });
    renderApp("/practice/7");

    expect(await screen.findByText("Question 1 of 1")).toBeInTheDocument();
    expect(screen.queryByText("Where should I put the paper?")).not.toBeInTheDocument();
    expect(screen.queryByText("On the desk.")).not.toBeInTheDocument();
    expect(screen.getByLabelText("Option A")).toBeInTheDocument();
    expect(screen.getByLabelText("Option C")).toBeInTheDocument();

    await user.click(screen.getByLabelText("Option B"));
    await waitFor(() => expect(putBodies(fetchMock)).toEqual([["/attempts/7/answers/201", { chosenOption: "At noon." }]]));
  });

  it("Part 3 shows the three questions of one conversation together with one audio player", async () => {
    const user = userEvent.setup();
    routeFetch({
      "GET /attempts/7": () => jsonResponse(200, attemptDetail({
        type: "TEST", testId: 12, selectedParts: ["Part 3"], timeLimitSeconds: 1020, remainingSeconds: 900,
        questions: [...conversation(301, 11), ...conversation(304, 12)],
      })),
    });
    renderApp("/practice/7");

    expect(await screen.findByText("Question 1 of 6")).toBeInTheDocument();
    expect(screen.getByText("Question 2 of 6")).toBeInTheDocument();
    expect(screen.getByText("Question 3 of 6")).toBeInTheDocument();
    expect(screen.queryByText("Question 4 of 6")).not.toBeInTheDocument();
    expect(document.querySelectorAll("audio")).toHaveLength(1);
    expect(screen.getByLabelText("Audio for questions 1 to 3")).toBeInTheDocument();
    expect(screen.getByRole("button", { name: "Previous" })).toBeDisabled();

    await user.click(screen.getByRole("button", { name: "Next" }));
    expect(screen.getByText("Question 4 of 6")).toBeInTheDocument();
    expect(screen.queryByText("Question 1 of 6")).not.toBeInTheDocument();
    expect(screen.getByRole("button", { name: "Next" })).toBeDisabled();
  });

  it("clicking a question number jumps to the screen that holds it and Part 4 groups the same way", async () => {
    const user = userEvent.setup();
    routeFetch({
      "GET /attempts/7": () => jsonResponse(200, attemptDetail({
        type: "TEST", testId: 12, selectedParts: ["Part 4"], timeLimitSeconds: 960, remainingSeconds: 900,
        questions: [...conversation(401, 21, "TALK"), ...conversation(404, 22, "TALK")],
      })),
    });
    renderApp("/practice/7");

    await screen.findByText("Question 1 of 6");
    await user.click(screen.getByRole("button", { name: "Question 5" }));
    expect(screen.getByText("Question 4 of 6")).toBeInTheDocument();
    expect(screen.getByText("Question 6 of 6")).toBeInTheDocument();
    expect(screen.getByRole("button", { name: "Question 5" })).toHaveAttribute("aria-current", "step");
  });

  it("resumes at the first screen that still has an unanswered question", async () => {
    routeFetch({
      "GET /attempts/7": () => jsonResponse(200, attemptDetail({
        type: "TEST", testId: 12, selectedParts: ["Part 3"], timeLimitSeconds: 1020, remainingSeconds: 900,
        questions: [...conversation(301, 11), ...conversation(304, 12)],
        answers: [301, 302, 303].map((id) => ({ questionId: id, chosenOption: "Alpha" })),
      })),
    });
    renderApp("/practice/7");

    expect(await screen.findByText("Question 4 of 6")).toBeInTheDocument();
    expect(screen.queryByText("Question 1 of 6")).not.toBeInTheDocument();
  });

  it("Part 6 and 7 passages are read on screen: no listening audio, and the questions of one passage are shown together", async () => {
    const user = userEvent.setup();
    const passageQuestions = (firstId, groupId) => [0, 1].map((n) => ({
      id: firstId + n,
      prompt: `Question text ${firstId + n}`,
      options: ["one", "two", "three", "four"],
      passage: `Passage number ${groupId}`,
      audioUrl: null,
      audioText: null,
      groupId,
      groupType: "PASSAGE",
      groupContent: `Passage number ${groupId}`,
    }));
    routeFetch({
      "GET /attempts/7": () => jsonResponse(200, attemptDetail({
        type: "TEST", testId: 12, selectedParts: ["Part 7"], timeLimitSeconds: 3300, remainingSeconds: 3000,
        questions: [...passageQuestions(501, 31), ...passageQuestions(503, 32)],
      })),
    });
    renderApp("/practice/7");

    expect(await screen.findByText("Question 1 of 4")).toBeInTheDocument();
    expect(screen.getByText("Question 2 of 4")).toBeInTheDocument();
    expect(screen.getAllByText("Passage number 31")).toHaveLength(1);
    expect(screen.queryByRole("button", { name: /Play listening audio/ })).not.toBeInTheDocument();
    expect(screen.queryByText("Computer-generated voice")).not.toBeInTheDocument();

    await user.click(screen.getByRole("button", { name: "Next" }));
    expect(screen.getByText("Question 3 of 4")).toBeInTheDocument();
    expect(screen.getByText("Passage number 32")).toBeInTheDocument();
  });

  it("renders a reading passage from shared group content when the question has no individual passage", async () => {
    routeFetch({
      "GET /attempts/7": () => jsonResponse(200, attemptDetail({
        type: "TEST", testId: 12, selectedParts: ["Part 7"], timeLimitSeconds: 3300, remainingSeconds: 3000,
        questions: [{
          id: 550, prompt: "What is the notice about?", options: ["A meeting", "A delivery"],
          passage: null, groupId: 44, groupType: "PASSAGE",
          groupContent: "NOTICE TO STAFF: The office will close early on Friday.",
        }],
      })),
    });
    renderApp("/practice/7");

    expect(await screen.findByText(/NOTICE TO STAFF: The office will close early on Friday/)).toBeInTheDocument();
    expect(screen.getByText("What is the notice about?")).toBeInTheDocument();
  });

  it("Part 1 shows the photo with only the letters A to D and hides the statements", async () => {
    const user = userEvent.setup();
    const fetchMock = routeFetch({
      "GET /attempts/7": () => jsonResponse(200, attemptDetail({
        type: "TEST", testId: 12, selectedParts: ["Part 1"], timeLimitSeconds: 180, remainingSeconds: 170,
        questions: [{
          id: 601, prompt: "Look at the photo.", options: ["A man is reading.", "Boxes are stacked.", "A car is parked.", "People are eating."],
          imageUrl: "/part1/photo-1.jpg", audioUrl: "/audio/test-01/p1.mp3", audioText: "Number 1.", groupId: 3, groupType: "PHOTO",
          groupContent: "Number 1.",
        }],
      })),
      "PUT /attempts/7/answers/601": () => jsonResponse(200, { questionId: 601, chosenOption: "A car is parked." }),
    });
    renderApp("/practice/7");

    expect(await screen.findByText("Question 1 of 1")).toBeInTheDocument();
    expect(screen.getByAltText("Illustration for question 1")).toBeInTheDocument();
    expect(screen.queryByText("A man is reading.")).not.toBeInTheDocument();
    expect(screen.queryByText("Look at the photo.")).not.toBeInTheDocument();
    for (const l of ["A", "B", "C", "D"]) expect(screen.getByLabelText(`Option ${l}`)).toBeInTheDocument();

    await user.click(screen.getByLabelText("Option C"));
    await waitFor(() => expect(putBodies(fetchMock)).toEqual([["/attempts/7/answers/601", { chosenOption: "A car is parked." }]]));
  });
});

describe("Exam mode listening audio", () => {
  const examAttempt = (type, part) => attemptDetail({
    type, part, testId: type === "TEST" ? 12 : undefined, selectedParts: type === "TEST" ? ["Part 3"] : undefined,
    timeLimitSeconds: type === "TEST" ? 1020 : null, remainingSeconds: type === "TEST" ? 900 : null,
    questions: [{
      id: 301, prompt: "Where does this take place?", options: ["Office", "Bank", "Shop", "Park"],
      audioUrl: "/audio/test-01/g11.mp3", audioText: "Script", groupId: 11, groupType: "CONVERSATION", groupContent: "Script",
    }],
  });

  it("in a Test the audio can be played only once, even after reloading the page", async () => {
    const user = userEvent.setup();
    const play = vi.spyOn(window.HTMLMediaElement.prototype, "play").mockImplementation(() => Promise.resolve());
    routeFetch({ "GET /attempts/7": () => jsonResponse(200, examAttempt("TEST", null)) });
    const first = renderApp("/practice/7");

    expect(await screen.findByText("Question 1 of 1")).toBeInTheDocument();
    expect(document.querySelector("audio")).not.toHaveAttribute("controls");
    await user.click(screen.getByRole("button", { name: "Play audio (one time only)" }));
    expect(play).toHaveBeenCalledTimes(1);
    expect(screen.getByRole("button", { name: /Playing|Audio has been played/ })).toBeDisabled();

    // Simulate a refresh: a fresh render of the same attempt must not allow another play.
    first.unmount();
    routeFetch({ "GET /attempts/7": () => jsonResponse(200, examAttempt("TEST", null)) });
    renderApp("/practice/7");
    expect(await screen.findByRole("button", { name: "Audio has been played" })).toBeDisabled();
    expect(play).toHaveBeenCalledTimes(1);
    play.mockRestore();
  });

  it("gives the play back when the browser refuses to play the audio", async () => {
    const user = userEvent.setup();
    const play = vi.spyOn(window.HTMLMediaElement.prototype, "play").mockImplementation(() => Promise.reject(new Error("blocked")));
    routeFetch({ "GET /attempts/7": () => jsonResponse(200, examAttempt("TEST", null)) });
    renderApp("/practice/7");

    await user.click(await screen.findByRole("button", { name: "Play audio (one time only)" }));
    expect(await screen.findByRole("button", { name: "Play audio (one time only)" })).toBeEnabled();
    play.mockRestore();
  });

  it("in Part practice the audio keeps its normal player controls", async () => {
    routeFetch({ "GET /attempts/7": () => jsonResponse(200, examAttempt("PART_PRACTICE", "Part 3")) });
    renderApp("/practice/7");

    expect(await screen.findByText("Question 1 of 1")).toBeInTheDocument();
    expect(document.querySelector("audio")).toHaveAttribute("controls");
    expect(screen.queryByText(/one time only/)).not.toBeInTheDocument();
  });
});
