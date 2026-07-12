import { Routes, Route, Navigate } from "react-router-dom";
import { ProtectedRoute } from "./ProtectedRoute";
import { PublicOnlyRoute } from "./PublicOnlyRoute";

import LandingPage from "../pages/LandingPage";
import LoginPage from "../pages/LoginPage";
import RegisterPage from "../pages/RegisterPage";
import OAuthRedirectPage from "../pages/OAuthRedirectPage";
import SharedReportPage from "../pages/SharedReportPage";
import NotFoundPage from "../pages/NotFoundPage";

import SubmitPage from "../pages/SubmitPage";
import ProcessingPage from "../pages/ProcessingPage";
import AuditDetailPage from "../pages/AuditDetailPage";
import HistoryPage from "../pages/HistoryPage";
import ReliabilityPage from "../pages/ReliabilityPage";
import ProfilePage from "../pages/ProfilePage";
import AdminPage from "../pages/AdminPage";

export function AppRouter() {
  return (
      <Routes>
        {/* Public, redirect away if already authenticated */}
        <Route element={<PublicOnlyRoute />}>
          <Route path="/" element={<LandingPage />} />
          <Route path="/login" element={<LoginPage />} />
          <Route path="/register" element={<RegisterPage />} />
        </Route>

        {/* Public, no auth*/}
        <Route path="/oauth2/redirect" element={<OAuthRedirectPage />} />
        <Route path="/shared/:token" element={<SharedReportPage />} />

        {/* Authenticated app */}
        <Route element={<ProtectedRoute />}>
          <Route path="/app" element={<Navigate to="/app/submit" replace />} />
          <Route path="/app/submit" element={<SubmitPage />} />
          <Route path="/app/audits/:id/processing" element={<ProcessingPage />} />
          <Route path="/app/audits/:id" element={<AuditDetailPage />} />
          <Route path="/app/history" element={<HistoryPage />} />
          <Route path="/app/reliability" element={<ReliabilityPage />} />
          <Route path="/app/profile" element={<ProfilePage />} />
        </Route>

        {/* Admin only */}
        <Route element={<ProtectedRoute requiredRole="ADMIN" />}>
          <Route path="/admin" element={<AdminPage />} />
        </Route>

        <Route path="*" element={<NotFoundPage />} />
      </Routes>
  );
}