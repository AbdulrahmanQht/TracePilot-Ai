import { useQuery, useMutation, type UseQueryOptions, type UseMutationOptions } from "@tanstack/react-query";
import { getCurrentUser, updateCurrentUser, deleteCurrentUser } from "../api/user";
import type { UserProfileResponse, UpdateUserRequest } from "../schemas/user";
import type { ApiError } from "../schemas/error";

export const userKeys = {
  all: ["user"] as const,
  current: () => [...userKeys.all, "me"] as const,
};

export function useCurrentUser(
  options?: Omit<UseQueryOptions<UserProfileResponse, ApiError>, "queryKey" | "queryFn">
) {
  return useQuery({
    queryKey: userKeys.current(),
    queryFn: getCurrentUser,
    ...options,
  });
}

export function useUpdateUser(
  options?: UseMutationOptions<UserProfileResponse, ApiError, UpdateUserRequest>
) {
  return useMutation({
    mutationFn: updateCurrentUser,
    ...options,
  });
}

export function useDeleteUser(
  options?: UseMutationOptions<void, ApiError, void>
) {
  return useMutation({
    mutationFn: deleteCurrentUser,
    ...options,
  });
}