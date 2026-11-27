import client, { isOfflineOrMockError } from "../lib/axios/client";
import { offlineDemoStore } from "./offline-demo.store";
import type { WorkbookDto } from "./workbook.service";

export interface CustomViewDto {
  id: string;
  dashboardId: string;
  name: string;
  filters: Record<string, unknown>;
  parameters: Record<string, unknown>;
  isDefault: boolean;
  isShared: boolean;
  ownerId: string;
  ownerName: string;
  createdAt: string;
  updatedAt: string;
}

export interface CreateCustomViewRequest {
  dashboardId: string;
  name: string;
  filters?: Record<string, unknown>;
  parameters?: Record<string, unknown>;
  isDefault?: boolean;
  isShared?: boolean;
}

export interface SaveAsWorkbookRequest {
  sourceWorkbookId: string;
  targetProjectId: string;
  newName: string;
  description?: string | null;
}

export const customViewService = {
  list: async (dashboardId: string) => {
    try {
      const res = await client.get<CustomViewDto[]>(`/v1/dashboards/${dashboardId}/custom-views`);
      return res.data;
    } catch (err) {
      if (isOfflineOrMockError(err)) return offlineDemoStore.listCustomViews(dashboardId);
      throw err;
    }
  },

  create: async (dashboardId: string, data: CreateCustomViewRequest) => {
    try {
      const res = await client.post<CustomViewDto>(
        `/v1/dashboards/${dashboardId}/custom-views`,
        data,
      );
      return res.data;
    } catch (err) {
      if (isOfflineOrMockError(err)) return offlineDemoStore.createCustomView(dashboardId, data);
      throw err;
    }
  },

  setDefault: async (id: string) => {
    try {
      await client.put(`/v1/custom-views/${id}/default`);
    } catch (err) {
      if (isOfflineOrMockError(err)) {
        offlineDemoStore.setDefaultCustomView(id);
        return;
      }
      throw err;
    }
  },

  setShared: async (id: string, shared: boolean) => {
    try {
      await client.put(`/v1/custom-views/${id}/share`, null, {
        params: { shared },
      });
    } catch (err) {
      if (isOfflineOrMockError(err)) {
        offlineDemoStore.setSharedCustomView(id, shared);
        return;
      }
      throw err;
    }
  },

  delete: async (id: string) => {
    try {
      await client.delete(`/v1/custom-views/${id}`);
    } catch (err) {
      if (isOfflineOrMockError(err)) {
        offlineDemoStore.deleteCustomView(id);
        return;
      }
      throw err;
    }
  },

  saveAsWorkbook: async (data: SaveAsWorkbookRequest) => {
    try {
      const res = await client.post<WorkbookDto>("/v1/workbooks/save-as", data);
      return res.data;
    } catch (err) {
      if (isOfflineOrMockError(err)) return offlineDemoStore.saveAsWorkbook(data);
      throw err;
    }
  },
};
