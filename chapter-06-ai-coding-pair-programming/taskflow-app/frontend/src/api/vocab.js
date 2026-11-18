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

// The flashcards due now (the server limits the deck), plus the total number of due words.
export function getReviewDeck(token, limit) {
  return request(`/vocab/review${limit ? `?limit=${limit}` : ""}`, { token });
}

// remembered = true moves the card up a box; false sends it back to box 1.
export function reviewWord(token, id, remembered) {
  return request(`/vocab/${id}/review`, { method: "POST", token, body: { remembered } });
}
