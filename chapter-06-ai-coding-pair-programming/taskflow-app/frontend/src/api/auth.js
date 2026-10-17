import { request } from "./client.js";

// POST /auth/register -> 201 AuthResponse | 409 | 422
export function register(email, password) {
  return request("/auth/register", { method: "POST", body: { email, password } });
}

// POST /auth/login -> 200 AuthResponse | 401 (generic, no field disclosure)
export function login(email, password) {
  return request("/auth/login", { method: "POST", body: { email, password } });
}
