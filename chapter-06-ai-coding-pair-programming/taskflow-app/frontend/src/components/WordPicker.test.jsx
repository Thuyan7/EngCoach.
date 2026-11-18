import { afterEach, beforeEach, describe, expect, it, vi } from "vitest";
import { act, screen } from "@testing-library/react";
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

const passageNodes = () => screen.getAllByText(PASSAGE).map((paragraph) => paragraph.firstChild);

describe("Saving a selected word", () => {
  it("offers to save the selected word, pre-fills the sentence, and stores it with its question", async () => {
    const user = userEvent.setup();
    const fetchMock = routeFetch({
      "GET /attempts/mistakes": () => jsonResponse(200, mistakes),
      "POST /vocab": () => jsonResponse(201, { id: 1, word: "quarterly invoice", meaning: "hoa don hang quy" }),
    });
    renderApp("/mistakes");
    await screen.findAllByText(PASSAGE);

    select("quarterly invoice", passageNodes()[0]);
    expect(await screen.findByText("quarterly invoice", { selector: "strong" })).toBeInTheDocument();
    expect(screen.getByRole("link", { name: "Look it up" })).toHaveAttribute("href", expect.stringContaining("quarterly%20invoice"));

    await user.click(screen.getByRole("button", { name: 'Save "quarterly invoice"' }));
    expect(screen.getByLabelText("Word or phrase")).toHaveValue("quarterly invoice");
    expect(screen.getByLabelText(/Example sentence/)).toHaveValue("Please bring the quarterly invoice to the front desk.");
    expect(screen.getByLabelText("Meaning")).toHaveFocus();

    await user.type(screen.getByLabelText("Meaning"), "hoa don hang quy");
    await user.click(screen.getByRole("button", { name: "Save word" }));

    expect(await screen.findByText('Saved "quarterly invoice" to your vocabulary.')).toBeInTheDocument();
    const post = fetchMock.mock.calls.find(([url, init]) => url === "/vocab" && init?.method === "POST");
    expect(JSON.parse(post[1].body)).toEqual({
      word: "quarterly invoice",
      meaning: "hoa don hang quy",
      example: "Please bring the quarterly invoice to the front desk.",
      questionId: 1,
    });
  });

  it("shows the selection only on the card where the text was selected", async () => {
    routeFetch({ "GET /attempts/mistakes": () => jsonResponse(200, mistakes) });
    renderApp("/mistakes");
    await screen.findAllByText(PASSAGE);

    select("meeting", passageNodes()[1]);
    expect(await screen.findAllByText("meeting", { selector: "strong" })).toHaveLength(1);
    expect(screen.getAllByRole("button", { name: 'Save "meeting"' })).toHaveLength(1);
    expect(screen.getAllByText("Select a word or phrase in the text above to save it to your vocabulary.")).toHaveLength(1);
  });

  it("ignores a selection outside the cards, one that is empty and one that is too long", async () => {
    routeFetch({ "GET /attempts/mistakes": () => jsonResponse(200, mistakes) });
    renderApp("/mistakes");
    await screen.findAllByText(PASSAGE);

    select("meeting", document.body);
    select("   ", passageNodes()[0]);
    select("word ".repeat(30), passageNodes()[0]);
    expect(screen.queryByRole("group", { name: "Selected text" })).not.toBeInTheDocument();
  });

  it("keeps the selection when the browser clears it, and Dismiss removes it", async () => {
    const user = userEvent.setup();
    routeFetch({ "GET /attempts/mistakes": () => jsonResponse(200, { ...mistakes, mistakes: [mistakes.mistakes[0]] }) });
    renderApp("/mistakes");
    await screen.findAllByText(PASSAGE);

    select("meeting", passageNodes()[0]);
    expect(await screen.findByRole("button", { name: 'Save "meeting"' })).toBeInTheDocument();

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

    select("office", screen.getByText("Shared passage about the new office.").firstChild);
    expect(await screen.findByRole("button", { name: 'Save "office"' })).toBeInTheDocument();
  });
});
