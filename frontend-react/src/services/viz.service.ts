import client from "../lib/axios/client";

export interface QuerySpec {
  datasourceId: string;
  dimensions: string[];
  measures: string[];
  filters?: Record<string, unknown>;
  aggregations?: Record<string, string>;
  sort?: { field: string; direction: "ASC" | "DESC" };
  limit?: number;
}

export interface QueryResult {
  columns: string[];
  rows: Record<string, unknown>[];
  markCount: number;
  chartTypeSuggestion: string;
  executionTimeMs: number;
}

export interface TooltipConfigDto {
  showTooltip: boolean;
  template?: string;
  fields: string[];
}

export interface SheetDto {
  id: string;
  workbookId: string;
  datasourceId: string;
  name: string;
  chartType: string;
  config: Record<string, unknown>;
  tooltipConfig?: TooltipConfigDto;
  createdAt: string;
  updatedAt: string;
}

export interface SaveSheetRequest {
  datasourceId: string;
  name: string;
  chartType: string;
  config: Record<string, unknown>;
  tooltipConfig?: TooltipConfigDto;
}

export interface FormulaValidateRequest {
  datasourceId: string;
  formula: string;
  existingFieldNames?: string[];
  currentFieldName?: string;
}

export interface FormulaValidateResponse {
  valid: boolean;
  errorCode?: string;
  errorMessage?: string;
  errorPosition?: number;
}

export interface CalculatedFieldDto {
  id: string;
  datasourceId: string;
  name: string;
  formula: string;
  returnType: string;
  createdAt: string;
  updatedAt: string;
}

export interface SaveCalculatedFieldRequest {
  name: string;
  formula: string;
  returnType: string;
}

export interface ParameterDto {
  id: string;
  workbookId: string;
  name: string;
  dataType: string;
  allowableValuesType: string;
  allowableValuesConfig?: Record<string, unknown>;
  currentValue: string;
  createdAt: string;
  updatedAt: string;
}

export interface SaveParameterRequest {
  name: string;
  dataType: string;
  allowableValuesType: string;
  allowableValuesConfig?: Record<string, unknown>;
  currentValue: string;
}

export interface DashboardTileDto {
  id: string;
  tileType: string;
  sheetId?: string;
  title?: string;
  content?: string;
  x: number;
  y: number;
  width: number;
  height: number;
}

export interface DashboardActionDto {
  id: string;
  name: string;
  actionType: string;
  triggerEvent: string;
  sourceSheetId: string;
  targetSheetId?: string;
  config?: Record<string, unknown>;
}

export interface DashboardDto {
  id: string;
  workbookId: string;
  name: string;
  sizeMode: string;
  customWidth?: number;
  customHeight?: number;
  tiles: DashboardTileDto[];
  actions: DashboardActionDto[];
  createdAt: string;
  updatedAt: string;
}

export interface SaveDashboardRequest {
  name: string;
  sizeMode: string;
  customWidth?: number;
  customHeight?: number;
  tiles: DashboardTileDto[];
  actions: DashboardActionDto[];
}

export interface SaveActionsRequest {
  actions: DashboardActionDto[];
}

export interface TrustedDomainsResponse {
  domains: string[];
}

