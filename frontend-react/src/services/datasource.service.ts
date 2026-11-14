import client from "../lib/axios/client";

export interface ConnectorDto {
  id: string;
  name: string;
  category: "FILE" | "DATABASE" | "CLOUD";
  available: boolean;
  defaultPort: number | null;
  driverDownloadUrl: string | null;
}

export interface ColumnSchemaDto {
  name: string;
  dataType: string;
  defaultRole: "DIMENSION" | "MEASURE";
}

export interface DiscoveredTableDto {
  tableName: string;
  schemaName: string;
  rowCount: number;
  suggestedMode: "LIVE" | "EXTRACT";
  columns: ColumnSchemaDto[];
  previewRows: Record<string, unknown>[];
}

export interface TestConnectionPayload {
  connectorType: string;
  host?: string;
  port?: number;
  databaseName?: string;
  schemaName?: string;
  username?: string;
  password?: string;
  driverMissing?: boolean;
}

export interface TestConnectionResponse {
  success: boolean;
  message: string;
  estimatedRowCount: number;
  suggestedMode: "LIVE" | "EXTRACT";
  tables: DiscoveredTableDto[];
}

export interface CreateDatasourcePayload {
  name: string;
  description?: string;
  connectorType: string;
  mode?: "LIVE" | "EXTRACT";
  projectId?: string;
  host?: string;
  port?: number;
  databaseName?: string;
  schemaName?: string;
  username?: string;
  password?: string;
  estimatedRowCount?: number;
  csvContent?: string;
}

export interface ConnectionSummaryDto {
  host: string | null;
  port: number | null;
  databaseName: string | null;
  schemaName: string | null;
  username: string | null;
  hasPassword: boolean;
}

export interface RefreshScheduleDto {
  frequency: string;
  runTime: string;
  timezone: string;
  refreshType: string;
  incrementalColumn: string | null;
  alertEmails: string | null;
  enabled: boolean;
  paused: boolean;
  consecutiveFailures: number;
  nextRunAt: string | null;
}

export interface DatasourceDto {
  id: string;
  name: string;
  description: string | null;
  connectorType: string;
  mode: "LIVE" | "EXTRACT";
  suggestedMode: "LIVE" | "EXTRACT";
  status: string;
  ownerId: string | null;
  projectId: string | null;
  fileName: string | null;
  rowCount: number;
  certified: boolean;
  qualityWarning: string | null;
  sensitivity: string | null;
  dependentWorkbooksCount: number;
  connection: ConnectionSummaryDto | null;
  schedule: RefreshScheduleDto | null;
  lastRefreshedAt: string | null;
  createdAt: string;
  updatedAt: string;
}

export interface ModelTableDto {
  id: string;
  tableName: string;
  schemaName: string;
  datasourceId: string;
  columns: string[];
}

export interface ModelRelationshipDto {
  id: string;
  leftTable: string;
  leftKey: string;
  leftKeyType: string;
  leftDatasourceId?: string;
  rightTable: string;
  rightKey: string;
  rightKeyType: string;
  rightDatasourceId?: string;
  joinType: "RELATIONSHIP" | "INNER" | "LEFT" | "RIGHT" | "FULL" | "BLEND";
  operator: string;
}

export interface ModelFieldDto {
  name: string;
  alias: string;
  sourceTable: string;
  dataType: string;
  role: "DIMENSION" | "MEASURE";
  defaultAggregation: string;
  hidden: boolean;
  description?: string | null;
  folder?: string | null;
  label?: string | null;
}

export interface DataModelDto {
  datasourceId: string;
  datasourceName: string;
  tables: ModelTableDto[];
  relationships: ModelRelationshipDto[];
  fields: ModelFieldDto[];
  suggestedRelationships: ModelRelationshipDto[];
  updatedAt: string;
}

export interface SaveDataModelPayload {
  tables?: ModelTableDto[];
  relationships?: ModelRelationshipDto[];
  fields?: ModelFieldDto[];
}

export interface ModelPreviewResponse {
  fields: ModelFieldDto[];
  rows: Record<string, unknown>[];
  rowCount: number;
}

export interface RefreshRunDto {
  id: string;
  datasourceId: string;
  jobId: string | null;
  refreshType: string;
  triggerType: string;
  status: string;
  rowsProcessed: number;
  errorCode: string | null;
  errorMessage: string | null;
  startedAt: string;
  finishedAt: string | null;
}

