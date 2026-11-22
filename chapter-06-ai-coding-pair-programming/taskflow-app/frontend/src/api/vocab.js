import { request } from "./client.js";

// All saved words, newest first, with how many are due for review.
export function listWords(token) {
  return request("/vocab", { token });
}

// word and meaning are required; example and questionId are optional.
export function addWord(token, { word, meaning, example, questionId }) {
  return request("/vocab", { method: "POST", token, body: { word, meaning, example: example || null, questionId: questionId ?? null } });
}

export function updateWord(token, id, { word, meaning, example }) {
  return request(`/vocab/${id}`, { method: "PUT", token, body: { word, meaning, example: example || null } });
}

export function deleteWord(token, id) {
  return request(`/vocab/${id}`, { method: "DELETE", token });
}

// The flashcards to study, available at any time. scope is "all" (default), "learning", "learned" or "marked";
// total counts every word in that group while the server limits the deck.
export function getReviewDeck(token, { scope, limit } = {}) {
  const params = new URLSearchParams();
  if (scope && scope !== "all") params.set("scope", scope);
  if (limit) params.set("limit", String(limit));
  const query = params.toString();
  return request(`/vocab/review${query ? `?${query}` : ""}`, { token });
}

// Star a word for flashcards and/or mark it learned; a flag that is left out keeps its value.
export function flagWord(token, id, { learned, marked }) {
  return request(`/vocab/${id}`, { method: "PATCH", token, body: { learned, marked } });
}

// remembered = true moves the card up a box and counts it as learned; false sends it back to box 1 as still learning.
export function reviewWord(token, id, remembered) {
  return request(`/vocab/${id}/review`, { method: "POST", token, body: { remembered } });
}

// A suggested Vietnamese meaning for a selected word: { meaning, partOfSpeech, phonetic }, any of them may be null.
export function lookupWord(token, word, context) {
  return request(`/vocab/lookup?word=${encodeURIComponent(word)}&context=${encodeURIComponent(context || "")}`, { token });
}
