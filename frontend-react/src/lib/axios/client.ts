import axios, { type AxiosError, type AxiosRequestConfig } from "axios";
import { useAuthStore } from "../../stores/auth.store";
import { applyApiError, extractApiError } from "../api/error";

export { applyApiError, extractApiError };

interface RetryableConfig extends AxiosRequestConfig {
  _retry?: boolean;
}

let refreshPromise: Promise<string | null> | null = null;

export const authRedirect = {
  toLogin: () => {
    if (typeof window !== "undefined") {
      window.location.assign("/login?error=expired");
    }
  },
};

const client = axios.create({
  baseURL: "/api",
  timeout: 15_000,
  withCredentials: true,
});

client.interceptors.request.use((config) => {
  const token = useAuthStore.getState().accessToken ?? localStorage.getItem("davs-token");
  if (token) {
    config.headers.Authorization = `Bearer ${token}`;
  }
  return config;
});

async function refreshAccessToken(): Promise<string | null> {
  if (!refreshPromise) {
    refreshPromise = (async () => {
      try {
        const refreshToken =
          useAuthStore.getState().refreshToken ?? localStorage.getItem("davs-refresh-token");
        const response = await axios.post<{ accessToken?: string; refreshToken?: string }>(
          "/api/v1/auth/refresh",
          refreshToken ? { refreshToken } : {},
          { withCredentials: true },
        );
        const nextAccessToken = response.data?.accessToken ?? null;
        if (nextAccessToken) {
          useAuthStore.getState().setTokens(nextAccessToken, response.data?.refreshToken);
        }
        return nextAccessToken;
      } catch {
        return null;
      } finally {
        refreshPromise = null;
      }
    })();
  }
  return refreshPromise;
}

client.interceptors.response.use(
  (response) => response,
  async (error: AxiosError) => {
    const originalRequest = error.config as RetryableConfig | undefined;
    const status = error.response?.status;
    const apiError = extractApiError(error);
    const isAuthExpiredCode = apiError?.code === "ERR-AUTH-05";

    if (isAuthExpiredCode) {
      useAuthStore.getState().logout();
      authRedirect.toLogin();
      return Promise.reject(error);
    }

    if (
      status === 401 &&
      originalRequest &&
      !originalRequest._retry &&
      !String(originalRequest.url || "").includes("/auth/refresh") &&
      !String(originalRequest.url || "").includes("/auth/login")
    ) {
      originalRequest._retry = true;
      const newToken = await refreshAccessToken();
      if (newToken) {
        originalRequest.headers = {
          ...(originalRequest.headers ?? {}),
          Authorization: `Bearer ${newToken}`,
        };
        return client(originalRequest);
      }
      useAuthStore.getState().logout();
      authRedirect.toLogin();
    }

    return Promise.reject(error);
  },
);

export default client;
