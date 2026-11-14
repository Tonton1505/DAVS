import client from "../lib/axios/client";

export interface SystemSettingDto {
  key: string;
  value: string;
  valueType: "STRING" | "INT" | "BOOLEAN" | "JSON";
  description: string | null;
  updatedAt: string;
  updatedBy: string | null;
}

export interface AuditLogResponse {
  id: string;
  action: string;
  actorId: string | null;
  actorEmail: string | null;
  resourceType: string | null;
  resourceId: string | null;
  ipAddress: string | null;
  createdAt: string;
}

export interface PageResponse<T> {
  content: T[];
  page: number;
  size: number;
  totalElements: number;
}

export const adminSettingsService = {
  async getSettings(): Promise<SystemSettingDto[]> {
    const { data } = await client.get<SystemSettingDto[]>("/v1/admin/settings");
    return data;
  },
  async updateSettings(settings: Record<string, string>): Promise<SystemSettingDto[]> {
    const { data } = await client.put<SystemSettingDto[]>("/v1/admin/settings", { settings });
    return data;
  },
  async listAuditLogs(page = 0, size = 25): Promise<PageResponse<AuditLogResponse>> {
    const { data } = await client.get<PageResponse<AuditLogResponse>>("/v1/audit-logs", {
      params: { page, size },
    });
    return data;
  },
};
