import { mockQuestionsFor } from "../mock/mockQuestions.js";

// THE SEAM for question content. Today: sample data (see mock/). Later: the
// real API will provide questions (either inside GET /attempts/{id} or from
// a dedicated endpoint — a decision still open). When it does, only this
// function changes; TestPage keeps calling it the same way.
export const QUESTIONS_ARE_SAMPLES = true;

export async function loadQuestions(_token, attempt) {
  return mockQuestionsFor(attempt.part);
}
