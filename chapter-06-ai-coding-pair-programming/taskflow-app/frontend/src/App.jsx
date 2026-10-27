import { Navigate, Route, Routes } from "react-router-dom";
import AppShell from "./components/AppShell.jsx";
import ProtectedRoute from "./components/ProtectedRoute.jsx";
import DashboardPage from "./pages/DashboardPage.jsx";
import LoginPage from "./pages/LoginPage.jsx";
import ResultPage from "./pages/ResultPage.jsx";
import TestPage from "./pages/TestPage.jsx";
import RegisterPage from "./pages/RegisterPage.jsx";

export default function App() {
  return (
    <Routes>
      <Route path="/login" element={<LoginPage />} />
      <Route path="/register" element={<RegisterPage />} />
      <Route
        element={
          <ProtectedRoute>
            <AppShell />
          </ProtectedRoute>
        }
      >
        <Route path="/dashboard" element={<DashboardPage />} />
        {/* Not under /attempts: the dev proxy forwards /attempts/* to the API, so a refresh there would hit the backend, not this SPA. */}
        <Route path="/practice/:attemptId" element={<TestPage />} />
        <Route path="/result/:attemptId" element={<ResultPage />} />
      </Route>
      <Route path="*" element={<Navigate to="/dashboard" replace />} />
    </Routes>
  );
}
