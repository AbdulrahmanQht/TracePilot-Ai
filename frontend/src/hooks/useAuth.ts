import { useMutation, type UseMutationOptions } from "@tanstack/react-query";
import {
  register, login, refresh, logout, verifyEmail,
  resendVerification, forgotPassword, resetPassword, changePassword,
} from "../api/auth";
import type { AuthResponse } from "../schemas/auth";
import type { ApiError } from "../schemas/error";
import type { RegisterRequest, LoginRequest, ChangePasswordRequest } from "../schemas/auth-requests";

export function useRegister(options?: UseMutationOptions<AuthResponse, ApiError, RegisterRequest>) {
  return useMutation({ mutationFn: register, ...options });
}

export function useLogin(options?: UseMutationOptions<AuthResponse, ApiError, LoginRequest>) {
  return useMutation({ mutationFn: login, ...options });
}

export function useRefresh(options?: UseMutationOptions<AuthResponse, ApiError, void>) {
  return useMutation({ mutationFn: refresh, ...options });
}

export function useLogout(options?: UseMutationOptions<void, ApiError, void>) {
  return useMutation({ mutationFn: logout, ...options });
}

export function useVerifyEmail(options?: UseMutationOptions<void, ApiError, string>) {
  return useMutation({ mutationFn: verifyEmail, ...options });
}

export function useResendVerification(options?: UseMutationOptions<void, ApiError, string>) {
  return useMutation({ mutationFn: resendVerification, ...options });
}

export function useForgotPassword(options?: UseMutationOptions<void, ApiError, string>) {
  return useMutation({ mutationFn: forgotPassword, ...options });
}

export function useResetPassword(options?: UseMutationOptions<void, ApiError, { token: string; newPassword: string }>) {
  return useMutation({
    mutationFn: ({ token, newPassword }) => resetPassword(token, newPassword),
    ...options,
  });
}

export function useChangePassword(options?: UseMutationOptions<void, ApiError, ChangePasswordRequest>) {
  return useMutation({ mutationFn: changePassword, ...options });
}