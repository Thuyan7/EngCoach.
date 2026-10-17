# Slice S5b — Login and Register screens against the real API (frontend) — implementation notes

**Scope implemented:** implementation-plan.md Slice S5b — "Frontend: login
and registration screens work against the real API." Depends on S1, S2
(done). Definition of done: *submitting valid credentials on the real UI
stores a JWT and redirects to Dashboard; invalid credentials show the
generic error from the prototype.*

This is the first frontend slice, so it also creates the frontend module
(`taskflow-app/frontend`, sibling of `backend`). Nothing outside this new
folder was changed.

## What was built

- Vite + React 18 + React Router 6, plain JavaScript, Vitest +
  Testing Library. Architecture only fixes "React static build on Vercel";
  the toolchain is a choice made here (see "Decisions to confirm").
- `api/client.js` — `fetch` wrapper. Every failure becomes an `ApiError`
  (`status` 0 = no response; `problem` = the RFC 9457 body;
  `fieldErrors` = the 422 map). `api/auth.js` — `register()`, `login()`,
  field names exactly as in `openapi.yaml` and the backend DTOs.
- `auth/AuthContext.jsx` — holds the session `{accessToken, expiresAt}`,
  persists it in `localStorage`, discards it if already expired (ADR-003:
  3 hours, no refresh token).
- `LoginPage`, `RegisterPage` on a shared `AuthLayout` — the split layout,
  brand panel, tokens, button and field styles are ported from the
  Chapter 4 prototype (Concept C), not redesigned.
- `ProtectedRoute` + `/dashboard` placeholder; `/login` and `/register`
  redirect to `/dashboard` when a valid session exists.

## States covered (design brief §5)

| Screen | Loading | Error | Success |
|---|---|---|---|
| Login | button disabled, "Logging in…" | any 4xx → "Email or password is incorrect." (no field disclosure); no response / 5xx → "We couldn't reach the server…" | JWT stored, redirect to Dashboard |
| Register | button disabled, "Creating account…" | 409 → email field "already exists"; 422 → API `fieldErrors` inline per field; no response / 5xx → form-level alert | account created = auto-login, redirect to Dashboard |

Accessibility (design brief §7): real `<label for>`, error text linked
with `aria-describedby` + `aria-invalid`, `role="alert"` on form errors,
a real `<form>` so Enter submits, visible focus ring, reduced-motion rule.

## Decisions to confirm (proposed by the AI, not yet approved)

1. **Register screen is derived, not designed.** The prototype has a Login
   screen only, but the design brief (§4, §5) requires Register. It reuses
   the Login layout and components unchanged. The copy ("Create your
   account", "Start practicing TOEIC in a minute.", the two cross-links,
   "An account with this email already exists.") is AI-written.
2. **Session token in `localStorage`.** Chosen so a refresh keeps the
   session (S7b needs "refreshing mid-attempt restores the same state").
   Trade-off: readable by any script on the page (XSS). ADR-003 does not
   say where the client keeps the token.
3. **Toolchain:** Vite, React Router, plain JS (no TypeScript), Vitest.
4. **Dashboard placeholder + "Log out" button.** The plan only needs a
   redirect target; the real Dashboard is S6b. The placeholder shows no
   mock data. The Log out button is not in the plan; it exists so the
   session flow can be exercised by hand.
5. **Error copy for server/network failure** ("We couldn't reach the
   server. Please try again.") is not in the prototype.

## Known gaps / flagged, not silently decided

- **CORS (blocks production).** The backend has no CORS configuration.
  Local development is unaffected because the Vite dev server proxies
  `/auth`, `/parts`, `/attempts` to `localhost:8080`. A Vercel-hosted
  frontend calling the Railway backend is cross-origin and will be
  blocked until the backend allows that origin (and the frontend is built
  with `VITE_API_BASE_URL` set). Needs a deliberate backend change and a
  deployment decision, so it is not made here.
- **Vercel deep links.** A SPA needs a rewrite to `index.html` for URLs
  like `/register`; no `vercel.json` was added yet.
- **No forgot/reset password screens.** The backend has them (S4) and the
  design brief lists the screen, but `implementation-plan.md` has no
  frontend slice for it. "Forgot your password?" is plain, non-interactive
  text, exactly as in the prototype. Needs a slice added to the plan.
- **Prototype motion layer not ported** (per-letter heading animation,
  button ripple, screen-depth transitions, the prototype control bar).
  Kept: button hover/press and the reduced-motion rule. One layout
  deviation: `min-height: 100vh` instead of `100vh - 38px`, because the
  prototype's control bar is not part of the product.
- `npm install` printed audit advisories; not reviewed.

## API contract check

No change to `openapi.yaml`. Request and response shapes match the
backend (`AuthResponse`: `accessToken`, `tokenType`, `expiresAt`; 409/422
problem bodies; 422 `fieldErrors` keyed by request field name).

## Verification status

Run in this sandbox:
- `npm test` — 16 tests pass (7 Login, 5 Register, 4 session handling),
  with `fetch` mocked.
- `npm run build` — succeeds.
- Dev proxy — a `POST /auth/login` through the Vite server reached a stub
  server on :8080 with the body intact.

**NOT verified here:**
- Against the real Spring Boot backend (Maven Central is not reachable
  from this sandbox).
- Visually against the prototype in a browser. Per P6.3, that comparison
  is the human review gate.

To check locally, in two terminals:

```bash
# terminal 1 — backend
cd chapter-06-ai-coding-pair-programming/taskflow-app/backend/engcoach-backend
mvn spring-boot:run          # needs MySQL (see application.yml) running

# terminal 2 — frontend
cd chapter-06-ai-coding-pair-programming/taskflow-app/frontend
npm install
npm run dev                  # http://localhost:5173
npm test
```

Then: register a new email, confirm you land on the Dashboard placeholder
and `localStorage["engcoach.session"]` holds a token; log out; log in with
a wrong password and confirm the generic message; open the prototype
(`chapter-04.../prototype/index.html`) side by side and compare Login.
