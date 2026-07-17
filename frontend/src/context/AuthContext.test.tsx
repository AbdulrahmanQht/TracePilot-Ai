import { describe, it, expect, beforeEach, afterEach, vi } from "vitest";
import { render, screen, waitFor } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import MockAdapter from "axios-mock-adapter";
import { apiClient, setAccessToken } from "../api/client";
import { AuthProvider, useAuthContext } from "./AuthContext";

const VALID_USER = {
  id: "11111111-1111-4111-8111-111111111111",
  email: "user@example.com",
  displayName: "Test User",
  role: "USER" as const,
  verified: true,
  createdAt: new Date().toISOString(),
  auditCountToday: 0,
};

function authResponse(accessToken: string, user = VALID_USER) {
  return { accessToken, tokenType: "Bearer", expiresAt: new Date().toISOString(), user };
}

function TestConsumer() {
  const { isAuthenticated, isInitializing, user, login, logout } = useAuthContext();
  if (isInitializing) return <div>initializing</div>;
  return (
    <div>
      <div data-testid="auth-state">{isAuthenticated ? "authenticated" : "anonymous"}</div>
      <div data-testid="user-email">{user?.email ?? "no-user"}</div>
      <button onClick={() => login({ email: "user@example.com", password: "password123" })}>
        login
      </button>
      <button onClick={() => logout()}>logout</button>
    </div>
  );
}

function renderWithProviders() {
  const queryClient = new QueryClient({ defaultOptions: { queries: { retry: false } } });
  return render(
    <QueryClientProvider client={queryClient}>
      <AuthProvider>
        <TestConsumer />
      </AuthProvider>
    </QueryClientProvider>
  );
}

describe("AuthProvider", () => {
  let mock: MockAdapter;

  beforeEach(() => {
    mock = new MockAdapter(apiClient);
    setAccessToken(null);
  });

  afterEach(() => {
    mock.restore();
    vi.restoreAllMocks();
  });

  it("starts as anonymous when the initial refresh (silent session check) fails with 401", async () => {
    mock.onPost("/auth/refresh").reply(401, { message: "no session" });

    renderWithProviders();

    await waitFor(() => expect(screen.getByTestId("auth-state")).toHaveTextContent("anonymous"));
  });

  it("restores an authenticated session when the initial refresh succeeds (page reload case)", async () => {
    mock.onPost("/auth/refresh").reply(200, authResponse("restored-token"));
    mock.onGet("/users/me").reply(200, VALID_USER);

    renderWithProviders();

    await waitFor(() => expect(screen.getByTestId("auth-state")).toHaveTextContent("authenticated"));
    await waitFor(() => expect(screen.getByTestId("user-email")).toHaveTextContent(VALID_USER.email));
  });

  it("logs in successfully and transitions from anonymous to authenticated", async () => {
    mock.onPost("/auth/refresh").reply(401, { message: "no session" });
    mock.onPost("/auth/login").reply(200, authResponse("login-token"));
    mock.onGet("/users/me").reply(200, VALID_USER);

    renderWithProviders();
    await waitFor(() => expect(screen.getByTestId("auth-state")).toHaveTextContent("anonymous"));

    await userEvent.click(screen.getByText("login"));

    await waitFor(() => expect(screen.getByTestId("auth-state")).toHaveTextContent("authenticated"));
  });

  it("logs out and clears authenticated state even if the logout API call fails, without throwing", async () => {
    mock.onPost("/auth/refresh").reply(200, authResponse("restored-token"));
    mock.onGet("/users/me").reply(200, VALID_USER);
    mock.onPost("/auth/logout").reply(500);

    renderWithProviders();
    await waitFor(() => expect(screen.getByTestId("auth-state")).toHaveTextContent("authenticated"));

    // userEvent.click awaits the onClick handler; if logout() rethrew, this would reject.
    await userEvent.click(screen.getByText("logout"));

    await waitFor(() => expect(screen.getByTestId("auth-state")).toHaveTextContent("anonymous"));
  });
});