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
});
