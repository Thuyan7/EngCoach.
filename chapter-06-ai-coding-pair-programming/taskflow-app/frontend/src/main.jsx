import React from "react";
import { createRoot } from "react-dom/client";
import { BrowserRouter } from "react-router-dom";
import { AuthProvider } from "./auth/AuthContext.jsx";
import App from "./App.jsx";
import "./styles.css";
import "./theme.css";
import { applyTheme, currentTheme } from "./theme.js";

// Apply the saved or device theme before the first paint of the app, also on the login pages.
applyTheme(currentTheme());

createRoot(document.getElementById("root")).render(
  <React.StrictMode>
    <BrowserRouter future={{ v7_startTransition: true, v7_relativeSplatPath: true }}>
      <AuthProvider>
        <App />
      </AuthProvider>
    </BrowserRouter>
  </React.StrictMode>,
);
