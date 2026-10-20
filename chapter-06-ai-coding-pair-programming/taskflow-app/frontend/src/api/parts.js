import { request } from "./client.js";

// GET /parts -> 200 string[] (e.g. ["Part 1", "Part 5"]); an empty array
// means no questions exist yet and is not an error. 401 without a valid JWT.
export function listParts(token) {
  return request("/parts", { token });
}
