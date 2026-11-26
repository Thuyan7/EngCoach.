import { beforeEach, describe, expect, it, vi } from "vitest";
import { act, screen, waitFor, within } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { jsonResponse, renderApp, routeFetch, signIn } from "../test/helpers.jsx";

beforeEach(() => {
  window.localStorage.clear();
  signIn();
});

const word = (id, text, meaning, extra = {}) => ({
  id, word: text, meaning, example: null, questionId: null, box: 1, learned: false, marked: false, reviewCount: 0,
  nextReviewAt: "2026-11-18T00:00:00Z", createdAt: "2026-11-18T00:00:00Z", ...extra,
});

const list = (words) => ({
  total: words.length,
  learnedCount: words.filter((w) => w.learned).length,
  learningCount: words.filter((w) => !w.learned).length,
  markedCount: words.filter((w) => w.marked).length,
  words,
});

describe("Vocabulary page", () => {
  it("lists the saved words with their state, and links to study them", async () => {
    routeFetch({ "GET /vocab": () => jsonResponse(200, list([word(1, "invoice", "hoa don", { example: "Send the invoice." }), word(2, "deadline", "han chot", { learned: true, box: 3 })])) });
    renderApp("/vocabulary");

    expect(await screen.findByText("2 words to study")).toBeInTheDocument();
    expect(screen.getByText(/You have 2 saved words\./)).toBeInTheDocument();
    expect(screen.getByText("invoice")).toBeInTheDocument();
    expect(screen.getByText("Send the invoice.")).toBeInTheDocument();
    expect(screen.getByText("Learned", { selector: "small" })).toBeInTheDocument();
    expect(screen.getByText("Still learning", { selector: "small" })).toBeInTheDocument();
    expect(screen.getByRole("link", { name: "Study flashcards" })).toHaveAttribute("href", "/vocabulary/review");
  });

  it("disables studying with no words and shows an empty state with no words", async () => {
    routeFetch({ "GET /vocab": () => jsonResponse(200, list([])) });
    renderApp("/vocabulary");

    expect(await screen.findByText("You have not saved any words yet.")).toBeInTheDocument();
    expect(screen.getByRole("button", { name: "Study flashcards" })).toBeDisabled();
  });

  it("saves a new word and reloads the list", async () => {
    const user = userEvent.setup();
    let saved = false;
    const fetchMock = routeFetch({
      "GET /vocab": () => jsonResponse(200, saved ? list([word(5, "refund", "hoan tien")]) : list([])),
      "POST /vocab": () => { saved = true; return jsonResponse(201, word(5, "refund", "hoan tien")); },
    });
    renderApp("/vocabulary");

    await user.type(await screen.findByLabelText("Word or phrase"), "refund");
    await user.type(screen.getByLabelText("Meaning"), "hoan tien");
    await user.click(screen.getByRole("button", { name: "Save word" }));

    expect(await screen.findByText("1 word to study")).toBeInTheDocument();
    expect(screen.getByLabelText("Word or phrase")).toHaveValue("");
    const post = fetchMock.mock.calls.find(([url, init]) => url === "/vocab" && init?.method === "POST");
    expect(JSON.parse(post[1].body)).toEqual({ word: "refund", meaning: "hoan tien", example: null, questionId: null });
  });

  it("asks for the word and meaning, and shows the server message for a duplicate", async () => {
    const user = userEvent.setup();
    routeFetch({
      "GET /vocab": () => jsonResponse(200, list([word(1, "invoice", "hoa don")])),
      "POST /vocab": () => jsonResponse(409, { detail: "You have already saved this word." }),
    });
    renderApp("/vocabulary");

    await user.click(await screen.findByRole("button", { name: "Save word" }));
    expect(await screen.findByRole("alert")).toHaveTextContent("Enter the word and its meaning.");

    await user.type(screen.getByLabelText("Word or phrase"), "Invoice");
    await user.type(screen.getByLabelText("Meaning"), "again");
    await user.click(screen.getByRole("button", { name: "Save word" }));
    expect(await screen.findByText("You have already saved this word.")).toBeInTheDocument();
  });

  it("edits a word in place", async () => {
    const user = userEvent.setup();
    let meaning = "hoa don";
    const fetchMock = routeFetch({
      "GET /vocab": () => jsonResponse(200, list([word(1, "invoice", meaning)])),
      "PUT /vocab/1": () => { meaning = "hoa don ban hang"; return jsonResponse(200, word(1, "invoice", meaning)); },
    });
    renderApp("/vocabulary");

    await user.click(await screen.findByRole("button", { name: "Edit invoice" }));
    const field = screen.getAllByLabelText("Meaning").find((input) => input.value === "hoa don");
    await user.clear(field);
    await user.type(field, "hoa don ban hang");
    await user.click(screen.getByRole("button", { name: "Save changes" }));

    expect(await screen.findByText("hoa don ban hang")).toBeInTheDocument();
    const put = fetchMock.mock.calls.find(([url, init]) => url === "/vocab/1" && init?.method === "PUT");
    expect(JSON.parse(put[1].body)).toEqual({ word: "invoice", meaning: "hoa don ban hang", example: null });
  });

  it("deletes a word after confirmation, and keeps it when the learner cancels", async () => {
    const user = userEvent.setup();
    let words = [word(1, "invoice", "hoa don")];
    routeFetch({
      "GET /vocab": () => jsonResponse(200, list(words)),
      "DELETE /vocab/1": () => { words = []; return new Response(null, { status: 204 }); },
    });
    const confirm = vi.spyOn(window, "confirm").mockReturnValueOnce(false).mockReturnValueOnce(true);
    renderApp("/vocabulary");

    await user.click(await screen.findByRole("button", { name: "Delete invoice" }));
    expect(screen.getByText("invoice")).toBeInTheDocument();

    await user.click(screen.getByRole("button", { name: "Delete invoice" }));
    expect(await screen.findByText("You have not saved any words yet.")).toBeInTheDocument();
    confirm.mockRestore();
  });

  it("shows an error with a retry button when the list cannot be loaded", async () => {
    routeFetch({ "GET /vocab": () => jsonResponse(500, { title: "boom" }) });
    renderApp("/vocabulary");

    expect(await screen.findByRole("alert")).toHaveTextContent("couldn't load your vocabulary");
    expect(within(document.body).getByRole("button", { name: "Try again" })).toBeInTheDocument();
  });

  describe("groups, stars and learned words", () => {
    const words = () => [
      word(1, "invoice", "hoa don"),
      word(2, "deadline", "han chot", { learned: true, marked: true }),
      word(3, "refund", "hoan tien", { marked: true }),
    ];

    it("filters the list by group and studies the chosen group", async () => {
      const user = userEvent.setup();
      routeFetch({ "GET /vocab": () => jsonResponse(200, list(words())) });
      renderApp("/vocabulary");

      expect(await screen.findByText("3 words to study")).toBeInTheDocument();
      expect(screen.getByRole("link", { name: "Study flashcards" })).toHaveAttribute("href", "/vocabulary/review");

      await user.click(screen.getByRole("button", { name: /^Starred/ }));
      expect(screen.getByText("2 words to study")).toBeInTheDocument();
      expect(screen.queryByText("invoice")).not.toBeInTheDocument();
      expect(screen.getByRole("link", { name: "Study flashcards" })).toHaveAttribute("href", "/vocabulary/review?scope=marked");

      await user.click(screen.getByRole("button", { name: /^Learned/ }));
      expect(screen.getByText("deadline")).toBeInTheDocument();
      expect(screen.queryByText("refund")).not.toBeInTheDocument();
      expect(screen.getByRole("link", { name: "Study flashcards" })).toHaveAttribute("href", "/vocabulary/review?scope=learned");
    });

    it("stars a word and marks it as learned without leaving the page", async () => {
      const user = userEvent.setup();
      let current = words();
      const fetchMock = routeFetch({
        "GET /vocab": () => jsonResponse(200, list(current)),
        "PATCH /vocab/1": (init) => {
          const flags = JSON.parse(init.body);
          current = current.map((w) => (w.id === 1 ? { ...w, ...flags } : w));
          return jsonResponse(200, current[0]);
        },
      });
      renderApp("/vocabulary");

      await user.click(await screen.findByRole("button", { name: "Star invoice for flashcards" }));
      expect(await screen.findByRole("button", { name: "Remove star from invoice" })).toHaveAttribute("aria-pressed", "true");
      await user.click(screen.getByRole("button", { name: "Mark invoice as learned" }));
      expect(await screen.findByRole("button", { name: "Mark invoice as still learning" })).toBeInTheDocument();

      const bodies = fetchMock.mock.calls.filter(([url, init]) => url === "/vocab/1" && init?.method === "PATCH").map(([, init]) => JSON.parse(init.body));
      expect(bodies).toEqual([{ marked: true }, { learned: true }]);
    });

    it("says so when a group is empty", async () => {
      const user = userEvent.setup();
      routeFetch({ "GET /vocab": () => jsonResponse(200, list([word(1, "invoice", "hoa don")])) });
      renderApp("/vocabulary");

      await user.click(await screen.findByRole("button", { name: /^Learned/ }));
      expect(screen.getByText("No words in this group yet.")).toBeInTheDocument();
      expect(screen.getByRole("button", { name: "Study flashcards" })).toBeDisabled();
    });
  });

  describe("add form", () => {
    const lookup = (w) => `GET /vocab/lookup?word=${w}&context=`;

    it("fills in the meaning and details while the word is typed, so only the word is needed", async () => {
      const user = userEvent.setup();
      const fetchMock = routeFetch({
        "GET /vocab": () => jsonResponse(200, list([])),
        [lookup("refund")]: () => jsonResponse(200, { meaning: "hoan tien", partOfSpeech: "noun", phonetic: "/rifund/" }),
        "POST /vocab": () => jsonResponse(201, word(5, "refund", "hoan tien")),
      });
      renderApp("/vocabulary");

      await user.type(await screen.findByLabelText("Word or phrase"), "refund");
      await waitFor(() => expect(screen.getByLabelText("Meaning")).toHaveValue("hoan tien"), { timeout: 4000 });
      expect(screen.getByText("noun")).toBeInTheDocument();
      expect(screen.getByText("/rifund/")).toBeInTheDocument();

      await user.click(screen.getByRole("button", { name: "Save word" }));
      await waitFor(() => expect(screen.getByLabelText("Word or phrase")).toHaveValue(""));
      const post = fetchMock.mock.calls.find(([url, init]) => url === "/vocab" && init?.method === "POST");
      expect(JSON.parse(post[1].body)).toMatchObject({ word: "refund", meaning: "hoan tien" });
      expect(screen.getByLabelText("Meaning")).toHaveValue("");
    });

    it("never overwrites a meaning the learner typed", async () => {
      const user = userEvent.setup();
      routeFetch({
        "GET /vocab": () => jsonResponse(200, list([])),
        [lookup("refund")]: () => jsonResponse(200, { meaning: "suggested", partOfSpeech: null, phonetic: null }),
      });
      renderApp("/vocabulary");

      await user.type(await screen.findByLabelText("Meaning"), "mine");
      await user.type(screen.getByLabelText("Word or phrase"), "refund");
      await act(async () => { await new Promise((resolve) => setTimeout(resolve, 1200)); });
      expect(screen.getByLabelText("Meaning")).toHaveValue("mine");
    });

    it("keeps the example sentence out of the way until it is asked for", async () => {
      const user = userEvent.setup();
      routeFetch({ "GET /vocab": () => jsonResponse(200, list([])) });
      renderApp("/vocabulary");

      await screen.findByLabelText("Word or phrase");
      expect(screen.queryByLabelText(/Example sentence/)).not.toBeInTheDocument();
      await user.click(screen.getByRole("button", { name: "+ Add an example sentence" }));
      expect(screen.getByLabelText(/Example sentence/)).toBeInTheDocument();
    });
  });
});
