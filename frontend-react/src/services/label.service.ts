import client from "../lib/axios/client";

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
    const response = await client.get<LabelCategoryDto[]>("/v1/label-categories");
    return response.data;
  },

  async createCategory(payload: {
    name: string;
    description?: string;
    isWarning?: boolean;
  }): Promise<LabelCategoryDto> {
    const response = await client.post<LabelCategoryDto>("/v1/label-categories", payload);
    return response.data;
  },

  async listLabels(params?: { categoryId?: string; q?: string }): Promise<LabelDto[]> {
    const queryParams: Record<string, string> = {};
    if (params?.categoryId) {
      queryParams.categoryId = params.categoryId;
    }
    if (params?.q && params.q.trim()) {
      queryParams.q = params.q.trim();
    }
    const response = await client.get<LabelDto[]>("/v1/labels", { params: queryParams });
    return response.data;
  },

  async createLabel(payload: {
    categoryId: string;
    value: string;
    description?: string;
    isWarning?: boolean;
  }): Promise<LabelDto> {
    const response = await client.post<LabelDto>("/v1/labels", payload);
    return response.data;
  },

  async updateLabel(
    id: string,
    payload: {
      value?: string;
      description?: string;
      isWarning?: boolean;
    },
  ): Promise<LabelDto> {
    const response = await client.patch<LabelDto>(`/v1/labels/${id}`, payload);
    return response.data;
  },

  async deleteLabel(id: string): Promise<{ deletedId: string; unassignedCount: number }> {
    const response = await client.delete<{ deletedId: string; unassignedCount: number }>(
      `/v1/labels/${id}`,
    );
    return response.data;
  },

  async assignLabel(labelId: string, targets: LabelTargetRef[]): Promise<LabelDto> {
    const response = await client.post<LabelDto>(`/v1/labels/${labelId}/assign`, { targets });
    return response.data;
  },

  async unassignLabel(labelId: string, targets: LabelTargetRef[]): Promise<LabelDto> {
    const response = await client.post<LabelDto>(`/v1/labels/${labelId}/unassign`, { targets });
    return response.data;
  },

  async listLabelsForTarget(type: string, id: string): Promise<LabelDto[]> {
    const response = await client.get<LabelDto[]>("/v1/labels/targets", {
      params: { type, id },
    });
    return response.data;
  },
};
