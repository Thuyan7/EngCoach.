import { afterEach, beforeEach, describe, expect, it, vi } from "vitest";
import { act, screen, waitFor } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { attemptDetail, jsonResponse, questionsFixture, renderApp, routeFetch, signIn } from "../test/helpers.jsx";

beforeEach(() => {
  window.localStorage.clear();
  signIn();
});

afterEach(() => {
  vi.restoreAllMocks();
});

const PASSAGE = "The meeting starts at nine. Please bring the quarterly invoice to the front desk.";

const mistakes = {
  total: 2,
  partCounts: [{ part: "Part 7", count: 2 }],
  mistakes: [1, 2].map((id) => ({
    attemptId: 3,
    answeredAt: "2026-11-01T08:00:00Z",
    part: "Part 7",
    chosenOption: "B",
    explanationText: null,
    question: { id, prompt: `What should staff bring? (${id})`, options: ["A", "B", "C", "D"], correctOption: "A", groupContent: PASSAGE },
  })),
};

// Pretend the learner selected `text` inside `node` (jsdom has no real text selection).
function select(text, node) {
  vi.spyOn(window, "getSelection").mockReturnValue({ toString: () => text, anchorNode: node });
  act(() => {
    document.dispatchEvent(new Event("selectionchange"));
  });
}

// The card attaches its selection listener in an effect that can run just after the text appears, so a selection
// made at that moment may be missed. Make the selection again until the expected result shows up.
async function selectUntil(text, getNode, expectation) {
  await waitFor(() => {
    select(text, getNode());
    expectation();
  });
}

const passageNodes = () => screen.getAllByText(PASSAGE).map((paragraph) => paragraph.firstChild);

