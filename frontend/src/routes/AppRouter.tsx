import { Suspense, lazy } from "react";
import { Routes, Route, Navigate } from "react-router-dom";
import { ProtectedRoute } from "./ProtectedRoute";
import { PublicOnlyRoute } from "./PublicOnlyRoute";

const LandingPage = lazy(() => import("@/pages/LandingPage"));
const LoginPage = lazy(() => import("@/pages/LoginPage"));
const RegisterPage = lazy(() => import("@/pages/RegisterPage"));
const ForgotPasswordPage = lazy(() => import("@/pages/ForgotPasswordPage"));
const ResetPasswordPage = lazy(() => import("@/pages/ResetPasswordPage"));
const OAuthRedirectPage = lazy(() => import("@/pages/OAuthRedirectPage"));
const SharedReportPage = lazy(() => import("@/pages/SharedReportPage"));
const NotFoundPage = lazy(() => import("@/pages/NotFoundPage"));
const EmailVerifiedPage = lazy(() => import("@/pages/EmailVerifiedPage"));


const AppShell = lazy(() => import("@/components/AppShell"));
const SubmitPage = lazy(() => import("@/pages/SubmitPage"));
const ProcessingPage = lazy(() => import("@/pages/ProcessingPage"));
const AuditDetailPage = lazy(() => import("@/pages/AuditDetailPage"));
const HistoryPage = lazy(() => import("@/pages/HistoryPage"));
const ReliabilityPage = lazy(() => import("@/pages/ReliabilityPage"));
const ProfilePage = lazy(() => import("@/pages/ProfilePage"));
const AdminPage = lazy(() => import("@/pages/AdminPage"));

export function AppRouter() {
return (
        <Suspense fallback={<div>Loading@.</div>}>
          <Routes>
            {/* Public, redirect away if already authenticated */}
            <Route element={<PublicOnlyRoute />}>
            <Route path="/" element={<LandingPage />} />
            <Route path="/login" element={<LoginPage />} />
            <Route path="/register" element={<RegisterPage />} />
            <Route path="/forgot-password" element={<ForgotPasswordPage />} />
            <Route path="/reset-password" element={<ResetPasswordPage />} />
            </Route>

            {/* Public, no auth*/}
            <Route path="/oauth2/redirect" element={<OAuthRedirectPage />} />
            <Route path="/email-verified" element={<EmailVerifiedPage />} />
            <Route path="/shared/:token" element={<SharedReportPage />} />

            {/* Authenticated app */}
            <Route element={<ProtectedRoute />}>
            <Route element={<AppShell />}>
              <Route path="/app" element={<Navigate to="/app/submit" replace />} />
              <Route path="/app/submit" element={<SubmitPage />} />
              <Route path="/app/audits/:id/processing" element={<ProcessingPage />} />
              <Route path="/app/audits/:id" element={<AuditDetailPage />} />
              <Route path="/app/history" element={<HistoryPage />} />
              <Route path="/app/reliability" element={<ReliabilityPage />} />
              <Route path="/app/profile" element={<ProfilePage />} />

              {/* Admin only */}
              <Route element={<ProtectedRoute requiredRole="ADMIN" />}>
                <Route path="/admin" element={<AdminPage />} />
              </Route>
            </Route>
          </Route>

            <Route path="*" element={<NotFoundPage />} />
          </Routes>
        </Suspense>
);
}