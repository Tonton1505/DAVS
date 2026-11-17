import client from "../lib/axios/client";

export interface DatasourceRlsStatusDto {
  datasourceId: string;
  datasourceName: string;
  rlsEnabled: boolean;
}

export interface RowFilterDto {
  id: string;
  datasourceId: string;
  datasourceName: string;
  subjectType: "USER" | "GROUP";
  subjectId: string;
  subjectName: string;
  fieldName: string;
  allowedValues: string[];
  createdAt: string;
  updatedAt: string;
}

export interface SaveRowFilterRequest {
  datasourceId: string;
  subjectType: "USER" | "GROUP";
  subjectId: string;
  fieldName: string;
  allowedValues: string[];
}

export const rlsService = {
  listDatasources: async (): Promise<DatasourceRlsStatusDto[]> => {
    const res = await client.get<DatasourceRlsStatusDto[]>("/v1/admin/rls/datasources");
    return res.data;
  },

  toggleDatasource: async (
    datasourceId: string,
    enabled: boolean,
  ): Promise<DatasourceRlsStatusDto> => {
    const res = await client.put<DatasourceRlsStatusDto>(
      `/v1/admin/rls/datasources/${datasourceId}`,
      { enabled },
    );
    return res.data;
  },

  listPolicies: async (datasourceId?: string): Promise<RowFilterDto[]> => {
    const res = await client.get<RowFilterDto[]>("/v1/admin/rls/policies", {
      params: datasourceId ? { datasourceId } : undefined,
    });
    return res.data;
  },

  savePolicy: async (payload: SaveRowFilterRequest): Promise<RowFilterDto> => {
    const res = await client.post<RowFilterDto>("/v1/admin/rls/policies", payload);
    return res.data;
  },

  deletePolicy: async (id: string): Promise<void> => {
    await client.delete(`/v1/admin/rls/policies/${id}`);
  },
};
