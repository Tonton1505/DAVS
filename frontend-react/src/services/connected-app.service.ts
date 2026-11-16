import client from "../lib/axios/client";

export interface CreateConnectedAppRequest {
  name: string;
  trustType: "DIRECT_TRUST" | "OAUTH2_TRUST";
  allowedDomains?: string[];
  appUrl?: string;
  projectId?: string;
  permissionScope?: "ALL" | "PROJECT" | "WORKBOOK";
  runAsUserId?: string;
}

export interface UpdateConnectedAppRequest {
  status?: "ENABLED" | "DISABLED";
  allowedDomains?: string[];
  appUrl?: string;
  projectId?: string;
  permissionScope?: string;
}

export interface ConnectedAppDto {
  id: string;
  name: string;
  trustType: "DIRECT_TRUST" | "OAUTH2_TRUST";
  status: "ENABLED" | "DISABLED";
  clientId: string;
  clientSecret?: string | null;
  allowedDomains: string[];
  appUrl?: string;
  projectId?: string;
  permissionScope: string;
  runAsUserId?: string;
  secretRotatedAt?: string;
  createdAt: string;
  updatedAt: string;
}

export const connectedAppService = {
  list: async (): Promise<ConnectedAppDto[]> => {
    const res = await client.get<ConnectedAppDto[]>("/v1/connected-apps");
    return res.data;
  },
  get: async (id: string): Promise<ConnectedAppDto> => {
    const res = await client.get<ConnectedAppDto>(`/v1/connected-apps/${id}`);
    return res.data;
  },
  create: async (data: CreateConnectedAppRequest): Promise<ConnectedAppDto> => {
    const res = await client.post<ConnectedAppDto>("/v1/connected-apps", data);
    return res.data;
  },
  rotateSecret: async (id: string): Promise<ConnectedAppDto> => {
    const res = await client.post<ConnectedAppDto>(`/v1/connected-apps/${id}/rotate-secret`);
    return res.data;
  },
  update: async (id: string, data: UpdateConnectedAppRequest): Promise<ConnectedAppDto> => {
    const res = await client.patch<ConnectedAppDto>(`/v1/connected-apps/${id}`, data);
    return res.data;
  },
  delete: async (id: string): Promise<void> => {
    await client.delete(`/v1/connected-apps/${id}`);
  },
};
