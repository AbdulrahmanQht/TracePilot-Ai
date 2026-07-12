import axios, { AxiosError, type InternalAxiosRequestConfig } from "axios";
import { AuthResponseSchema } from "../schemas/auth";
import { parseApiError } from "../schemas/error";

const BASE_URL = import.meta.env.VITE_API_BASE_URL ?? "/api/v1";

let accessToken: string | null = null;
export function setAccessToken(token: string | null) {
  accessToken = token;
}
export function getAccessToken() {
  return accessToken;
}

export const apiClient = axios.create({
  baseURL: BASE_URL,
  withCredentials: true, // required: refresh token travels as an httpOnly cookie
});

apiClient.interceptors.request.use((config) => {
  if (accessToken) {
    config.headers.Authorization = `Bearer ${accessToken}`;
  }
  return config;
});


let refreshPromise: Promise<string> | null = null;

let onUnauthorized: (() => void) | null = null;
export function setUnauthorizedHandler(fn: () => void) {
  onUnauthorized = fn;
}

async function refreshAccessToken(): Promise<string> {
  const { data } = await axios.post(
    `${BASE_URL}/auth/refresh`,
    {},
    { withCredentials: true }
  );
  const parsed = AuthResponseSchema.parse(data);
  setAccessToken(parsed.accessToken);
  return parsed.accessToken;
}

apiClient.interceptors.response.use(
  (res) => res,
  async (error: AxiosError) => {
    const original = error.config as InternalAxiosRequestConfig & { _retry?: boolean };

    // Don't try to refresh the refresh call itself, or unauthenticated endpoints.
    const isAuthRoute = original?.url?.includes("/auth/");
    if (error.response?.status === 401 && !original._retry && !isAuthRoute) {
      original._retry = true;
      try {
        refreshPromise ??= refreshAccessToken().finally(() => {
          refreshPromise = null;
        });
        const token = await refreshPromise;
        original.headers.Authorization = `Bearer ${token}`;
        return apiClient(original);
      } catch {
        setAccessToken(null);
        onUnauthorized?.();
      }
    }

    return Promise.reject(parseApiError(error.response?.data));
  }
);