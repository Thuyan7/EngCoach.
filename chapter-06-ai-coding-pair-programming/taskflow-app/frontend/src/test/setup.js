import "@testing-library/jest-dom/vitest";

// jsdom does not implement media playback; the pages call pause() when a question with audio is left.
// (The proxy test runs in the node environment, where there is no window.)
if (typeof window !== "undefined") window.HTMLMediaElement.prototype.pause = () => {};
