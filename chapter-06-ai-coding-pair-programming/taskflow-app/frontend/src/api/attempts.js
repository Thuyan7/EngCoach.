import { request } from "./client.js";

// POST /attempts -> 201 {id,type,part,status,startedAt}. Resume semantics:
// an existing IN_PROGRESS attempt for the same Part is returned.
export function startPartPractice(token, part) {
  return request("/attempts", { method: "POST", token, body: { type: "PART_PRACTICE", part } });
}

export function getAttempt(token, attemptId) {
  return request(`/attempts/${attemptId}`, { token });
}

export function saveAnswer(token, attemptId, questionId, chosenOption) {
  return request(`/attempts/${attemptId}/answers/${questionId}`, {
    method: "PUT", token, body: { chosenOption },
  });
}

export function listAttempts(token) {
  return request("/attempts", { token });
}

export function deleteAttempt(token, attemptId) {
  return request(`/attempts/${attemptId}`, { method: "DELETE", token });
}

export function submitAttempt(token, attemptId) {
  return request(`/attempts/${attemptId}/submit`, { method: "POST", token });
}

export function retryExplanation(token, attemptId, questionId) {
  return request(`/attempts/${attemptId}/answers/${questionId}/retry-explanation`, { method: "POST", token });
}

export function startMockTest(token) {
  return request("/attempts", { method: "POST", token, body: { type: "MOCK_TEST" } });
}

// POST /attempts -> TEST attempt scoped to one Test and selected Parts.
export function startToeicTest(token, testId, parts) {
  return request("/attempts", { method: "POST", token, body: { type: "TEST", testId, parts } });
}