export const vizService = {
  // UC15: Query Execution & Sheet Management
  executeQuery: async (spec: QuerySpec): Promise<QueryResult> => {
    const res = await client.post<QueryResult>("/v1/query", spec);
    return res.data;
  },

  getSheets: async (workbookId: string): Promise<SheetDto[]> => {
    const res = await client.get<SheetDto[]>(`/v1/workbooks/${workbookId}/sheets`);
    return res.data;
  },

  getSheet: async (workbookId: string, sheetId: string): Promise<SheetDto> => {
    const res = await client.get<SheetDto>(`/v1/workbooks/${workbookId}/sheets/${sheetId}`);
    return res.data;
  },

  saveSheet: async (workbookId: string, payload: SaveSheetRequest): Promise<SheetDto> => {
    const res = await client.post<SheetDto>(`/v1/workbooks/${workbookId}/sheets`, payload);
    return res.data;
  },

  updateSheet: async (
    workbookId: string,
    sheetId: string,
    payload: SaveSheetRequest,
  ): Promise<SheetDto> => {
    const res = await client.put<SheetDto>(
      `/v1/workbooks/${workbookId}/sheets/${sheetId}`,
      payload,
    );
    return res.data;
  },

  updateTooltip: async (
    workbookId: string,
    sheetId: string,
    payload: TooltipConfigDto,
  ): Promise<SheetDto> => {
    const res = await client.put<SheetDto>(
      `/v1/workbooks/${workbookId}/sheets/${sheetId}/tooltip`,
      payload,
    );
    return res.data;
  },

  deleteSheet: async (workbookId: string, sheetId: string): Promise<void> => {
    await client.delete(`/v1/workbooks/${workbookId}/sheets/${sheetId}`);
  },

  // UC16: Calculated Fields & Parameters
  validateFormula: async (
    payload: FormulaValidateRequest,
  ): Promise<FormulaValidateResponse> => {
    const res = await client.post<FormulaValidateResponse>(
      "/v1/formulas/validate",
      payload,
    );
    return res.data;
  },

  getCalculatedFields: async (
    datasourceId: string,
  ): Promise<CalculatedFieldDto[]> => {
    const res = await client.get<CalculatedFieldDto[]>(
      `/v1/datasources/${datasourceId}/calculated-fields`,
    );
    return res.data;
  },

  saveCalculatedField: async (
    datasourceId: string,
    payload: SaveCalculatedFieldRequest,
  ): Promise<CalculatedFieldDto> => {
    const res = await client.post<CalculatedFieldDto>(
      `/v1/datasources/${datasourceId}/calculated-fields`,
      payload,
    );
    return res.data;
  },

  deleteCalculatedField: async (
    datasourceId: string,
    fieldId: string,
  ): Promise<void> => {
    await client.delete(
      `/v1/datasources/${datasourceId}/calculated-fields/${fieldId}`,
    );
  },

  getParameters: async (workbookId: string): Promise<ParameterDto[]> => {
    const res = await client.get<ParameterDto[]>(
      `/v1/workbooks/${workbookId}/parameters`,
    );
    return res.data;
  },

  saveParameter: async (
    workbookId: string,
    payload: SaveParameterRequest,
  ): Promise<ParameterDto> => {
    const res = await client.post<ParameterDto>(
      `/v1/workbooks/${workbookId}/parameters`,
      payload,
    );
    return res.data;
  },

  deleteParameter: async (
    workbookId: string,
    parameterId: string,
  ): Promise<void> => {
    await client.delete(`/v1/workbooks/${workbookId}/parameters/${parameterId}`);
  },

  // UC17: Dashboards
  getDashboards: async (workbookId: string): Promise<DashboardDto[]> => {
    const res = await client.get<DashboardDto[]>(
      `/v1/workbooks/${workbookId}/dashboards`,
    );
    return res.data;
  },

  getDashboard: async (
    workbookId: string,
    dashboardId: string,
  ): Promise<DashboardDto> => {
    const res = await client.get<DashboardDto>(
      `/v1/workbooks/${workbookId}/dashboards/${dashboardId}`,
    );
    return res.data;
  },

  saveDashboard: async (
    workbookId: string,
    payload: SaveDashboardRequest,
  ): Promise<DashboardDto> => {
    const res = await client.post<DashboardDto>(
      `/v1/workbooks/${workbookId}/dashboards`,
      payload,
    );
    return res.data;
  },

  updateDashboard: async (
    workbookId: string,
    dashboardId: string,
    payload: SaveDashboardRequest,
  ): Promise<DashboardDto> => {
    const res = await client.put<DashboardDto>(
      `/v1/workbooks/${workbookId}/dashboards/${dashboardId}`,
      payload,
    );
    return res.data;
  },

  deleteDashboard: async (
    workbookId: string,
    dashboardId: string,
  ): Promise<void> => {
    await client.delete(
      `/v1/workbooks/${workbookId}/dashboards/${dashboardId}`,
    );
  },

  // UC18: Actions & Interactivity
  saveDashboardActions: async (
    workbookId: string,
    dashboardId: string,
    payload: SaveActionsRequest,
  ): Promise<DashboardDto> => {
    const res = await client.put<DashboardDto>(
      `/v1/workbooks/${workbookId}/dashboards/${dashboardId}/actions`,
      payload,
    );
    return res.data;
  },

  getTrustedDomains: async (): Promise<TrustedDomainsResponse> => {
    const res = await client.get<TrustedDomainsResponse>(
      "/v1/settings/trusted-domains",
    );
    return res.data;
  },
};
