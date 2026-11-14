import client from "../lib/axios/client";
import { API } from "../common/constants/api-endpoints";
import type { User } from "../common/constants/types";
import type { BackendSiteRole } from "./auth.service";

export type AuthMethod = "LOCAL" | "SSO";

export interface LicensePoolDto {
  role: BackendSiteRole;
  total: number;
  used: number;
  available: number;
}

export interface CreateUsersPayload {
  authMethod: AuthMethod;
  usernames: string[];
  role: BackendSiteRole;
}

export interface CreateUsersResponse {
  createdCount: number;
  skippedCount: number;
  uninvitedCount: number;
  created: { id: string; email: string; status: string }[];
  skipped: { email: string; code: string; message: string }[];
}

export interface ImportJobResponse {
  jobId: string;
}

export interface ImportJobStatusResponse {
  jobId: string;
  status: string;
  totalRows: number;
  createdCount: number;
  skippedCount: number;
  errorCount: number;
  hasErrorReport: boolean;
}

export const userService = {
  list: async () => (await client.get<User[]>(API.users)).data,

  async listLicenses(): Promise<LicensePoolDto[]> {
    const { data } = await client.get<LicensePoolDto[]>("/v1/users/licenses");
    return data;
  },

  async createUsers(payload: CreateUsersPayload): Promise<CreateUsersResponse> {
    const { data } = await client.post<CreateUsersResponse>("/v1/users", payload);
    return data;
  },

  async importUsers(
    file: File,
    authMethod: AuthMethod,
    defaultRole: BackendSiteRole,
  ): Promise<ImportJobResponse> {
    const formData = new FormData();
    formData.append("file", file);
    formData.append("authMethod", authMethod);
    formData.append("defaultRole", defaultRole);
    const { data } = await client.post<ImportJobResponse>("/v1/users/import", formData, {
      headers: { "Content-Type": "multipart/form-data" },
    });
    return data;
  },

  async getImportStatus(jobId: string): Promise<ImportJobStatusResponse> {
    const { data } = await client.get<ImportJobStatusResponse>(`/v1/users/import/${jobId}`);
    return data;
  },

  async downloadImportErrors(jobId: string): Promise<Blob> {
    const { data } = await client.get<Blob>(`/v1/users/import/${jobId}/errors`, {
      responseType: "blob",
    });
    return data;
  },

  async downloadTemplate(): Promise<Blob> {
    const { data } = await client.get<Blob>("/v1/users/import-template", {
      responseType: "blob",
    });
    return data;
  },
};
