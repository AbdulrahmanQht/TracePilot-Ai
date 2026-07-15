import { Navigate, Outlet } from "react-router-dom";
import { useAuthContext } from "../context/AuthContext";

export function PublicOnlyRoute() {
  const { isAuthenticated, isInitializing } = useAuthContext();
  if (isInitializing) return null;
  if (isAuthenticated) return <Navigate to="/app/submit" replace />;
  return <Outlet />;
}