describe("Saving a selected word", () => {
  const SENTENCE = "Please bring the quarterly invoice to the front desk.";
  const lookupKey = (word, context) => `GET /vocab/lookup?word=${encodeURIComponent(word)}&context=${encodeURIComponent(context)}`;
  const selectInvoice = () => selectUntil("quarterly invoice", () => passageNodes()[0],
    () => expect(screen.getByText("quarterly invoice", { selector: "strong" })).toBeInTheDocument());
  const postedBody = (fetchMock) => {
    const post = fetchMock.mock.calls.find(([url, init]) => url === "/vocab" && init?.method === "POST");
    return post && JSON.parse(post[1].body);
  };

  const meaningBox = () => screen.getByRole("textbox", { name: "Meaning" });

  it("looks the meaning up, fills it in and saves the word with one click", async () => {
    const user = userEvent.setup();
    const fetchMock = routeFetch({
      "GET /attempts/mistakes": () => jsonResponse(200, mistakes),
      [lookupKey("quarterly invoice", SENTENCE)]: () => jsonResponse(200, { meaning: "hoa don hang quy", partOfSpeech: "noun", phonetic: "/x/" }),
      "POST /vocab": () => jsonResponse(201, { id: 1, word: "quarterly invoice", meaning: "hoa don hang quy" }),
    });
    renderApp("/mistakes");
    await screen.findAllByText(PASSAGE);

    await selectInvoice();
    await waitFor(() => expect(meaningBox()).toHaveValue("hoa don hang quy"));
    expect(screen.getByText("noun")).toBeInTheDocument();
    expect(screen.getByText("/x/")).toBeInTheDocument();
    expect(screen.getByText(SENTENCE, { selector: ".word-bar-example" })).toBeInTheDocument();
    expect(screen.getByRole("link", { name: "Look it up" })).toHaveAttribute("href", expect.stringContaining("quarterly%20invoice"));

    select("", passageNodes()[0]); // a real browser drops the highlight once the learner clicks the button
    await user.click(screen.getByRole("button", { name: 'Save "quarterly invoice"' }));

    expect(await screen.findByText('Saved "quarterly invoice" to your vocabulary.')).toBeInTheDocument();
    expect(screen.queryByRole("group", { name: "Save selected word" })).not.toBeInTheDocument();
    expect(postedBody(fetchMock)).toEqual({
      word: "quarterly invoice",
      meaning: "hoa don hang quy",
      example: SENTENCE,
      questionId: 1,
    });
  });

  it("lets the learner correct the suggested meaning in the bar and save with Enter", async () => {
    const user = userEvent.setup();
    const fetchMock = routeFetch({
      "GET /attempts/mistakes": () => jsonResponse(200, mistakes),
      [lookupKey("quarterly invoice", SENTENCE)]: () => jsonResponse(200, { meaning: "hoa don", partOfSpeech: null, phonetic: null }),
      "POST /vocab": () => jsonResponse(201, { id: 1 }),
    });
    renderApp("/mistakes");
    await screen.findAllByText(PASSAGE);

    await selectInvoice();
    await waitFor(() => expect(meaningBox()).toHaveValue("hoa don"));
    await user.clear(meaningBox());
    await user.type(meaningBox(), "hoa don hang quy{Enter}");

    expect(await screen.findByText('Saved "quarterly invoice" to your vocabulary.')).toBeInTheDocument();
    expect(postedBody(fetchMock)).toMatchObject({ meaning: "hoa don hang quy" });
  });

  it("does not overwrite what the learner typed when the lookup answers late", async () => {
    const user = userEvent.setup();
    let release;
    const late = new Promise((resolve) => { release = resolve; });
    routeFetch({
      "GET /attempts/mistakes": () => jsonResponse(200, mistakes),
      [lookupKey("quarterly invoice", SENTENCE)]: async () => {
        await late;
        return jsonResponse(200, { meaning: "suggested", partOfSpeech: null, phonetic: null });
      },
    });
    renderApp("/mistakes");
    await screen.findAllByText(PASSAGE);

    await selectInvoice();
    expect(meaningBox()).toHaveAttribute("placeholder", "Looking up the meaning…");
    await user.type(meaningBox(), "mine");
    release();
    await act(async () => { await late; });
    await waitFor(() => expect(meaningBox()).toHaveAttribute("placeholder", "Type the meaning"));
    expect(meaningBox()).toHaveValue("mine");
  });

  it("asks for a meaning when none could be found, then saves what the learner types", async () => {
    const user = userEvent.setup();
    const fetchMock = routeFetch({
      "GET /attempts/mistakes": () => jsonResponse(200, mistakes),
      [lookupKey("quarterly invoice", SENTENCE)]: () => jsonResponse(200, { meaning: null, partOfSpeech: null, phonetic: null }),
      "POST /vocab": () => jsonResponse(201, { id: 1 }),
    });
    renderApp("/mistakes");
    await screen.findAllByText(PASSAGE);

    await selectInvoice();
    await waitFor(() => expect(meaningBox()).toHaveAttribute("placeholder", "Type the meaning"));
    expect(meaningBox()).toHaveValue("");
    await user.click(screen.getByRole("button", { name: 'Save "quarterly invoice"' }));
    expect(await screen.findByRole("alert")).toHaveTextContent("Type the meaning first.");
    expect(postedBody(fetchMock)).toBeUndefined();

    await user.type(meaningBox(), "hoa don hang quy");
    await user.click(screen.getByRole("button", { name: 'Save "quarterly invoice"' }));
    expect(await screen.findByText('Saved "quarterly invoice" to your vocabulary.')).toBeInTheDocument();
    expect(postedBody(fetchMock)).toMatchObject({ word: "quarterly invoice", meaning: "hoa don hang quy" });
  });

  it("falls back to typing when the lookup fails", async () => {
    routeFetch({
      "GET /attempts/mistakes": () => jsonResponse(200, mistakes),
      [lookupKey("quarterly invoice", SENTENCE)]: () => jsonResponse(500, { detail: "boom" }),
    });
    renderApp("/mistakes");
    await screen.findAllByText(PASSAGE);

    await selectInvoice();
    await waitFor(() => expect(meaningBox()).toHaveAttribute("placeholder", "Type the meaning"));
    expect(meaningBox()).toHaveValue("");
  });

  it("keeps a single bar open when text is selected in another card", async () => {
    routeFetch({ "GET /attempts/mistakes": () => jsonResponse(200, mistakes) });
    renderApp("/mistakes");
    await screen.findAllByText(PASSAGE);

    await selectUntil("meeting", () => passageNodes()[0],
      () => expect(screen.getAllByRole("group", { name: "Save selected word" })).toHaveLength(1));
    await selectUntil("quarterly invoice", () => passageNodes()[1],
      () => expect(screen.getByText("quarterly invoice", { selector: "strong" })).toBeInTheDocument());
    expect(screen.getAllByRole("group", { name: "Save selected word" })).toHaveLength(1);
    expect(screen.queryByText("meeting", { selector: "strong" })).not.toBeInTheDocument();
  });

  it("shows the selection only on the card where the text was selected", async () => {
    routeFetch({ "GET /attempts/mistakes": () => jsonResponse(200, mistakes) });
    renderApp("/mistakes");
    await screen.findAllByText(PASSAGE);

    await selectUntil("meeting", () => passageNodes()[1],
      () => expect(screen.getAllByText("meeting", { selector: "strong" })).toHaveLength(1));
    expect(screen.getAllByRole("button", { name: 'Save "meeting"' })).toHaveLength(1);
    expect(screen.getAllByRole("group", { name: "Save selected word" })).toHaveLength(1);
  });

  it("ignores a selection outside the cards, one that is empty and one that is too long", async () => {
    routeFetch({ "GET /attempts/mistakes": () => jsonResponse(200, mistakes) });
    renderApp("/mistakes");
    await screen.findAllByText(PASSAGE);

    // Prove the listener is attached by making one valid selection, then dismiss it before the ignored ones.
    await selectUntil("meeting", () => passageNodes()[0],
      () => expect(screen.getAllByRole("button", { name: "Dismiss" })).toHaveLength(1));
    select("", passageNodes()[0]); // the real browser selection is gone once the learner clicks elsewhere
    await userEvent.click(screen.getByRole("button", { name: "Dismiss" }));
    expect(screen.queryByRole("group", { name: "Save selected word" })).not.toBeInTheDocument();

    select("word", document.body);
    select("   ", passageNodes()[0]);
    select("word ".repeat(30), passageNodes()[0]);
    expect(screen.queryByRole("group", { name: "Save selected word" })).not.toBeInTheDocument();
  });

  it("keeps the selection when the browser clears it, and Dismiss removes it", async () => {
    const user = userEvent.setup();
    routeFetch({ "GET /attempts/mistakes": () => jsonResponse(200, { ...mistakes, mistakes: [mistakes.mistakes[0]] }) });
    renderApp("/mistakes");
    await screen.findAllByText(PASSAGE);

    await selectUntil("meeting", () => passageNodes()[0],
      () => expect(screen.getByRole("button", { name: 'Save "meeting"' })).toBeInTheDocument());

    // Pressing a button empties the browser selection; the Save button must still be there.
    select("", passageNodes()[0]);
    expect(screen.getByRole("button", { name: 'Save "meeting"' })).toBeInTheDocument();

    await user.click(screen.getByRole("button", { name: "Dismiss" }));
    expect(screen.queryByRole("button", { name: 'Save "meeting"' })).not.toBeInTheDocument();
    expect(screen.getByRole("button", { name: "Save a word" })).toBeInTheDocument();
  });

  it("still lets the learner type a word by hand", async () => {
    const user = userEvent.setup();
    const fetchMock = routeFetch({
      "GET /attempts/mistakes": () => jsonResponse(200, { ...mistakes, mistakes: [mistakes.mistakes[0]] }),
      "POST /vocab": () => jsonResponse(201, { id: 2 }),
    });
    renderApp("/mistakes");

    await user.click(await screen.findByRole("button", { name: "Save a word" }));
    expect(screen.getByLabelText("Word or phrase")).toHaveValue("");
    await user.type(screen.getByLabelText("Word or phrase"), "front desk");
    await user.type(screen.getByLabelText("Meaning"), "quay le tan");
    await user.click(screen.getByRole("button", { name: "Save word" }));

    expect(await screen.findByText('Saved "front desk" to your vocabulary.')).toBeInTheDocument();
    const post = fetchMock.mock.calls.find(([url, init]) => url === "/vocab" && init?.method === "POST");
    expect(JSON.parse(post[1].body)).toMatchObject({ word: "front desk", example: null, questionId: 1 });
  });
});

describe("Result review", () => {
  const grouped = () => attemptDetail({
    status: "SUBMITTED",
    questions: questionsFixture().slice(0, 3).map((question, index) => ({
      ...question,
      correctOption: question.options[0],
      groupId: index < 2 ? 31 : 32,
      groupType: "PASSAGE",
      groupContent: index < 2 ? "Shared passage about the new office." : "A second passage about travel.",
    })),
  });
  const result = () => ({ id: 7, status: "SUBMITTED", score: 0, totalQuestions: 3, answers: [] });

  it("shows the shared passage once per group, and lets the learner select words in it", async () => {
    routeFetch({ "GET /attempts/7": () => jsonResponse(200, grouped()) });
    renderApp("/result/7", { result: result() });

    expect(await screen.findByText("Shared passage about the new office.")).toBeInTheDocument();
    expect(screen.getAllByText("Shared passage about the new office.")).toHaveLength(1);
    expect(screen.getAllByText("A second passage about travel.")).toHaveLength(1);

    await selectUntil("office", () => screen.getByText("Shared passage about the new office.").firstChild,
      () => expect(screen.getByRole("button", { name: 'Save "office"' })).toBeInTheDocument());
  });
});
