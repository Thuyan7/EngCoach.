import { screen } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { beforeEach, describe, expect, it } from "vitest";
import { renderApp, routeFetch, signIn } from "../test/helpers.jsx";

beforeEach(() => {
  window.localStorage.clear();
  signIn();
});

describe("Test catalog — Part selection preview", () => {
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
    expect(screen.getByText(/This is a UI preview only/)).toBeInTheDocument();
    expect(screen.getByText("Part 4 — Short Talks")).toBeInTheDocument();
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

  it("preview never starts an attempt or calls the attempts API", async () => {
    const user = userEvent.setup();
    const fetchMock = routeFetch({});
    renderApp("/tests");

    await user.click(screen.getByRole("button", { name: "Preview selection" }));

    expect(screen.getByRole("heading", { name: "Your selected Parts" })).toBeInTheDocument();
    expect(fetchMock.mock.calls.some(([url]) => url === "/attempts" && fetchMock.mock.calls.some(([, options]) => options?.method === "POST"))).toBe(false);
  });
});
