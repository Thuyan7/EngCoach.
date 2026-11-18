import { beforeEach, describe, expect, it, vi } from "vitest";
import { screen, within } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { jsonResponse, renderApp, routeFetch, signIn } from "../test/helpers.jsx";

beforeEach(() => {
  window.localStorage.clear();
  signIn();
});

const word = (id, text, meaning, extra = {}) => ({
  id, word: text, meaning, example: null, questionId: null, box: 1, due: true, reviewCount: 0,
  nextReviewAt: "2026-11-18T00:00:00Z", createdAt: "2026-11-18T00:00:00Z", ...extra,
});

const list = (words, dueCount = words.filter((w) => w.due).length) => ({ total: words.length, dueCount, words });

describe("Vocabulary page", () => {
  it("lists the saved words and how many are due, with a link to study", async () => {
    routeFetch({ "GET /vocab": () => jsonResponse(200, list([word(1, "invoice", "hoa don", { example: "Send the invoice." }), word(2, "deadline", "han chot", { due: false, box: 3 })])) });
    renderApp("/vocabulary");

    expect(await screen.findByText("1 word due for review")).toBeInTheDocument();
    expect(screen.getByText("You have 2 saved words.")).toBeInTheDocument();
    expect(screen.getByText("invoice")).toBeInTheDocument();
    expect(screen.getByText("Send the invoice.")).toBeInTheDocument();
    expect(screen.getByText("Box 3 of 5")).toBeInTheDocument();
    expect(screen.getByRole("link", { name: "Study flashcards" })).toHaveAttribute("href", "/vocabulary/review");
  });

  it("disables studying when nothing is due and shows an empty state with no words", async () => {
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

    expect(await screen.findByText("1 word due for review")).toBeInTheDocument();
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
});
