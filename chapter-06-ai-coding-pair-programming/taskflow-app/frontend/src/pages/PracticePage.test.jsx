import { beforeEach, describe, expect, it, vi } from "vitest";
import { screen, waitFor } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { jsonResponse, renderApp, routeFetch, signIn } from "../test/helpers.jsx";

beforeEach(() => {
  window.localStorage.clear();
  signIn();
});

describe("Practice Test catalog", () => {
  it("loads Tests, selects one or more Parts, and starts only the selected Parts", async () => {
    const user = userEvent.setup();
    let postedBody;
    routeFetch({
      "GET /parts": () => jsonResponse(200, ["Part 1", "Part 2"]),
      "GET /tests": () => jsonResponse(200, [{
        id: 21,
        title: "TOEIC Practice Test 1",
        description: "Sample test",
        parts: [
          { part: "Part 1", questionCount: 6, durationSeconds: 360 },
          { part: "Part 2", questionCount: 25, durationSeconds: 600 },
          { part: "Part 3", questionCount: 39, durationSeconds: 1020 },
        ],
      }]),
      "POST /attempts": (init) => {
        postedBody = JSON.parse(init.body);
        return jsonResponse(201, { id: 88, type: "TEST" });
      },
      "GET /attempts/88": () => jsonResponse(200, {
        id: 88, type: "TEST", testId: 21, selectedParts: ["Part 1", "Part 3"],
        status: "IN_PROGRESS", remainingSeconds: 300, answers: [],
        questions: [{ id: 1, prompt: "Look at the picture.", options: ["A", "B", "C", "D"] }],
      }),
    });
    renderApp("/practice");

    expect(await screen.findByRole("heading", { name: "TOEIC Practice Test 1" })).toBeInTheDocument();
    expect(screen.getByText("3 Parts · 70 questions · 33 minutes")).toBeInTheDocument();

    await user.click(screen.getByLabelText(/Part 2/));
    await user.click(screen.getByRole("button", { name: "Start selected Parts" }));

    await waitFor(() => expect(postedBody).toEqual({ type: "TEST", testId: 21, parts: ["Part 1", "Part 3"] }));
    expect(await screen.findByRole("heading", { name: "TOEIC Test" })).toBeInTheDocument();
  });

  it("allows deselecting all Parts but prevents starting an empty selection", async () => {
    const user = userEvent.setup();
    routeFetch({
      "GET /parts": () => jsonResponse(200, []),
      "GET /tests": () => jsonResponse(200, [{
        id: 22, title: "Test 2", parts: [{ part: "Part 1", questionCount: 6, durationSeconds: 300 }],
      }]),
    });
    renderApp("/practice");

    await screen.findByRole("heading", { name: "Test 2" });
    await user.click(screen.getByLabelText(/Part 1/));
    expect(screen.getByRole("button", { name: "Start selected Parts" })).toBeDisabled();
    expect(screen.getByText("0 Parts · 0 questions · 0 minutes")).toBeInTheDocument();
  });

  it("selects every Part with the Select all control", async () => {
    const user = userEvent.setup();
    routeFetch({
      "GET /parts": () => jsonResponse(200, []),
      "GET /tests": () => jsonResponse(200, [{
        id: 23, title: "Test 3", parts: [
          { part: "Part 1", questionCount: 6, durationSeconds: 300 },
          { part: "Part 2", questionCount: 25, durationSeconds: 600 },
        ],
      }]),
    });
    renderApp("/practice");

    await screen.findByRole("heading", { name: "Test 3" });
    await user.click(screen.getByRole("button", { name: "Select all" }));
    expect(screen.getByLabelText(/Part 1/)).toBeChecked();
    expect(screen.getByLabelText(/Part 2/)).toBeChecked();
    expect(screen.getByText("2 Parts · 31 questions · 15 minutes")).toBeInTheDocument();
  });

  it("Start full test begins the Test with every one of its Parts, and Part-bank practice is gone", async () => {
    const user = userEvent.setup();
    let postedBody;
    routeFetch({
      "GET /tests": () => jsonResponse(200, [{
        id: 31, title: "TOEIC AI Practice Test 01", parts: [
          { part: "Part 1", questionCount: 6, durationSeconds: 180 },
          { part: "Part 2", questionCount: 25, durationSeconds: 540 },
        ],
      }]),
      "POST /attempts": (init) => { postedBody = JSON.parse(init.body); return jsonResponse(201, { id: 5, type: "TEST" }); },
      "GET /attempts/5": () => jsonResponse(200, {
        id: 5, type: "TEST", testId: 31, selectedParts: ["Part 1", "Part 2"], status: "IN_PROGRESS",
        remainingSeconds: 720, answers: [], questions: [{ id: 1, prompt: "q", options: ["A", "B", "C", "D"] }],
      }),
    });
    renderApp("/practice");

    expect(screen.queryByText("Practice by Part")).not.toBeInTheDocument();
    await user.click(await screen.findByRole("button", { name: "Start full test (2 Parts)" }));

    await waitFor(() => expect(postedBody).toEqual({ type: "TEST", testId: 31, parts: ["Part 1", "Part 2"] }));
  });

  it("shows an error, not an empty catalog, when /tests does not return a JSON list", async () => {
    globalThis.fetch = () => Promise.resolve(new Response("<!doctype html><html></html>", { status: 200 }));
    renderApp("/practice");

    expect(await screen.findByRole("alert")).toHaveTextContent("couldn't load the Tests");
    expect(screen.queryByText(/No TOEIC Tests are published yet/)).not.toBeInTheDocument();
  });
});
