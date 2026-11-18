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
  accessToken: string | null;
  expiresInSeconds: number;
  requiresTermsAcceptance: boolean;
  mustChangePassword: boolean;
  user: AuthUserDto;
  mfaRequired?: boolean;
  mfaToken?: string | null;
}

export interface SsoConfigResponse {
  oidcEnabled: boolean;
  providerName: string;
  mfaEnabled: boolean;
  mfaRequired: boolean;
}

export interface OidcAuthorizeResponse {
  authorizationUrl: string;
  state: string;
}

export interface MfaStatusResponse {
  mfaEnabled: boolean;
  mfaRequired: boolean;
}

export interface MfaSetupResponse {
  secret: string;
  otpauthUri: string;
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
  async getSsoConfig(): Promise<SsoConfigResponse> {
    const { data } = await client.get<SsoConfigResponse>("/v1/auth/sso/config");
    return data;
  },
  async getOidcAuthorizeUrl(redirectUri?: string): Promise<OidcAuthorizeResponse> {
    const { data } = await client.get<OidcAuthorizeResponse>("/v1/auth/oidc/authorize", {
      params: redirectUri ? { redirectUri } : undefined,
    });
    return data;
  },
  async oidcCallback(payload: {
    code: string;
    state: string;
    redirectUri?: string;
  }): Promise<AuthTokenResponse> {
    const { data } = await client.post<AuthTokenResponse>("/v1/auth/oidc/callback", payload);
    return data;
  },
  async verifyMfa(mfaToken: string, code: string): Promise<AuthTokenResponse> {
    const { data } = await client.post<AuthTokenResponse>("/v1/auth/mfa/verify", {
      mfaToken,
      code,
    });
    return data;
  },
  async getMfaStatus(): Promise<MfaStatusResponse> {
    const { data } = await client.get<MfaStatusResponse>("/v1/auth/mfa/status");
    return data;
  },
  async setupMfa(): Promise<MfaSetupResponse> {
    const { data } = await client.post<MfaSetupResponse>("/v1/auth/mfa/setup", {});
    return data;
  },
  async enableMfa(code: string): Promise<MfaStatusResponse> {
    const { data } = await client.post<MfaStatusResponse>("/v1/auth/mfa/enable", { code });
    return data;
  },
  async disableMfa(code: string): Promise<MfaStatusResponse> {
    const { data } = await client.post<MfaStatusResponse>("/v1/auth/mfa/disable", { code });
    return data;
  },
};
