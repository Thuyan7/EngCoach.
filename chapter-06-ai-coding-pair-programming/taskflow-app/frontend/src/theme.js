// Light/dark theme. The choice is kept in localStorage; until the learner picks one, the device setting is used.
const KEY = "engcoach.theme";

function savedTheme() {
  try {
    const value = window.localStorage.getItem(KEY);
    return value === "dark" || value === "light" ? value : null;
  } catch {
    return null;
  }
}

function deviceTheme() {
  return window.matchMedia && window.matchMedia("(prefers-color-scheme: dark)").matches ? "dark" : "light";
}

export function currentTheme() {
  return savedTheme() || deviceTheme();
}

export function applyTheme(theme) {
  document.documentElement.dataset.theme = theme;
}

// Remember the learner's choice and apply it.
export function setTheme(theme) {
  try {
    window.localStorage.setItem(KEY, theme);
  } catch {
    // The choice just will not be remembered.
  }
  applyTheme(theme);
}
