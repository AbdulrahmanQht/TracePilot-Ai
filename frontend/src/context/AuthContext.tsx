import { createContext, useContext, useEffect, useState, useCallback, type ReactNode, } from "react";
import { useQueryClient } from "@tanstack/react-query";
import { useLogin, useRegister, useLogout, useRefresh } from "../hooks/useAuth";
import { useCurrentUser, userKeys } from "../hooks/useUser";
import { setAccessToken, setUnauthorizedHandler } from "../api/client";
import type { LoginRequest, RegisterRequest } from "../schemas/auth-requests";
import type { UserProfileResponse } from "../schemas/user";

interface AuthContextValue {
  user: UserProfileResponse | undefined;
  isAuthenticated: boolean;
  isInitializing: boolean;
  login: (payload: LoginRequest) => Promise<void>;
  loginWithToken: (token: string) => Promise<void>;
  register: (payload: RegisterRequest) => Promise<void>;
  logout: () => Promise<void>;
}

const AuthContext = createContext<AuthContextValue | undefined>(undefined);

export function AuthProvider({ children }: { children: ReactNode }) {
  const queryClient = useQueryClient();
  const [isAuthenticated, setIsAuthenticated] = useState(false);
  const [isInitializing, setIsInitializing] = useState(true);

  const loginMutation = useLogin();
  const registerMutation = useRegister();
  const logoutMutation = useLogout();
  const refreshMutation = useRefresh();

  useEffect(() => {
    refreshMutation.mutate(undefined, {
      onSuccess: (data) => {
        setAccessToken(data.accessToken);
        setIsAuthenticated(true);
      },
      onError: () => {
        setAccessToken(null);
        setIsAuthenticated(false);
      },
      onSettled: () => setIsInitializing(false),
    });
    // run once on mount only
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, []);

  const clearSession = useCallback(() => {
    setAccessToken(null);
    setIsAuthenticated(false);
    queryClient.clear();
  }, [queryClient]);

  useEffect(() => {
    setUnauthorizedHandler(clearSession);
  }, [clearSession]);

  
  const { data: user } = useCurrentUser({ enabled: isAuthenticated });

  const login = useCallback(
    async (payload: LoginRequest) => {
      const data = await loginMutation.mutateAsync(payload);
      setAccessToken(data.accessToken);
      setIsAuthenticated(true);
      await queryClient.invalidateQueries({ queryKey: userKeys.current() });
    },
    [loginMutation, queryClient]
  );

  const register = useCallback(
    async (payload: RegisterRequest) => {
      const data = await registerMutation.mutateAsync(payload);
      setAccessToken(data.accessToken);
      setIsAuthenticated(true);
      await queryClient.invalidateQueries({ queryKey: userKeys.current() });
    },
    [registerMutation, queryClient]
  );
    
    const loginWithToken = useCallback(
    async (token: string) => {
        setAccessToken(token);
        setIsAuthenticated(true);
        await queryClient.invalidateQueries({ queryKey: userKeys.current() });
    },
    [queryClient]
    );

  const logout = useCallback(async () => {
    try {
      await logoutMutation.mutateAsync();
    } finally {
      clearSession();
    }
  }, [logoutMutation, clearSession]);

  return (
    <AuthContext.Provider value={{ user, isAuthenticated, isInitializing, login, register, logout, loginWithToken }}>
      {children}
    </AuthContext.Provider>
  );
}

/* eslint-disable react-refresh/only-export-components */
export function useAuthContext() {
  const ctx = useContext(AuthContext);
  if (!ctx) throw new Error("useAuthContext must be used within an AuthProvider");
  return ctx;
}