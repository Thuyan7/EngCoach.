import "@testing-library/jest-dom/vitest";

// jsdom does not implement media playback; the pages call pause() when a question with audio is left.
window.HTMLMediaElement.prototype.pause = () => {};
