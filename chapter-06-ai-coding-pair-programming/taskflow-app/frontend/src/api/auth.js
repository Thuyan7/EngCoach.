import { request } from "./client.js";

// POST /auth/register -> 201 AuthResponse | 409 | 422
export function register(email, password) {
  return request("/auth/register", { method: "POST", body: { email, password } });
}

// POST /auth/login -> 200 AuthResponse | 401 (generic, no field disclosure)
export function login(email, password) {
  return request("/auth/login", { method: "POST", body: { email, password } });
}

// POST /auth/forgot-password -> 200 generic confirmation regardless of account existence.
export function forgotPassword(email) {
  return request("/auth/forgot-password", { method: "POST", body: { email } });
}

// POST /auth/reset-password -> 200 when the password has been changed.
export function resetPassword(token, newPassword) {
  return request("/auth/reset-password", { method: "POST", body: { token, newPassword } });
}
