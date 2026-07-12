import { apiClient } from "./client";
import { AuthResponseSchema, type AuthResponse, } from "../schemas/auth";
import {
  RegisterRequestSchema, type RegisterRequest,
  LoginRequestSchema, type LoginRequest,
  ChangePasswordRequestSchema, type ChangePasswordRequest,
  ForgotPasswordRequestSchema,
  ResetPasswordRequestSchema,
  ResendVerificationRequestSchema,
} from "../schemas/auth-requests";

export async function register(payload: RegisterRequest): Promise<AuthResponse> {
  const body = RegisterRequestSchema.parse(payload);
  const { data } = await apiClient.post("/auth/register", body);
  return AuthResponseSchema.parse(data);
}

export async function login(payload: LoginRequest): Promise<AuthResponse> {
  const body = LoginRequestSchema.parse(payload);
  const { data } = await apiClient.post("/auth/login", body);
  return AuthResponseSchema.parse(data);
}

export async function refresh(): Promise<AuthResponse> {
  const { data } = await apiClient.post("/auth/refresh");
  return AuthResponseSchema.parse(data);
}

export async function logout(): Promise<void> {
  await apiClient.post("/auth/logout");
}

export async function verifyEmail(token: string): Promise<void> {
  await apiClient.get("/auth/verify-email", { params: { token } });
}

export async function resendVerification(email: string): Promise<void> {
  const body = ResendVerificationRequestSchema.parse({ email });
  await apiClient.post("/auth/resend-verification", body);
}

export async function forgotPassword(email: string): Promise<void> {
  const body = ForgotPasswordRequestSchema.parse({ email });
  await apiClient.post("/auth/forgot-password", body);
}

export async function resetPassword(token: string, newPassword: string): Promise<void> {
  const body = ResetPasswordRequestSchema.parse({ token, newPassword });
  await apiClient.post("/auth/reset-password", body);
}

export async function changePassword(payload: ChangePasswordRequest): Promise<void> {
  const body = ChangePasswordRequestSchema.parse(payload);
  await apiClient.post("/auth/change-password", body);
}