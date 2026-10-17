import { createContext, useCallback, useContext, useMemo, useState } from "react";

// ADR-003: one access token, 3-hour expiry, no refresh token. The token is
// kept in localStorage so a page refresh keeps the session (required later by
// S7b: "refreshing mid-attempt restores the same state"). Trade-off flagged
// in SLICE-S5B-NOTES.md.
const STORAGE_KEY = "engcoach.session";

function readStoredSession() {
  try {
    const raw = window.localStorage.getItem(STORAGE_KEY);
    if (!raw) return null;
    const session = JSON.parse(raw);
    if (!session?.accessToken || !session?.expiresAt) return null;
    if (new Date(session.expiresAt).getTime() <= Date.now()) {
      window.localStorage.removeItem(STORAGE_KEY);
      return null;
    }
    return session;
  } catch {
    return null;
  }
}

function writeStoredSession(session) {
  try {
    if (session) window.localStorage.setItem(STORAGE_KEY, JSON.stringify(session));
    else window.localStorage.removeItem(STORAGE_KEY);
  } catch {
    // Storage unavailable (private mode, quota): the in-memory session still
    // works for this page load; it just won't survive a refresh.
  }
}

const AuthContext = createContext(null);

export function AuthProvider({ children }) {
  const [session, setSession] = useState(readStoredSession);

  // `authResponse` is the body of POST /auth/register or /auth/login.
  const signIn = useCallback((authResponse) => {
    const next = {
      accessToken: authResponse.accessToken,
      expiresAt: authResponse.expiresAt,
    };
    writeStoredSession(next);
    setSession(next);
  }, []);

  const signOut = useCallback(() => {
    writeStoredSession(null);
    setSession(null);
  }, []);

  const value = useMemo(
    () => ({
      token: session?.accessToken ?? null,
      isAuthenticated: session !== null,
      signIn,
      signOut,
    }),
    [session, signIn, signOut],
  );

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>;
}

export function useAuth() {
  const ctx = useContext(AuthContext);
  if (!ctx) throw new Error("useAuth must be used inside <AuthProvider>");
  return ctx;
}
