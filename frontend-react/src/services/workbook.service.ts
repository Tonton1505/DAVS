import client, { isOfflineOrMockError } from "../lib/axios/client";
import type { DashboardItem } from "../common/constants/types";
import type { LabelDto } from "./label.service";
import { offlineDemoStore } from "./offline-demo.store";

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
  async list(): Promise<DashboardItem[]> {
    try {
      return (await client.get<DashboardItem[]>("/v1/dashboards")).data;
    } catch (err) {
      if (isOfflineOrMockError(err)) return offlineDemoStore.listDashboardItems();
      throw err;
    }
  },

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

    try {
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
    } catch (err) {
      if (isOfflineOrMockError(err)) return offlineDemoStore.uploadWorkbook(input);
      throw err;
    }
  },

  async getById(id: string): Promise<WorkbookDto> {
    try {
      const response = await client.get<WorkbookDto>(`/v1/workbooks/${id}`);
      return response.data;
    } catch (err) {
      if (isOfflineOrMockError(err)) return offlineDemoStore.getWorkbookById(id);
      throw err;
    }
  },

  async listByProject(projectId: string): Promise<WorkbookDto[]> {
    try {
      const response = await client.get<WorkbookDto[]>(`/v1/projects/${projectId}/workbooks`);
      return response.data;
    } catch (err) {
      if (isOfflineOrMockError(err)) return offlineDemoStore.listWorkbooksByProject(projectId);
      throw err;
    }
  },

  async listVersions(workbookId: string): Promise<WorkbookVersionDto[]> {
    try {
      const response = await client.get<WorkbookVersionDto[]>(`/v1/workbooks/${workbookId}/versions`);
      return response.data;
    } catch (err) {
      if (isOfflineOrMockError(err)) return offlineDemoStore.listVersions(workbookId);
      throw err;
    }
  },

  async restoreVersion(
    workbookId: string,
    versionNumber: number,
    note: string,
  ): Promise<WorkbookVersionDto> {
    try {
      const response = await client.post<WorkbookVersionDto>(
        `/v1/workbooks/${workbookId}/versions/${versionNumber}/restore`,
        { note },
      );
      return response.data;
    } catch (err) {
      if (isOfflineOrMockError(err)) {
        return offlineDemoStore.restoreVersion(workbookId, versionNumber, note);
      }
      throw err;
    }
  },

  async downloadVersion(workbookId: string, versionNumber: number): Promise<Blob> {
    try {
      const response = await client.get<Blob>(
        `/v1/workbooks/${workbookId}/versions/${versionNumber}/download`,
        { responseType: "blob" },
      );
      return response.data;
    } catch (err) {
      if (isOfflineOrMockError(err)) {
        return offlineDemoStore.downloadVersion(workbookId, versionNumber);
      }
      throw err;
    }
  },

  async deleteVersion(workbookId: string, versionNumber: number): Promise<void> {
    try {
      await client.delete(`/v1/workbooks/${workbookId}/versions/${versionNumber}`);
    } catch (err) {
      if (isOfflineOrMockError(err)) {
        offlineDemoStore.deleteVersion(workbookId, versionNumber);
        return;
      }
      throw err;
    }
  },
};