export interface DependentWorkbookDto {
  id: string;
  name: string;
  projectId: string | null;
  ownerId: string | null;
  status: string;
  updatedAt: string | null;
}

export interface UpdateSchedulePayload {
  frequency: string;
  runTime: string;
  timezone: string;
  refreshType: string;
  incrementalColumn?: string;
  alertEmails?: string;
  enabled?: boolean;
}

export interface UpdateConnectionPayload {
  host?: string;
  port?: number;
  databaseName?: string;
  schemaName?: string;
  username?: string;
  password?: string;
}

export const datasourceService = {
  async list(managed = true): Promise<DatasourceDto[]> {
    const { data } = await client.get<DatasourceDto[]>("/v1/datasources", {
      params: { managed },
    });
    return data;
  },

  async listConnectors(): Promise<ConnectorDto[]> {
    const { data } = await client.get<ConnectorDto[]>("/v1/connectors");
    return data;
  },

  async testConnection(payload: TestConnectionPayload): Promise<TestConnectionResponse> {
    const { data } = await client.post<TestConnectionResponse>("/v1/datasources/test", payload);
    return data;
  },

  async create(payload: CreateDatasourcePayload): Promise<DatasourceDto> {
    const { data } = await client.post<DatasourceDto>("/v1/datasources", payload);
    return data;
  },

  async uploadFile(
    file: File,
    params: { name?: string; connectorType?: string; mode?: string; projectId?: string } = {},
  ): Promise<DatasourceDto> {
    const formData = new FormData();
    formData.append("file", file);
    if (params.name) formData.append("name", params.name);
    if (params.connectorType) formData.append("connectorType", params.connectorType);
    if (params.mode) formData.append("mode", params.mode);
    if (params.projectId) formData.append("projectId", params.projectId);
    const { data } = await client.post<DatasourceDto>("/v1/datasources", formData, {
      headers: { "Content-Type": "multipart/form-data" },
    });
    return data;
  },

  async getById(id: string): Promise<DatasourceDto> {
    const { data } = await client.get<DatasourceDto>(`/v1/datasources/${id}`);
    return data;
  },

  async listTables(id: string): Promise<DiscoveredTableDto[]> {
    const { data } = await client.get<DiscoveredTableDto[]>(`/v1/datasources/${id}/tables`);
    return data;
  },

  async getModel(id: string): Promise<DataModelDto> {
    const { data } = await client.get<DataModelDto>(`/v1/datasources/${id}/model`);
    return data;
  },

  async saveModel(id: string, payload: SaveDataModelPayload): Promise<DataModelDto> {
    const { data } = await client.put<DataModelDto>(`/v1/datasources/${id}/model`, payload);
    return data;
  },

  async suggestRelationships(id: string): Promise<ModelRelationshipDto[]> {
    const { data } = await client.post<ModelRelationshipDto[]>(
      `/v1/datasources/${id}/model/suggest-relationships`,
    );
    return data;
  },

  async previewModel(id: string, payload?: SaveDataModelPayload): Promise<ModelPreviewResponse> {
    const { data } = await client.post<ModelPreviewResponse>(
      `/v1/datasources/${id}/model/preview`,
      payload ?? {},
    );
    return data;
  },

  async updateMode(id: string, mode: "LIVE" | "EXTRACT"): Promise<DatasourceDto> {
    const { data } = await client.put<DatasourceDto>(`/v1/datasources/${id}/mode`, { mode });
    return data;
  },

  async updateSchedule(id: string, payload: UpdateSchedulePayload): Promise<DatasourceDto> {
    const { data } = await client.put<DatasourceDto>(
      `/v1/datasources/${id}/refresh-schedule`,
      payload,
    );
    return data;
  },

  async triggerRefresh(id: string): Promise<RefreshRunDto> {
    const { data } = await client.post<RefreshRunDto>(`/v1/datasources/${id}/refresh`);
    return data;
  },

  async listRefreshRuns(id: string): Promise<RefreshRunDto[]> {
    const { data } = await client.get<RefreshRunDto[]>(`/v1/datasources/${id}/refresh-runs`);
    return data;
  },

  async updateConnection(id: string, payload: UpdateConnectionPayload): Promise<DatasourceDto> {
    const { data } = await client.put<DatasourceDto>(`/v1/datasources/${id}/connection`, payload);
    return data;
  },

  async listDependents(id: string): Promise<DependentWorkbookDto[]> {
    const { data } = await client.get<DependentWorkbookDto[]>(`/v1/datasources/${id}/dependents`);
    return data;
  },
};
