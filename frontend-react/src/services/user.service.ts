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

export interface UserManagementDto {
  id: string;
  email: string;
  fullName: string;
  siteRole: BackendSiteRole;
  authMethod: string;
  status: string;
  groups: string[];
  lastLoginAt: string | null;
  ownedResourcesCount: number;
  createdAt: string;
}

export interface UserPageResponse {
  content: UserManagementDto[];
  page: number;
  size: number;
  totalElements: number;
}

export interface SearchUsersParams {
  q?: string;
  role?: string;
  group?: string;
  status?: string;
  page?: number;
  size?: number;
}

export const userService = {
  list: async () => (await client.get<User[]>(API.users)).data,

  async searchUsers(params: SearchUsersParams = {}): Promise<UserPageResponse> {
    const { data } = await client.get<UserPageResponse>("/v1/users", { params });
    return data;
  },

  async listLicenses(): Promise<LicensePoolDto[]> {
    const { data } = await client.get<LicensePoolDto[]>("/v1/users/licenses");
    return data;
  },

  async createUsers(payload: CreateUsersPayload): Promise<CreateUsersResponse> {
    const { data } = await client.post<CreateUsersResponse>("/v1/users", payload);
    return data;
  },

  async changeRole(userId: string, role: BackendSiteRole): Promise<UserManagementDto> {
    const { data } = await client.patch<UserManagementDto>(`/v1/users/${userId}/role`, { role });
    return data;
  },

  async batchChangeRole(userIds: string[], role: BackendSiteRole): Promise<UserManagementDto[]> {
    const { data } = await client.patch<UserManagementDto[]>("/v1/users/batch-role", {
      userIds,
      role,
    });
    return data;
  },

  async deactivateUser(userId: string): Promise<UserManagementDto> {
    const { data } = await client.post<UserManagementDto>(`/v1/users/${userId}/deactivate`);
    return data;
  },

  async activateUser(userId: string): Promise<UserManagementDto> {
    const { data } = await client.post<UserManagementDto>(`/v1/users/${userId}/activate`);
    return data;
  },

  async resendInvite(userId: string): Promise<UserManagementDto> {
    const { data } = await client.post<UserManagementDto>(`/v1/users/${userId}/resend-invite`);
    return data;
  },

  async deleteUser(userId: string, newOwnerId?: string): Promise<void> {
    await client.delete(`/v1/users/${userId}`, {
      params: newOwnerId ? { newOwnerId } : undefined,
    });
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
