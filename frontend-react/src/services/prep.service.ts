import client from "../lib/axios/client";

export type PrepStepType =
  | "FILTER"
  | "RENAME"
  | "DROP"
  | "REPLACE_NULL"
  | "DEDUPE"
  | "CAST"
  | "JOIN"
  | "UNION"
  | "PIVOT"
  | "AGGREGATE";

export interface PrepStepDto {
  id: string;
  type: PrepStepType;
  name: string;
  config: Record<string, unknown>;
}

export interface LineageStepDto {
  stepIndex: number;
  stepType: string;
  description: string;
  inputRows: number;
  outputRows: number;
}

export interface PrepFlowDto {
  id: string;
  datasourceId: string | null;
  outputDatasourceId: string | null;
  name: string;
  description: string | null;
  steps: PrepStepDto[];
  lineage: LineageStepDto[];
  ownerId: string | null;
  published: boolean;
  createdAt: string;
  updatedAt: string;
}

export interface SavePrepFlowPayload {
  datasourceId?: string | null;
  name: string;
  description?: string;
  steps: PrepStepDto[];
  published?: boolean;
}

export interface ColumnProfileDto {
  fieldName: string;
  dataType: string;
  role: "DIMENSION" | "MEASURE";
  rowCount: number;
  nullCount: number;
  nullPercent: number;
  distinctCount: number;
  duplicateCount: number;
  minValue: string | null;
  maxValue: string | null;
}

export interface PrepPreviewResponse {
  stepIndex: number;
  inputRowCount: number;
  rowCount: number;
  duplicateRowCount: number;
  replacedNullsCount: number;
  profile: ColumnProfileDto[];
  lineage: LineageStepDto[];
  rows: Record<string, unknown>[];
}

export interface PrepRunDto {
  runId: string;
  flowId: string;
  jobId: string | null;
  status: string;
  inputRows: number;
  outputRows: number;
  removedDuplicates: number;
  replacedNulls: number;
  lineage: LineageStepDto[];
  errorCode: string | null;
  errorMessage: string | null;
  startedAt: string;
  finishedAt: string | null;
}

export const prepService = {
  async listFlows(datasourceId?: string): Promise<PrepFlowDto[]> {
    const { data } = await client.get<PrepFlowDto[]>("/v1/prep-flows", {
      params: datasourceId ? { datasourceId } : undefined,
    });
    return data;
  },

  async getFlow(id: string): Promise<PrepFlowDto> {
    const { data } = await client.get<PrepFlowDto>(`/v1/prep-flows/${id}`);
    return data;
  },

  async createFlow(payload: SavePrepFlowPayload): Promise<PrepFlowDto> {
    const { data } = await client.post<PrepFlowDto>("/v1/prep-flows", payload);
    return data;
  },

  async updateFlow(id: string, payload: SavePrepFlowPayload): Promise<PrepFlowDto> {
    const { data } = await client.put<PrepFlowDto>(`/v1/prep-flows/${id}`, payload);
    return data;
  },

  async previewFlow(
    id: string,
    payload: { stepIndex?: number; steps?: PrepStepDto[] } = {},
  ): Promise<PrepPreviewResponse> {
    const { data } = await client.post<PrepPreviewResponse>(
      `/v1/prep-flows/${id}/preview`,
      payload,
    );
    return data;
  },

  async runFlow(id: string): Promise<PrepRunDto> {
    const { data } = await client.post<PrepRunDto>(`/v1/prep-flows/${id}/run`);
    return data;
  },

  async listRuns(id: string): Promise<PrepRunDto[]> {
    const { data } = await client.get<PrepRunDto[]>(`/v1/prep-flows/${id}/runs`);
    return data;
  },
};
