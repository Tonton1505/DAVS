import type { APIRequestContext } from "@playwright/test";

export const DEFAULT_SEED_ADMIN = {
  email: process.env.ADMIN_EMAIL || "admin@davs.local",
  password: process.env.ADMIN_INITIAL_PASSWORD || "Admin@123456!ChangeMe",
};

export class ApiHelper {
  private readonly request: APIRequestContext;
  private readonly apiBaseUrl: string;
  private accessToken: string | null = null;

  constructor(
    request: APIRequestContext,
    apiBaseUrl = process.env.E2E_API_URL || "http://localhost:8080",
  ) {
    this.request = request;
    this.apiBaseUrl = apiBaseUrl.replace(/\/+$/, "");
  }

  async getHealth(): Promise<{ status: number; body: Record<string, unknown> }> {
    const res = await this.request.get(`${this.apiBaseUrl}/actuator/health`);
    return {
      status: res.status(),
      body: (await res.json().catch(() => ({}))) as Record<string, unknown>,
    };
  }

  async login(
    email = DEFAULT_SEED_ADMIN.email,
    password = DEFAULT_SEED_ADMIN.password,
  ): Promise<string | null> {
    const res = await this.request.post(`${this.apiBaseUrl}/api/v1/auth/login`, {
      data: { email, password },
    });
    if (!res.ok()) {
      return null;
    }
    const body = (await res.json()) as { accessToken?: string };
    this.accessToken = body.accessToken ?? null;
    return this.accessToken;
  }

  async createProject(name: string, description = "E2E project") {
    return this.request.post(`${this.apiBaseUrl}/api/v1/projects`, {
      headers: this.authHeaders(),
      data: { name, description },
    });
  }

  async updateSetting(key: string, value: string) {
    return this.request.put(`${this.apiBaseUrl}/api/v1/admin/settings/${encodeURIComponent(key)}`, {
      headers: this.authHeaders(),
      data: { value },
    });
  }

  private authHeaders(): Record<string, string> {
    return this.accessToken ? { Authorization: `Bearer ${this.accessToken}` } : {};
  }
}
