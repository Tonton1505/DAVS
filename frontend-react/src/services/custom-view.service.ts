import client from "../lib/axios/client";
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
    const res = await client.get<CustomViewDto[]>(`/api/v1/dashboards/${dashboardId}/custom-views`);
    return res.data;
  },

  create: async (dashboardId: string, data: CreateCustomViewRequest) => {
    const res = await client.post<CustomViewDto>(`/api/v1/dashboards/${dashboardId}/custom-views`, data);
    return res.data;
  },

  setDefault: async (id: string) => {
    await client.put(`/api/v1/custom-views/${id}/default`);
  },

  setShared: async (id: string, shared: boolean) => {
    await client.put(`/api/v1/custom-views/${id}/share`, null, {
      params: { shared },
    });
  },

  delete: async (id: string) => {
    await client.delete(`/api/v1/custom-views/${id}`);
  },

  saveAsWorkbook: async (data: SaveAsWorkbookRequest) => {
    const res = await client.post<WorkbookDto>("/api/v1/workbooks/save-as", data);
    return res.data;
  },
};
