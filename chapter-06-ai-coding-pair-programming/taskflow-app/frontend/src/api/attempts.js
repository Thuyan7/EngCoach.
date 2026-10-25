import { request } from "./client.js";

// POST /attempts -> 201 {id,type,part,status,startedAt}. Resume semantics
// (S6a): an existing IN_PROGRESS attempt for the same Part comes back instead
// of a new one. 422 when the Part has no questions.
export function startPartPractice(token, part) {
  return request("/attempts", { method: "POST", token, body: { type: "PART_PRACTICE", part } });
}

// GET /attempts/{id} -> 200 {id,type,part,status,startedAt,remainingSeconds,
// answers:[{questionId,chosenOption}]} (S7a). 403 someone else's, 404 unknown.
export function getAttempt(token, attemptId) {
  return request(`/attempts/${attemptId}`, { token });
}

// PUT /attempts/{id}/answers/{questionId} -> 200 {questionId,chosenOption}.
// Re-answering overwrites the previous value.
export function saveAnswer(token, attemptId, questionId, chosenOption) {
  return request(`/attempts/${attemptId}/answers/${questionId}`, {
    method: "PUT",
    token,
    body: { chosenOption },
  });
}

// GET /attempts -> 200 [{id,type,part,status:"SUBMITTED",score,totalQuestions,
// startedAt}], most recent first (S10a). Only completed attempts appear.
export function listAttempts(token) {
  return request("/attempts", { token });
}
