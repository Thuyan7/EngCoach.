import { beforeEach, describe, expect, it } from "vitest";
import { screen } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { jsonResponse, renderApp, routeFetch, signIn } from "../test/helpers.jsx";

beforeEach(() => {
  window.localStorage.clear();
  signIn();
});

const card = (id, text, meaning, example = null) => ({ id, word: text, meaning, example, box: 1, learned: false, marked: false });

describe("Flashcards page", () => {
  it("shows one card at a time, reveals the meaning, records each answer and ends with a summary", async () => {
    const user = userEvent.setup();
    const fetchMock = routeFetch({
      "GET /vocab/review": () => jsonResponse(200, { total: 2, cards: [card(1, "invoice", "hoa don", "Send the invoice."), card(2, "deadline", "han chot")] }),
      "POST /vocab/1/review": () => jsonResponse(200, card(1, "invoice", "hoa don")),
      "POST /vocab/2/review": () => jsonResponse(200, card(2, "deadline", "han chot")),
    });
    renderApp("/vocabulary/review");

    expect(await screen.findByText("Card 1 of 2")).toBeInTheDocument();
    expect(screen.getByText("invoice")).toBeInTheDocument();
    expect(screen.queryByText("hoa don")).not.toBeInTheDocument();
    expect(screen.getByRole("button", { name: "I remembered" })).toBeDisabled();

    await user.click(screen.getByRole("button", { name: /Show meaning/ }));
    expect(screen.getByText("hoa don")).toBeInTheDocument();
    expect(screen.getByText("Send the invoice.")).toBeInTheDocument();
    await user.click(screen.getByRole("button", { name: "I remembered" }));

    expect(await screen.findByText("Card 2 of 2")).toBeInTheDocument();
    expect(screen.queryByText("han chot")).not.toBeInTheDocument();
    await user.click(screen.getByRole("button", { name: /Show meaning/ }));
    await user.click(screen.getByRole("button", { name: "Still learning" }));

    expect(await screen.findByText("Session complete")).toBeInTheDocument();
    expect(screen.getByText("You remembered 1 and are still learning 1.")).toBeInTheDocument();
    expect(screen.getByRole("link", { name: "Back to vocabulary" })).toHaveAttribute("href", "/vocabulary");

    const bodies = fetchMock.mock.calls.filter(([, init]) => init?.method === "POST").map(([url, init]) => [url, JSON.parse(init.body)]);
    expect(bodies).toEqual([["/vocab/1/review", { remembered: true }], ["/vocab/2/review", { remembered: false }]]);
  });

  it("offers to study more when more words are in this group than the deck holds", async () => {
    const user = userEvent.setup();
    let calls = 0;
    routeFetch({
      "GET /vocab/review": () => { calls += 1; return jsonResponse(200, { total: calls === 1 ? 5 : 4, cards: [card(1, "alpha", "a")] }); },
      "POST /vocab/1/review": () => jsonResponse(200, card(1, "alpha", "a")),
    });
    renderApp("/vocabulary/review");

    await user.click(await screen.findByRole("button", { name: /Show meaning/ }));
    await user.click(screen.getByRole("button", { name: "I remembered" }));
    expect(await screen.findByText("4 more words are in this group.")).toBeInTheDocument();

    await user.click(screen.getByRole("button", { name: "Study more" }));
    expect(await screen.findByText("Card 1 of 1")).toBeInTheDocument();
  });

  it("says so when the group has no words", async () => {
    routeFetch({ "GET /vocab/review": () => jsonResponse(200, { total: 0, cards: [] }) });
    renderApp("/vocabulary/review");
    expect(await screen.findByText("You have no saved words to study yet.")).toBeInTheDocument();
  });

  it("keeps the card and shows an error when an answer cannot be saved", async () => {
    const user = userEvent.setup();
    routeFetch({
      "GET /vocab/review": () => jsonResponse(200, { total: 1, cards: [card(1, "alpha", "a")] }),
      "POST /vocab/1/review": () => jsonResponse(500, { title: "boom" }),
    });
    renderApp("/vocabulary/review");

    await user.click(await screen.findByRole("button", { name: /Show meaning/ }));
    await user.click(screen.getByRole("button", { name: "I remembered" }));
    expect(await screen.findByRole("alert")).toHaveTextContent("couldn't save your answer");
    expect(screen.getByText("Card 1 of 1")).toBeInTheDocument();
  });

  it("shows an error with a retry button when the deck cannot be loaded", async () => {
    routeFetch({ "GET /vocab/review": () => jsonResponse(500, { title: "boom" }) });
    renderApp("/vocabulary/review");
    expect(await screen.findByRole("alert")).toHaveTextContent("couldn't load your flashcards");
    expect(screen.getByRole("button", { name: "Try again" })).toBeInTheDocument();
  });

  it("studies the group named in the link, and shows which group it is", async () => {
    routeFetch({
      "GET /vocab/review?scope=learned": () => jsonResponse(200, { total: 1, cards: [card(3, "refund", "hoan tien", null)] }),
    });
    renderApp("/vocabulary/review?scope=learned");

    expect(await screen.findByText("Card 1 of 1")).toBeInTheDocument();
    expect(screen.getByText("Words you have learned")).toBeInTheDocument();
  });

  it("stars the current word for later", async () => {
    const user = userEvent.setup();
    const fetchMock = routeFetch({
      "GET /vocab/review": () => jsonResponse(200, { total: 1, cards: [card(1, "invoice", "hoa don")] }),
      "PATCH /vocab/1": () => jsonResponse(200, { ...card(1, "invoice", "hoa don"), marked: true }),
    });
    renderApp("/vocabulary/review");

    await user.click(await screen.findByRole("button", { name: "☆ Star this word" }));
    expect(await screen.findByRole("button", { name: "★ Starred" })).toHaveAttribute("aria-pressed", "true");
    const patch = fetchMock.mock.calls.find(([url, init]) => url === "/vocab/1" && init?.method === "PATCH");
    expect(JSON.parse(patch[1].body)).toEqual({ marked: true });
  });

  describe("keyboard", () => {
    const twoCards = () => routeFetch({
      "GET /vocab/review": () => jsonResponse(200, { total: 2, cards: [card(1, "invoice", "hoa don", "Send the invoice."), card(2, "deadline", "han chot")] }),
      "POST /vocab/1/review": () => jsonResponse(200, card(1, "invoice", "hoa don")),
      "POST /vocab/2/review": () => jsonResponse(200, card(2, "deadline", "han chot")),
    });

    it("flips the card with the space bar", async () => {
      const user = userEvent.setup();
      twoCards();
      renderApp("/vocabulary/review");
      await screen.findByText("Card 1 of 2");

      await user.keyboard(" ");
      expect(screen.getByText("hoa don")).toBeInTheDocument();
      expect(screen.queryByText("invoice")).not.toBeInTheDocument();
      await user.keyboard(" ");
      expect(screen.getByText("invoice")).toBeInTheDocument();
      expect(screen.queryByText("hoa don")).not.toBeInTheDocument();
    });

    it("moves between cards with the arrow keys and starts the next card on the word side", async () => {
      const user = userEvent.setup();
      twoCards();
      renderApp("/vocabulary/review");
      await screen.findByText("Card 1 of 2");

      await user.keyboard(" ");
      await user.keyboard("{ArrowRight}");
      expect(screen.getByText("Card 2 of 2")).toBeInTheDocument();
      expect(screen.getByText("deadline")).toBeInTheDocument();

      await user.keyboard("{ArrowLeft}");
      expect(screen.getByText("Card 1 of 2")).toBeInTheDocument();
      expect(screen.getByText("invoice")).toBeInTheDocument();
      await user.keyboard("{ArrowLeft}");
      expect(screen.getByText("Card 1 of 2")).toBeInTheDocument();
    });

    it("rates with 1 and 2 only after the card is flipped, and does not rate a card twice", async () => {
      const user = userEvent.setup();
      const fetchMock = twoCards();
      renderApp("/vocabulary/review");
      await screen.findByText("Card 1 of 2");

      await user.keyboard("2");
      expect(fetchMock.mock.calls.some(([, init]) => init?.method === "POST")).toBe(false);

      await user.keyboard(" ");
      await user.keyboard("2");
      expect(await screen.findByText("Card 2 of 2")).toBeInTheDocument();
      await user.keyboard("{ArrowLeft}");
      expect(screen.getByText("You remembered this word")).toBeInTheDocument();
      await user.keyboard(" ");
      await user.keyboard("1");
      const posts = fetchMock.mock.calls.filter(([, init]) => init?.method === "POST");
      expect(posts).toHaveLength(1);
    });

    it("lets a card be skipped and says so in the summary", async () => {
      const user = userEvent.setup();
      twoCards();
      renderApp("/vocabulary/review");
      await screen.findByText("Card 1 of 2");

      await user.keyboard("{ArrowRight}{ArrowRight}");
      expect(await screen.findByText("Session complete")).toBeInTheDocument();
      expect(screen.getByText("You skipped 2 cards.")).toBeInTheDocument();
    });

    it("ignores the shortcuts while typing", async () => {
      const user = userEvent.setup();
      twoCards();
      renderApp("/vocabulary/review");
      await screen.findByText("Card 1 of 2");
      const input = document.createElement("input");
      document.body.appendChild(input);
      input.focus();
      await user.keyboard(" ");
      expect(screen.getByText("invoice")).toBeInTheDocument();
      input.remove();
    });
  });
});
