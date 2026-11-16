import client from "../lib/axios/client";

export interface DashboardDto {
  id: string;
  workbookId: string;
  name: string;
  description: string | null;
  definition: string | null;
  ownerId: string;
  createdAt: string;
  updatedAt: string;
}

export interface DashboardViewDto {
  id: string;
  workbookId: string;
  workbookName: string;
  name: string;
  description: string | null;
  definition: string | null;
  dataAsOf: string;
  canView: boolean;
  canInteract: boolean;
  canFilter: boolean;
  canDownload: boolean;
  canComment: boolean;
  canWebEdit: boolean;
}

export interface QueryRequest {
  dashboardId: string;
  chartId?: string | null;
  filters?: Record<string, unknown>;
  parameters?: Record<string, unknown>;
  limit?: number | null;
}

export interface QueryResultDto {
  dashboardId: string;
  chartId: string | null;
  appliedFilters: Record<string, unknown>;
  totalRows: number;
  data: Record<string, unknown>[];
  dataAsOf: string;
}

export interface CreateDashboardRequest {
  workbookId: string;
  name: string;
  description?: string | null;
  definition?: string | null;
}

export const dashboardService = {
  getById: async (id: string) => {
    const res = await client.get<DashboardDto>(`/v1/dashboards/${id}`);
    return res.data;
  },

  getView: async (id: string) => {
    const res = await client.get<DashboardViewDto>(`/v1/dashboards/${id}/view`);
    return res.data;
  },

  create: async (data: CreateDashboardRequest) => {
    const res = await client.post<DashboardDto>("/v1/dashboards", data);
    return res.data;
  },

  query: async (data: QueryRequest) => {
    const res = await client.post<QueryResultDto>("/v1/dashboards/query", data);
    return res.data;
  },
};
