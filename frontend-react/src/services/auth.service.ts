import type { Role } from "../common/constants/types";
import client from "../lib/axios/client";

export type BackendSiteRole = "SITE_ADMIN" | "CREATOR" | "EXPLORER" | "VIEWER";

export interface AuthUserDto {
  id: string;
  email: string;
  fullName: string | null;
  siteRole: BackendSiteRole;
  status: string;
  termsAccepted: boolean;
  mustChangePassword: boolean;
}

export interface AuthTokenResponse {
  accessToken: string;
  expiresInSeconds: number;
  requiresTermsAcceptance: boolean;
  mustChangePassword: boolean;
  user: AuthUserDto;
}

export interface LoginPayload {
  email: string;
  password: string;
  rememberMe: boolean;
}

export function mapSiteRoleToUiRole(siteRole: BackendSiteRole): Role {
  switch (siteRole) {
    case "SITE_ADMIN":
      return "admin";
    case "CREATOR":
      return "creator";
    case "EXPLORER":
      return "explorer";
    case "VIEWER":
    default:
      return "viewer";
  }
}

export const authApi = {
  async login(payload: LoginPayload): Promise<AuthTokenResponse> {
    const { data } = await client.post<AuthTokenResponse>("/v1/auth/login", payload);
    return data;
  },
  async acceptTerms(accepted: boolean): Promise<AuthUserDto> {
    const { data } = await client.post<AuthUserDto>("/v1/auth/accept-terms", { accepted });
    return data;
  },
  async refresh(): Promise<AuthTokenResponse> {
    const { data } = await client.post<AuthTokenResponse>("/v1/auth/refresh", {});
    return data;
  },
  async logout(): Promise<void> {
    await client.post("/v1/auth/logout", {});
  },
  async me(): Promise<AuthUserDto> {
    const { data } = await client.get<AuthUserDto>("/v1/auth/me");
    return data;
  },
};
