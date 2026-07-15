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

const PUBLIC_URL_PATTERNS = [
  /^\/shared\//,
  /^\/auth\/login$/,
  /^\/auth\/register$/,
  /^\/auth\/refresh$/,
  /^\/auth\/forgot-password$/,
  /^\/auth\/reset-password$/,
  /^\/auth\/verify-email$/,
  /^\/auth\/resend-verification$/,
];
 
function isPublicRoute(url: string | undefined): boolean {
  if (!url) return false;
  return PUBLIC_URL_PATTERNS.some((p) => p.test(url));
}

apiClient.interceptors.request.use((config) => {
  if (accessToken && !isPublicRoute(config.url)) {
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
    
    const isPublic = isPublicRoute(original?.url);
    if (error.response?.status === 401 && !original._retry && !isPublic) {
      original._retry = true;
      try {
        refreshPromise ??= refreshAccessToken().finally(() => {
          refreshPromise = null;
        });
        const token = await refreshPromise;
        original.headers.Authorization = `Bearer ${token}`;
        return apiClient(original);
      } catch (err) {
          const refreshError = err as AxiosError;

          if (refreshError.response?.status === 401) {
            setAccessToken(null);
            onUnauthorized?.();
          }

          throw refreshError;
        }
    }
 
    return Promise.reject(parseApiError(error.response?.data));
  }
);