import { describe, expect, it } from "vitest";
import { cleanSelection, dictionaryUrl, sentenceAround } from "./vocabSelection.js";

describe("cleanSelection", () => {
  it("tidies whitespace and trims", () => {
    expect(cleanSelection("  sales \n  figures ")).toBe("sales figures");
  });

  it("rejects empty and over-long selections", () => {
    expect(cleanSelection("   ")).toBe("");
    expect(cleanSelection(null)).toBe("");
    expect(cleanSelection("x".repeat(101))).toBe("");
    expect(cleanSelection("x".repeat(100))).toBe("x".repeat(100));
  });
});

describe("sentenceAround", () => {
  const text = "The meeting starts at nine. Please bring the quarterly invoice! Questions are welcome?";

  it("returns the sentence that contains the selection", () => {
    expect(sentenceAround(text, "invoice")).toBe("Please bring the quarterly invoice!");
    expect(sentenceAround(text, "meeting")).toBe("The meeting starts at nine.");
    expect(sentenceAround(text, "welcome")).toBe("Questions are welcome?");
  });

  it("ignores case and line breaks", () => {
    expect(sentenceAround("First line.\nSecond   line here.", "LINE HERE")).toBe("Second line here.");
  });

  it("finds a phrase", () => {
    expect(sentenceAround(text, "quarterly invoice")).toBe("Please bring the quarterly invoice!");
  });

  it("returns an empty string when the selection is missing or the sentence is too long", () => {
    expect(sentenceAround(text, "refund")).toBe("");
    expect(sentenceAround(text, "")).toBe("");
    expect(sentenceAround(`${"word ".repeat(120)}end.`, "end")).toBe("");
  });
});

describe("dictionaryUrl", () => {
  it("builds a lower-case, encoded dictionary link", () => {
    expect(dictionaryUrl("Invoice")).toBe("https://dictionary.cambridge.org/dictionary/english-vietnamese/invoice");
    expect(dictionaryUrl("sales figures")).toContain("sales%20figures");
  });
});
