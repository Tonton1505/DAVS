import client from "../lib/axios/client";
import { API } from "../common/constants/api-endpoints";
import type { DashboardItem } from "../common/constants/types";
import type { LabelDto } from "./label.service";

export interface WorkbookDto {
  id: string;
  projectId: string;
  projectName: string;
  name: string;
  description: string | null;
  currentVersion: number;
  ownerId: string;
  ownerName: string;
  importStatus: string;
  needsReconnect: boolean;
  sizeBytes: number;
  fileName: string;
  labels: LabelDto[];
  createdAt: string;
  updatedAt: string;
}

export interface WorkbookVersionDto {
  id: string;
  workbookId: string;
  versionNumber: number;
  fileName: string;
  sizeBytes: number;
  note: string | null;
  createdBy: string | null;
  createdByName: string;
  createdAt: string;
  current: boolean;
  purged: boolean;
}

export interface UploadWorkbookInput {
  projectId: string;
  name: string;
  description?: string;
  overwrite?: boolean;
  note?: string;
  file: File;
  onProgress?: (percent: number) => void;
}

export const workbookService = {
  list: async () => (await client.get<DashboardItem[]>(API.dashboards)).data,

  async upload(input: UploadWorkbookInput): Promise<WorkbookDto> {
    const formData = new FormData();
    formData.append("name", input.name);
    if (input.description) {
      formData.append("description", input.description);
    }
    if (input.overwrite !== undefined) {
      formData.append("overwrite", String(input.overwrite));
    }
    if (input.note) {
      formData.append("note", input.note);
    }
    formData.append("file", input.file);

    const response = await client.post<WorkbookDto>(
      `/v1/projects/${input.projectId}/workbooks/upload`,
      formData,
      {
        headers: { "Content-Type": "multipart/form-data" },
        onUploadProgress: (event) => {
          if (input.onProgress && event.total && event.total > 0) {
            const pct = Math.min(100, Math.round((event.loaded * 100) / event.total));
            input.onProgress(pct);
          }
        },
      },
    );
    return response.data;
  },

  async getById(id: string): Promise<WorkbookDto> {
    const response = await client.get<WorkbookDto>(`/v1/workbooks/${id}`);
    return response.data;
  },

  async listByProject(projectId: string): Promise<WorkbookDto[]> {
    const response = await client.get<WorkbookDto[]>(`/v1/projects/${projectId}/workbooks`);
    return response.data;
  },

  async listVersions(workbookId: string): Promise<WorkbookVersionDto[]> {
    const response = await client.get<WorkbookVersionDto[]>(`/v1/workbooks/${workbookId}/versions`);
    return response.data;
  },

  async restoreVersion(
    workbookId: string,
    versionNumber: number,
    note: string,
  ): Promise<WorkbookVersionDto> {
    const response = await client.post<WorkbookVersionDto>(
      `/v1/workbooks/${workbookId}/versions/${versionNumber}/restore`,
      { note },
    );
    return response.data;
  },

  async downloadVersion(workbookId: string, versionNumber: number): Promise<Blob> {
    const response = await client.get<Blob>(
      `/v1/workbooks/${workbookId}/versions/${versionNumber}/download`,
      { responseType: "blob" },
    );
    return response.data;
  },

  async deleteVersion(workbookId: string, versionNumber: number): Promise<void> {
    await client.delete(`/v1/workbooks/${workbookId}/versions/${versionNumber}`);
  },
};
