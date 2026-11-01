import { screen } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { beforeEach, describe, expect, it } from "vitest";
import { attemptDetail, jsonResponse, renderApp, routeFetch, signIn } from "../test/helpers.jsx";

beforeEach(() => {
  window.localStorage.clear();
  signIn();
});

describe("Test catalog — Part selection and attempt start", () => {
  it("shows Test 01 with all seven Parts selected by default", async () => {
    routeFetch({});
    renderApp("/tests");

    expect(await screen.findByRole("heading", { name: "Test 01 · TOEIC Practice" })).toBeInTheDocument();
    expect(screen.getByText("7 of 7 Parts selected")).toBeInTheDocument();
    for (let part = 1; part <= 7; part += 1) {
      expect(screen.getByRole("checkbox", { name: new RegExp(`Part ${part}:`) })).toBeChecked();
    }
  });

  it("allows selecting only the Parts the learner wants", async () => {
    const user = userEvent.setup();
    routeFetch({});
    renderApp("/tests");

    await user.click(screen.getByRole("checkbox", { name: /Part 1:/ }));
    await user.click(screen.getByRole("checkbox", { name: /Part 2:/ }));
    await user.click(screen.getByRole("checkbox", { name: /Part 3:/ }));
    await user.click(screen.getByRole("button", { name: "Preview selection" }));

    expect(screen.getByText("4 of 7 Parts selected")).toBeInTheDocument();
    expect(screen.getByRole("heading", { name: "Your selected Parts" })).toBeInTheDocument();
    expect(screen.getByText("Short Talks")).toBeInTheDocument();
    expect(screen.getByText("Part 4")).toBeInTheDocument();
    expect(screen.queryByText("Part 1 — Photographs")).not.toBeInTheDocument();
  });

  it("select all and clear selection work, and an empty selection cannot be previewed", async () => {
    const user = userEvent.setup();
    routeFetch({});
    renderApp("/tests");

    await user.click(screen.getByRole("button", { name: "Clear selection" }));
    expect(screen.getByText("0 of 7 Parts selected")).toBeInTheDocument();
    expect(screen.getByRole("button", { name: "Preview selection" })).toBeDisabled();

    await user.click(screen.getByRole("button", { name: "Select all 7" }));
    expect(screen.getByText("7 of 7 Parts selected")).toBeInTheDocument();
  });

  it("starts a full timed mock test when all seven Parts are selected", async () => {
    const user = userEvent.setup();
    const fetchMock = routeFetch({
      "POST /attempts": () => jsonResponse(201, { id: 88, type: "MOCK_TEST", status: "IN_PROGRESS" }),
      "GET /attempts/88": () => jsonResponse(200, attemptDetail({
        id: 88,
        type: "MOCK_TEST",
        part: null,
        remainingSeconds: 7200,
      })),
    });
    renderApp("/tests");

    await user.click(screen.getByRole("button", { name: "Preview selection" }));
    await user.click(screen.getByRole("button", { name: "Start full test" }));

    expect(await screen.findByText("Time remaining: 120:00")).toBeInTheDocument();
    const post = fetchMock.mock.calls.find(([url, init]) => url === "/attempts" && init.method === "POST");
    expect(JSON.parse(post[1].body)).toEqual({ type: "MOCK_TEST" });
  });

  it("starts only the selected Parts for custom practice", async () => {
    const user = userEvent.setup();
    const fetchMock = routeFetch({
      "POST /attempts": () => jsonResponse(201, { id: 89, type: "PART_PRACTICE", part: "Part 2,Part 5", status: "IN_PROGRESS" }),
      "GET /attempts/89": () => jsonResponse(200, attemptDetail({
        id: 89,
        type: "PART_PRACTICE",
        part: "Part 2,Part 5",
      })),
    });
    renderApp("/tests");

    await user.click(screen.getByRole("checkbox", { name: /Part 1:/ }));
    await user.click(screen.getByRole("checkbox", { name: /Part 3:/ }));
    await user.click(screen.getByRole("checkbox", { name: /Part 4:/ }));
    await user.click(screen.getByRole("checkbox", { name: /Part 6:/ }));
    await user.click(screen.getByRole("checkbox", { name: /Part 7:/ }));
    await user.click(screen.getByRole("button", { name: "Preview selection" }));
    await user.click(screen.getByRole("button", { name: "Start selected Parts" }));

    expect(await screen.findByRole("heading", { name: "Part 2,Part 5 practice" })).toBeInTheDocument();
    const post = fetchMock.mock.calls.find(([url, init]) => url === "/attempts" && init.method === "POST");
    expect(JSON.parse(post[1].body)).toEqual({ type: "PART_PRACTICE", parts: ["Part 2", "Part 5"] });
  });

  it("shows a helpful error if the selected Parts are not available", async () => {
    const user = userEvent.setup();
    routeFetch({
      "POST /attempts": () => jsonResponse(422, { title: "Unprocessable Entity", detail: "No questions" }),
    });
    renderApp("/tests");

    await user.click(screen.getByRole("checkbox", { name: /Part 1:/ }));
    await user.click(screen.getByRole("button", { name: "Preview selection" }));
    await user.click(screen.getByRole("button", { name: "Start selected Parts" }));

    expect(await screen.findByRole("alert")).toHaveTextContent(/do not have questions yet/i);
  });
});
