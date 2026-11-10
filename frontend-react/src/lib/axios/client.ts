import axios, { type AxiosResponse, type InternalAxiosRequestConfig } from "axios";
import { MOCK_DATA } from "../../services/auth.service";

const client = axios.create({ baseURL: "/api", timeout: 5000 });
client.interceptors.request.use((config) => {
  const token = localStorage.getItem("davs-token");
  if (token) config.headers.Authorization = `Bearer ${token}`;
  return config;
});
client.interceptors.response.use(
  (r) => r,
  (error) => {
    if (error?.response?.status === 401) {
      localStorage.removeItem("davs-token");
      window.location.href = "/login?error=expired";
    }
    return Promise.reject(error);
  },
);
client.defaults.adapter = async (config: InternalAxiosRequestConfig) => {
  await new Promise((resolve) => setTimeout(resolve, 300));
  const path = (config.url || "").replace("/api", "");
  const data = MOCK_DATA[path] ?? { ok: true };
  return { data, status: 200, statusText: "OK", headers: {}, config } as AxiosResponse;
};
export default client;
