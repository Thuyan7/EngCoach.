// Thin fetch wrapper for the EngCoach API (openapi.yaml, Chapter 5).
// Every failure becomes an ApiError so screens branch on `status` instead of
// parsing responses themselves. status 0 means "no HTTP response at all"
// (network down, server unreachable).

const BASE_URL = import.meta.env.VITE_API_BASE_URL ?? "";

export class ApiError extends Error {
  constructor(status, problem) {
    super(problem?.detail || `Request failed with status ${status}`);
    this.name = "ApiError";
    this.status = status;
    // RFC 9457 body when the server sent one (type/title/status/detail,
    // plus `fieldErrors` on 422).
    this.problem = problem ?? null;
  }

  get fieldErrors() {
    return this.problem?.fieldErrors ?? {};
  }
}

export async function request(path, { method = "GET", body, token } = {}) {
  const headers = {};
  if (body !== undefined) headers["Content-Type"] = "application/json";
  if (token) headers["Authorization"] = `Bearer ${token}`;

  let response;
  try {
    response = await fetch(`${BASE_URL}${path}`, {
      method,
      headers,
      body: body !== undefined ? JSON.stringify(body) : undefined,
    });
  } catch {
    throw new ApiError(0, null);
  }

  const text = await response.text();
  let data = null;
  if (text) {
    try {
      data = JSON.parse(text);
    } catch {
      data = null;
    }
  }

  if (!response.ok) throw new ApiError(response.status, data);
  return data;
}
