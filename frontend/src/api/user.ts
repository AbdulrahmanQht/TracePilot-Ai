import { apiClient } from "./client";
import { UserProfileResponseSchema, type UserProfileResponse, UpdateUserRequestSchema, type UpdateUserRequest, } from "../schemas/user";

export async function getCurrentUser(): Promise<UserProfileResponse> {
  const { data } = await apiClient.get("/users/me");
  return UserProfileResponseSchema.parse(data);
}

export async function updateCurrentUser(payload: UpdateUserRequest): Promise<UserProfileResponse> {
  const body = UpdateUserRequestSchema.parse(payload);
  const { data } = await apiClient.patch("/users/me", body);
  return UserProfileResponseSchema.parse(data);
}

export async function deleteCurrentUser(): Promise<void> {
  await apiClient.delete("/users/me");
}