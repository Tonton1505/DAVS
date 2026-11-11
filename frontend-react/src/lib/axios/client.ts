import axios, { type AxiosResponse, type InternalAxiosRequestConfig } from "axios";
import { MOCK_DATA } from "../../services/auth.service";

const client = axios.create({ baseURL: "/api/v1", timeout: 10000 });

client.interceptors.request.use((config) => {
  const token = localStorage.getItem("davs-token");
  if (token) {
    config.headers.Authorization = `Bearer ${token}`;
  }
  return config;
});

client.interceptors.response.use(
  (response) => response,
  (error) => {
    const status = error?.response?.status;
    const errorCode = error?.response?.data?.code;

    // Handle session expired or unauthorized
    if (status === 401 || errorCode === "ERR-AUTH-05") {
      localStorage.removeItem("davs-token");
      localStorage.removeItem("davs-role");
      if (typeof window !== "undefined" && !window.location.pathname.startsWith("/login")) {
        const redirect = encodeURIComponent(window.location.pathname + window.location.search);
        window.location.href = `/login?error=ERR-AUTH-05&redirect=${redirect}`;
      }
    }
    return Promise.reject(error);
  },
);

// Fallback mock adapter for standalone demo mode when backend is unreachable
client.defaults.adapter = async (config: InternalAxiosRequestConfig) => {
  await new Promise((resolve) => setTimeout(resolve, 150));
  const path = (config.url || "").replace(/^\/api(\/v1)?/, "");
  const data = MOCK_DATA[path] ?? { ok: true };
  return { data, status: 200, statusText: "OK", headers: {}, config } as AxiosResponse;
};

export default client;
