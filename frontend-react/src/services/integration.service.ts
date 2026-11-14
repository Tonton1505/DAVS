import client from "../lib/axios/client";

export interface IntegrationDto {
  platform: "SLACK" | "SALESFORCE" | string;
  displayName: string;
  description: string;
  status: "CONNECTED" | "DISCONNECTED" | "RECONNECT_REQUIRED";
  scopes: string;
  accountName: string | null;
  hasToken: boolean;
  tokenExpiresAt: string | null;
  connectedAt: string | null;
  connectedBy: string | null;
  updatedAt: string;
}

export interface IntegrationConnectResponse {
  platform: string;
  authorizeUrl: string;
  state: string;
  scopes: string;
}

export const integrationService = {
  async list(): Promise<IntegrationDto[]> {
    const { data } = await client.get<IntegrationDto[]>("/v1/integrations");
    return data;
  },

  async initiateConnect(platform: string): Promise<IntegrationConnectResponse> {
    const { data } = await client.post<IntegrationConnectResponse>(
      `/v1/integrations/${platform}/connect`,
    );
    return data;
  },

  async handleCallback(
    platform: string,
    params: { code?: string; state?: string; error?: string; accountName?: string },
  ): Promise<IntegrationDto> {
    const { data } = await client.get<IntegrationDto>(`/v1/integrations/${platform}/callback`, {
      params,
    });
    return data;
  },

  async disconnect(platform: string): Promise<IntegrationDto> {
    const { data } = await client.delete<IntegrationDto>(`/v1/integrations/${platform}`);
    return data;
  },
};
