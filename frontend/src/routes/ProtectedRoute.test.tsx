import { describe, it, expect, vi, afterEach } from "vitest";
import { render, screen } from "@testing-library/react";
import { MemoryRouter, Routes, Route } from "react-router-dom";
import { ProtectedRoute } from "./ProtectedRoute";
import { PublicOnlyRoute } from "./PublicOnlyRoute";
import * as AuthContextModule from "../context/AuthContext";
import type { UserProfileResponse } from "@/schemas/user"; // wherever it lives

function createUser(
  role: UserProfileResponse["role"] = "USER"
): UserProfileResponse {
  return {
    id: "11111111-1111-1111-1111-111111111111",
    displayName: null,
    email: "u@x.com",
    role,
    verified: true,
    createdAt: "2026-01-01T00:00:00Z",
    auditCountToday: 0,
  };
}
function mockAuthContext(value: Partial<ReturnType<typeof AuthContextModule.useAuthContext>>) {
  vi.spyOn(AuthContextModule, "useAuthContext").mockReturnValue({
    user: undefined,
    isAuthenticated: false,
    isInitializing: false,
    login: vi.fn(),
    loginWithToken: vi.fn(),
    register: vi.fn(),
    logout: vi.fn(),
    ...value,
  });
}

function renderProtected(requiredRole?: "ADMIN") {
  const props = requiredRole ? { requiredRole } : {};
  return render(
    <MemoryRouter initialEntries={["/app/admin"]}>
      <Routes>
        <Route path="/login" element={<div>Login Page</div>} />
        <Route path="/app/submit" element={<div>Submit Page</div>} />
        <Route element={<ProtectedRoute {...props} />}>
          <Route path="/app/admin" element={<div>Admin Page</div>} />
        </Route>
      </Routes>
    </MemoryRouter>
  );
}

describe("ProtectedRoute", () => {
  afterEach(() => vi.restoreAllMocks());

  it("renders nothing while auth is initializing (no flash of login/content)", () => {
    mockAuthContext({ isInitializing: true });
    const { container } = renderProtected();
    expect(container).toBeEmptyDOMElement();
  });

  it("redirects to /login when not authenticated", () => {
    mockAuthContext({ isAuthenticated: false });
    renderProtected();
    expect(screen.getByText("Login Page")).toBeInTheDocument();
  });

  it("renders protected content when authenticated with no role requirement", () => {
    mockAuthContext({ isAuthenticated: true });
    renderProtected();
    expect(screen.getByText("Admin Page")).toBeInTheDocument();
  });

  it("redirects a non-admin authenticated user away from an ADMIN-only route", () => {
    mockAuthContext({
      isAuthenticated: true,
      user: createUser("USER"),
    });
    renderProtected("ADMIN");
    expect(screen.getByText("Submit Page")).toBeInTheDocument();
  });

  it("allows an ADMIN user onto an ADMIN-only route", () => {
    mockAuthContext({
      isAuthenticated: true,
      user: createUser("ADMIN"),
    });
    renderProtected("ADMIN");
    expect(screen.getByText("Admin Page")).toBeInTheDocument();
  });
});

describe("PublicOnlyRoute", () => {
  afterEach(() => vi.restoreAllMocks());

  function renderPublicOnly() {
    return render(
      <MemoryRouter initialEntries={["/login"]}>
        <Routes>
          <Route path="/app/submit" element={<div>Submit Page</div>} />
          <Route element={<PublicOnlyRoute />}>
            <Route path="/login" element={<div>Login Page</div>} />
          </Route>
        </Routes>
      </MemoryRouter>
    );
  }

  it("renders nothing while initializing", () => {
    mockAuthContext({ isInitializing: true });
    const { container } = renderPublicOnly();
    expect(container).toBeEmptyDOMElement();
  });

  it("shows the public page (login) when not authenticated", () => {
    mockAuthContext({ isAuthenticated: false });
    renderPublicOnly();
    expect(screen.getByText("Login Page")).toBeInTheDocument();
  });

  it("redirects an already-authenticated user away from the login page", () => {
    mockAuthContext({ isAuthenticated: true });
    renderPublicOnly();
    expect(screen.getByText("Submit Page")).toBeInTheDocument();
  });
});