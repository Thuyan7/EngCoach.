import { useState } from "react";
import { Link, Navigate, useNavigate } from "react-router-dom";
import { register } from "../api/auth.js";
import { useAuth } from "../auth/AuthContext.jsx";
import AuthLayout from "../components/AuthLayout.jsx";
import FormField from "../components/FormField.jsx";

const EMAIL_TAKEN = "An account with this email already exists.";
const UNREACHABLE = "We couldn't reach the server. Please try again.";

export default function RegisterPage() {
  const { isAuthenticated, signIn } = useAuth();
  const navigate = useNavigate();
  const [email, setEmail] = useState("");
  const [password, setPassword] = useState("");
  const [formError, setFormError] = useState(null);
  const [fieldErrors, setFieldErrors] = useState({});
  const [submitting, setSubmitting] = useState(false);

  if (isAuthenticated) return <Navigate to="/dashboard" replace />;

  async function handleSubmit(event) {
    event.preventDefault();
    setFormError(null);
    setFieldErrors({});
    setSubmitting(true);
    try {
      // 201 = account created and already logged in (feature-spec §1).
      const authResponse = await register(email, password);
      signIn(authResponse);
      navigate("/dashboard", { replace: true });
    } catch (err) {
      if (err.status === 409) {
        // Per design brief §5, registration may say the email is taken.
        setFieldErrors({ email: EMAIL_TAKEN });
      } else if (err.status === 422) {
        // Inline validation straight from the API (AC-001.2); keys are the
        // request field names, so they line up with the inputs.
        setFieldErrors(err.fieldErrors);
      } else {
        setFormError(UNREACHABLE);
      }
      setSubmitting(false);
    }
  }

  return (
    <AuthLayout
      title="Create your account"
      subtitle="Start practicing TOEIC in a minute."
      footer={
        <p className="login-foot">
          Already have an account? <Link to="/login">Log in</Link>
        </p>
      }
    >
      <form onSubmit={handleSubmit} noValidate aria-busy={submitting}>
        {formError && (
          <div className="error-msg" role="alert">
            {formError}
          </div>
        )}
        <FormField
          id="register-email"
          label="Email"
          type="email"
          autoComplete="email"
          placeholder="you@example.com"
          value={email}
          onChange={(e) => setEmail(e.target.value)}
          error={fieldErrors.email}
        />
        <FormField
          id="register-password"
          label="Password"
          type="password"
          autoComplete="new-password"
          value={password}
          onChange={(e) => setPassword(e.target.value)}
          error={fieldErrors.password}
        />
        <button type="submit" className="btn btn-primary btn-block" disabled={submitting}>
          {submitting ? "Creating account…" : "Create account"}
        </button>
      </form>
    </AuthLayout>
  );
}
