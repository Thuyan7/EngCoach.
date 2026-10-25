import { describe, expect, it } from "vitest";
import { summarizeAttempts } from "./stats.js";

describe("summarizeAttempts", () => {
  it("no attempts: count 0, no average", () => {
    expect(summarizeAttempts([])).toEqual({ attemptCount: 0, averagePercent: null });
  });

  it("averages each attempt's percentage, not the raw counts", () => {
    const result = summarizeAttempts([
      { score: 7, totalQuestions: 10 }, // 70%
      { score: 5, totalQuestions: 10 }, // 50%
      { score: 9, totalQuestions: 12 }, // 75%
    ]);
    expect(result).toEqual({ attemptCount: 3, averagePercent: 65 });
  });

  it("a long attempt does not outweigh a short one", () => {
    // raw-count mean would be (1 + 100) / 2 questions-weighted ≠ 55%
    expect(
      summarizeAttempts([
        { score: 1, totalQuestions: 10 }, // 10%
        { score: 100, totalQuestions: 100 }, // 100%
      ]).averagePercent,
    ).toBe(55);
  });

  it("counts attempts with no usable total but leaves them out of the average", () => {
    const result = summarizeAttempts([
      { score: 8, totalQuestions: 10 },
      { score: 0, totalQuestions: 0 },
      { score: null, totalQuestions: null },
    ]);
    expect(result).toEqual({ attemptCount: 3, averagePercent: 80 });
  });

  it("only unscored attempts: counted, but no average", () => {
    expect(summarizeAttempts([{ score: 0, totalQuestions: 0 }])).toEqual({
      attemptCount: 1,
      averagePercent: null,
    });
  });

  it("rounds to a whole percent", () => {
    expect(summarizeAttempts([{ score: 2, totalQuestions: 3 }]).averagePercent).toBe(67);
  });
});
