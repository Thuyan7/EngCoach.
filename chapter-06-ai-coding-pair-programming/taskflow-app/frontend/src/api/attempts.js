import { request } from "./client.js";

// POST /attempts. A selected Part set is stored as one practice attempt.
export function startPartPractice(token, parts) {
  return request("/attempts", {
    method: "POST",
    token,
    body: { type: "PART_PRACTICE", parts },
  });
}

// A full Listening + Reading run uses the backend's timed MOCK_TEST flow.
export function startMockTest(token) {
  return request("/attempts", {
    method: "POST",
    token,
    body: { type: "MOCK_TEST" },
  });
}

// GET /attempts/{id} -> attempt detail, saved answers and assigned questions.
export function getAttempt(token, attemptId) {
  return request(`/attempts/${attemptId}`, { token });
}

export function saveAnswer(token, attemptId, questionId, chosenOption) {
  return request(`/attempts/${attemptId}/answers/${questionId}`, {
    method: "PUT",
    token,
    body: { chosenOption },
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
  return request(`/attempts/${attemptId}/answers/${questionId}/retry-explanation`, {
    method: "POST",
    token,
  });
}
