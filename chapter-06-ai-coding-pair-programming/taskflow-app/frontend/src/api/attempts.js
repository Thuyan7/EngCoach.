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

// DELETE /attempts/{id} -> 204. Only the caller's own attempt can be deleted.
export function deleteAttempt(token, attemptId) {
  return request(`/attempts/${attemptId}`, { method: "DELETE", token });
}

// POST /attempts/{id}/submit -> 200 {id,status:"SUBMITTED",score,totalQuestions,
// answers:[{questionId,chosenOption,isCorrect,explanationStatus,explanationText}]}
// (S8a/S8b). Idempotent: submitting an already-submitted attempt returns the
// stored result without regrading. Grading + AI explanations can take a while.
// `answers` only lists questions that have a saved answer.
export function submitAttempt(token, attemptId) {
  return request(`/attempts/${attemptId}/submit`, { method: "POST", token });
}

// POST /attempts/{id}/answers/{questionId}/retry-explanation -> 200
// {questionId, explanationStatus: "OK"|"UNAVAILABLE", explanationText}.
// 409 when the attempt isn't submitted or that explanation isn't UNAVAILABLE.
export function retryExplanation(token, attemptId, questionId) {
  return request(`/attempts/${attemptId}/answers/${questionId}/retry-explanation`, {
    method: "POST",
    token,
  });
}
