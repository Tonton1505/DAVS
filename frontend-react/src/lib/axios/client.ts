import axios, { type AxiosError, type AxiosRequestConfig } from "axios";
import { useAuthStore } from "../../stores/auth.store";
import { applyApiError, extractApiError } from "../api/error";

export { applyApiError, extractApiError };

export const MOCK_DEV_TOKEN = "mock-dev-token";

interface RetryableConfig extends AxiosRequestConfig {
  _retry?: boolean;
}

let refreshPromise: Promise<string | null> | null = null;

function readCsrfCookie(): string | null {
  if (typeof document === "undefined" || !document.cookie) {
    return null;
  }
  const match = document.cookie
    .split(";")
    .map((part) => part.trim())
    .find((part) => part.startsWith("XSRF-TOKEN="));
  if (!match) {
    return null;
  }
  try {
    return decodeURIComponent(match.slice("XSRF-TOKEN=".length));
  } catch {
    return match.slice("XSRF-TOKEN=".length);
  }
}

export function isOfflineOrMockError(err: unknown): boolean {
  const currentToken =
    useAuthStore.getState().accessToken ??
    (typeof localStorage !== "undefined" ? localStorage.getItem("davs-token") : null);
  if (currentToken === MOCK_DEV_TOKEN) {
    return true;
  }
  const parsed = extractApiError(err);
  if (parsed?.code === "BACKEND_OFFLINE") {
    return true;
  }
  if (parsed?.code && (parsed.code.startsWith("ERR-") || parsed.code.startsWith("DIS-"))) {
    return false;
  }
  if (err && typeof err === "object" && "response" in err) {
    const status = (err as { response?: { status?: number } }).response?.status;
    return !status || status === 502 || status === 503 || status === 504;
  }
  return true;
}

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
  xsrfCookieName: "XSRF-TOKEN",
  xsrfHeaderName: "X-XSRF-TOKEN",
});

client.interceptors.request.use((config) => {
  const token = useAuthStore.getState().accessToken ?? localStorage.getItem("davs-token");
  if (token && token !== MOCK_DEV_TOKEN) {
    config.headers.Authorization = `Bearer ${token}`;
  }
  const csrf = readCsrfCookie();
  if (csrf) {
    config.headers["X-XSRF-TOKEN"] = csrf;
  }
  return config;
});

async function refreshAccessToken(): Promise<string | null> {
  if (!refreshPromise) {
    refreshPromise = (async () => {
      try {
        const refreshToken =
          useAuthStore.getState().refreshToken ?? localStorage.getItem("davs-refresh-token");
        const csrf = readCsrfCookie();
        const response = await axios.post<{ accessToken?: string; refreshToken?: string }>(
          "/api/v1/auth/refresh",
          refreshToken ? { refreshToken } : {},
          {
            withCredentials: true,
            headers: csrf ? { "X-XSRF-TOKEN": csrf } : undefined,
          },
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
    const currentToken = useAuthStore.getState().accessToken ?? localStorage.getItem("davs-token");
    if (currentToken === MOCK_DEV_TOKEN) {
      return Promise.reject(error);
    }

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
