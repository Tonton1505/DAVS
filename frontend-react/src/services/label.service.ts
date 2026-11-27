import client, { isOfflineOrMockError } from "../lib/axios/client";
import { offlineDemoStore } from "./offline-demo.store";

export interface LabelCategoryDto {
  id: string;
  name: string;
  description: string | null;
  isWarning: boolean;
  createdBy: string | null;
  createdAt: string;
}

export interface LabelDto {
  id: string;
  categoryId: string;
  categoryName: string;
  value: string;
  description: string | null;
  isWarning: boolean;
  assignedCount: number;
  createdBy: string | null;
  createdAt: string;
  updatedAt: string;
}

export interface LabelTargetRef {
  type: "WORKBOOK" | "DATASOURCE" | "TABLE" | "COLUMN" | "PROJECT";
  id: string;
}

export const labelService = {
  async listCategories(): Promise<LabelCategoryDto[]> {
    try {
      const response = await client.get<LabelCategoryDto[]>("/v1/label-categories");
      return response.data;
    } catch (err) {
      if (isOfflineOrMockError(err)) return offlineDemoStore.listCategories();
      throw err;
    }
  },

  async createCategory(payload: {
    name: string;
    description?: string;
    isWarning?: boolean;
  }): Promise<LabelCategoryDto> {
    try {
      const response = await client.post<LabelCategoryDto>("/v1/label-categories", payload);
      return response.data;
    } catch (err) {
      if (isOfflineOrMockError(err)) return offlineDemoStore.createCategory(payload);
      throw err;
    }
  },

  async listLabels(params?: { categoryId?: string; q?: string }): Promise<LabelDto[]> {
    const queryParams: Record<string, string> = {};
    if (params?.categoryId) {
      queryParams.categoryId = params.categoryId;
    }
    if (params?.q && params.q.trim()) {
      queryParams.q = params.q.trim();
    }
    try {
      const response = await client.get<LabelDto[]>("/v1/labels", { params: queryParams });
      return response.data;
    } catch (err) {
      if (isOfflineOrMockError(err)) return offlineDemoStore.listLabels(params);
      throw err;
    }
  },

  async createLabel(payload: {
    categoryId: string;
    value: string;
    description?: string;
    isWarning?: boolean;
  }): Promise<LabelDto> {
    try {
      const response = await client.post<LabelDto>("/v1/labels", payload);
      return response.data;
    } catch (err) {
      if (isOfflineOrMockError(err)) return offlineDemoStore.createLabel(payload);
      throw err;
    }
  },

  async updateLabel(
    id: string,
    payload: {
      value?: string;
      description?: string;
      isWarning?: boolean;
    },
  ): Promise<LabelDto> {
    try {
      const response = await client.patch<LabelDto>(`/v1/labels/${id}`, payload);
      return response.data;
    } catch (err) {
      if (isOfflineOrMockError(err)) return offlineDemoStore.updateLabel(id, payload);
      throw err;
    }
  },

  async deleteLabel(id: string): Promise<{ deletedId: string; unassignedCount: number }> {
    try {
      const response = await client.delete<{ deletedId: string; unassignedCount: number }>(
        `/v1/labels/${id}`,
      );
      return response.data;
    } catch (err) {
      if (isOfflineOrMockError(err)) return offlineDemoStore.deleteLabel(id);
      throw err;
    }
  },

  async assignLabel(labelId: string, targets: LabelTargetRef[]): Promise<LabelDto> {
    try {
      const response = await client.post<LabelDto>(`/v1/labels/${labelId}/assign`, { targets });
      return response.data;
    } catch (err) {
      if (isOfflineOrMockError(err)) return offlineDemoStore.assignLabel(labelId, targets);
      throw err;
    }
  },

  async unassignLabel(labelId: string, targets: LabelTargetRef[]): Promise<LabelDto> {
    try {
      const response = await client.post<LabelDto>(`/v1/labels/${labelId}/unassign`, { targets });
      return response.data;
    } catch (err) {
      if (isOfflineOrMockError(err)) return offlineDemoStore.unassignLabel(labelId, targets);
      throw err;
    }
  },

  async listLabelsForTarget(type: string, id: string): Promise<LabelDto[]> {
    try {
      const response = await client.get<LabelDto[]>("/v1/labels/targets", {
        params: { type, id },
      });
      return response.data;
    } catch (err) {
      if (isOfflineOrMockError(err)) return offlineDemoStore.listLabelsForTarget(type, id);
      throw err;
    }
  },
};
