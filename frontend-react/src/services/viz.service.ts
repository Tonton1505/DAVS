import client from "../lib/axios/client";

export const DEFAULT_DATASOURCE_ID = "00000000-0000-0000-0000-000000000101";
export const DEFAULT_WORKBOOK_ID = "00000000-0000-0000-0000-000000000201";

export interface Sales2026Row {
  month: string;
  value: number;
  profit: number;
  quantity: number;
  discount: number;
  Ma_KH?: string;
  Vung?: string;
  Doanh_so?: number;
  Loi_nhuan?: number;
  So_luong?: number;
  [key: string]: unknown;
}

export const DEFAULT_SALES_2026_ROWS: Sales2026Row[] = [
  { month: "T1", value: 82, profit: 24, quantity: 450, discount: 0.05, Ma_KH: "KH01", Vung: "Miền Bắc" },
  { month: "T2", value: 96, profit: 31, quantity: 510, discount: 0.05, Ma_KH: "KH02", Vung: "Miền Nam" },
  { month: "T3", value: 105, profit: 35, quantity: 580, discount: 0.08, Ma_KH: "KH03", Vung: "Miền Trung" },
  { month: "T4", value: 118, profit: 42, quantity: 620, discount: 0.08, Ma_KH: "KH04", Vung: "Miền Bắc" },
  { month: "T5", value: 126, profit: 40, quantity: 690, discount: 0.1, Ma_KH: "KH05", Vung: "Miền Nam" },
  { month: "T6", value: 138, profit: 48, quantity: 720, discount: 0.1, Ma_KH: "KH06", Vung: "Miền Trung" },
  { month: "T7", value: 148, profit: 53, quantity: 780, discount: 0.07, Ma_KH: "KH07", Vung: "Miền Bắc" },
  { month: "T8", value: 156, profit: 55, quantity: 810, discount: 0.07, Ma_KH: "KH08", Vung: "Miền Nam" },
  { month: "T9", value: 172, profit: 62, quantity: 890, discount: 0.12, Ma_KH: "KH09", Vung: "Miền Bắc" },
  { month: "T10", value: 188, profit: 68, quantity: 950, discount: 0.12, Ma_KH: "KH10", Vung: "Miền Trung" },
  { month: "T11", value: 205, profit: 75, quantity: 1040, discount: 0.15, Ma_KH: "KH11", Vung: "Miền Nam" },
  { month: "T12", value: 240, profit: 92, quantity: 1200, discount: 0.2, Ma_KH: "KH12", Vung: "Miền Bắc" },
];

const UUID_REGEX = /^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$/i;

function resolveDatasourceUuid(id?: string): string {
  if (!id || id === "ds-sales-demo") return DEFAULT_DATASOURCE_ID;
  if (UUID_REGEX.test(id)) return id;
  return DEFAULT_DATASOURCE_ID;
}

function resolveWorkbookUuid(id?: string): string {
  if (!id || id === "wb-default") return DEFAULT_WORKBOOK_ID;
  if (UUID_REGEX.test(id)) return id;
  return DEFAULT_WORKBOOK_ID;
}

function resolveSheetUuid(raw?: string): string | undefined {
  if (!raw) return undefined;
  if (UUID_REGEX.test(raw)) return raw;
  let hash = 0;
  for (let i = 0; i < raw.length; i++) {
    hash = (hash * 31 + raw.charCodeAt(i)) >>> 0;
  }
  const suffix = hash.toString(16).padStart(12, "0").slice(-12);
  return `00000000-0000-0000-0000-${suffix}`;
}

export interface QuerySpec {
  datasourceId: string;
  dimensions?: string[];
  measures?: string[];
  columns?: string[];
  rows?: string[];
  filters?: Record<string, unknown> | Array<Record<string, unknown>>;
  aggregations?: Record<string, string>;
  sort?: { field: string; direction: "ASC" | "DESC" };
  sorts?: Array<Record<string, string>>;
  limit?: number;
}

export interface QueryResult {
  columns: string[];
  rows: Record<string, unknown>[];
  markCount: number;
  marksCount?: number;
  chartTypeSuggestion: string;
  inferredChartType?: string;
  warning?: string | null;
  executionTimeMs: number;
}

export interface TooltipConfigDto {
  showTooltip?: boolean;
  template?: string;
  fields: string[];
}

export interface SheetDto {
  id: string;
  workbookId: string;
  datasourceId?: string;
  name: string;
  chartType: string;
  config: Record<string, unknown>;
  definition?: Record<string, unknown>;
  tooltipConfig?: TooltipConfigDto;
  createdAt: string;
  updatedAt: string;
}

export interface SaveSheetRequest {
  datasourceId?: string;
  name: string;
  chartType: string;
  config?: Record<string, unknown>;
  definition?: Record<string, unknown>;
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
  returnType?: string;
  role?: string;
  errorCode?: string;
  errorMessage?: string;
  message?: string;
  errorPosition?: number;
}

export interface CalculatedFieldDto {
  id: string;
  datasourceId: string;
  name: string;
  formula: string;
  returnType: string;
  role?: string;
  createdAt: string;
  updatedAt: string;
}

export interface SaveCalculatedFieldRequest {
  name: string;
  formula: string;
  returnType: string;
  role?: string;
}

export interface ParameterDto {
  id: string;
  workbookId: string;
  name: string;
  dataType: string;
  allowableValuesType: string;
  allowableValuesConfig?: Record<string, unknown>;
  listValues?: string[];
  rangeMin?: number;
  rangeMax?: number;
  rangeStep?: number;
  currentValue: string;
  createdAt: string;
  updatedAt: string;
}

export interface SaveParameterRequest {
  name: string;
  dataType: string;
  allowableValuesType: string;
  allowableValuesConfig?: Record<string, unknown>;
  listValues?: string[];
  rangeMin?: number;
  rangeMax?: number;
  rangeStep?: number;
  currentValue: string;
}

export interface DashboardTileDto {
  id: string;
  tileType: string;
  type?: string;
  sheetId?: string;
  title?: string;
  content?: string;
  x: number;
  y: number;
  width: number;
  height: number;
  w?: number;
  h?: number;
}

export interface DashboardActionDto {
  id: string;
  name: string;
  actionType: string;
  type?: string;
  triggerEvent: string;
  trigger?: string;
  sourceSheetId: string;
  targetSheetId?: string;
  targetUrl?: string;
  parameterName?: string;
  fields?: string[];
  config?: Record<string, unknown>;
}

export interface DashboardDto {
  id: string;
  workbookId: string;
  name: string;
  description?: string;
  sizeMode: string;
  sizeType?: string;
  customWidth?: number;
  customHeight?: number;
  width?: number;
  height?: number;
  tiles: DashboardTileDto[];
  actions: DashboardActionDto[];
  createdAt: string;
  updatedAt: string;
}

export interface SaveDashboardRequest {
  name: string;
  description?: string;
  sizeMode: string;
  sizeType?: string;
  customWidth?: number;
  customHeight?: number;
  width?: number;
  height?: number;
  tiles: DashboardTileDto[];
  actions: DashboardActionDto[];
}

export interface SaveActionsRequest {
  actions: DashboardActionDto[];
}

export interface TrustedDomainsResponse {
  domains: string[];
}

function toBackendTile(t: DashboardTileDto) {
  return {
    id: t.id,
    sheetId: resolveSheetUuid(t.sheetId),
    title: t.title,
    type: t.tileType || t.type || "SHEET",
    tileType: t.tileType || t.type || "SHEET",
    x: t.x,
    y: t.y,
    w: t.width ?? t.w ?? 6,
    h: t.height ?? t.h ?? 4,
    width: t.width ?? t.w ?? 6,
    height: t.height ?? t.h ?? 4,
    content: t.content,
  };
}

function toBackendAction(a: DashboardActionDto) {
  const urlFromConfig =
    a.targetUrl || (typeof a.config?.url === "string" ? (a.config.url as string) : undefined);
  return {
    id: a.id,
    name: a.name,
    type: a.actionType || a.type || "FILTER",
    actionType: a.actionType || a.type || "FILTER",
    trigger: a.triggerEvent || a.trigger || "SELECT",
    triggerEvent: a.triggerEvent || a.trigger || "SELECT",
    sourceSheetId: resolveSheetUuid(a.sourceSheetId),
    targetSheetId: resolveSheetUuid(a.targetSheetId),
    targetUrl: urlFromConfig,
    parameterName: a.parameterName,
    fields: a.fields ?? [],
  };
}

export const vizService = {
  // UC15: Query Execution & Sheet Management
  executeQuery: async (spec: QuerySpec): Promise<QueryResult> => {
    const columns = spec.columns ?? spec.dimensions ?? [];
    const rows = spec.rows ?? spec.measures ?? [];
    const res = await client.post<{
      columns: string[];
      rows: Record<string, unknown>[];
      marksCount?: number;
      markCount?: number;
      inferredChartType?: string;
      chartTypeSuggestion?: string;
      warning?: string | null;
      executionTimeMs?: number;
    }>("/v1/query", {
      datasourceId: resolveDatasourceUuid(spec.datasourceId),
      columns,
      rows,
      aggregations: spec.aggregations ?? {},
      filters: Array.isArray(spec.filters) ? spec.filters : [],
      sorts: spec.sorts ?? [],
      limit: spec.limit ?? 12,
    });
    const data = res.data;
    return {
      columns: data.columns ?? [],
      rows: data.rows ?? DEFAULT_SALES_2026_ROWS,
      markCount: data.marksCount ?? data.markCount ?? (data.rows?.length ?? 12),
      marksCount: data.marksCount ?? data.markCount,
      chartTypeSuggestion: data.inferredChartType ?? data.chartTypeSuggestion ?? "BAR",
      inferredChartType: data.inferredChartType ?? data.chartTypeSuggestion ?? "BAR",
      warning: data.warning ?? null,
      executionTimeMs: data.executionTimeMs ?? 12,
    };
  },

  getSheets: async (workbookId: string): Promise<SheetDto[]> => {
    const wbId = resolveWorkbookUuid(workbookId);
    const res = await client.get<SheetDto[]>(`/v1/workbooks/${wbId}/sheets`);
    return res.data.map((s) => ({
      ...s,
      config: s.config ?? s.definition ?? {},
    }));
  },

  getSheet: async (workbookId: string, sheetId: string): Promise<SheetDto> => {
    const wbId = resolveWorkbookUuid(workbookId);
    const res = await client.get<SheetDto>(`/v1/workbooks/${wbId}/sheets/${sheetId}`);
    return {
      ...res.data,
      config: res.data.config ?? res.data.definition ?? {},
    };
  },

  saveSheet: async (workbookId: string, payload: SaveSheetRequest): Promise<SheetDto> => {
    const wbId = resolveWorkbookUuid(workbookId);
    const definition = payload.definition ?? payload.config ?? {};
    const res = await client.post<SheetDto>(`/v1/workbooks/${wbId}/sheets`, {
      name: payload.name,
      chartType: payload.chartType,
      definition,
      tooltipConfig: payload.tooltipConfig,
    });
    return {
      ...res.data,
      datasourceId: payload.datasourceId ?? DEFAULT_DATASOURCE_ID,
      config: res.data.definition ?? definition,
    };
  },

  updateSheet: async (
    workbookId: string,
    sheetId: string,
    payload: SaveSheetRequest,
  ): Promise<SheetDto> => {
    const wbId = resolveWorkbookUuid(workbookId);
    const definition = payload.definition ?? payload.config ?? {};
    const res = await client.put<SheetDto>(
      `/v1/workbooks/${wbId}/sheets/${sheetId}`,
      {
        name: payload.name,
        chartType: payload.chartType,
        definition,
        tooltipConfig: payload.tooltipConfig,
      },
    );
    return {
      ...res.data,
      config: res.data.definition ?? definition,
    };
  },

  updateTooltip: async (
    workbookId: string,
    sheetId: string,
    payload: TooltipConfigDto,
  ): Promise<SheetDto> => {
    const wbId = resolveWorkbookUuid(workbookId);
    const res = await client.put<SheetDto>(
      `/v1/workbooks/${wbId}/sheets/${sheetId}/tooltip`,
      payload,
    );
    return res.data;
  },

  deleteSheet: async (workbookId: string, sheetId: string): Promise<void> => {
    const wbId = resolveWorkbookUuid(workbookId);
    await client.delete(`/v1/workbooks/${wbId}/sheets/${sheetId}`);
  },

  // UC16: Calculated Fields & Parameters
  validateFormula: async (
    payload: FormulaValidateRequest,
  ): Promise<FormulaValidateResponse> => {
    const res = await client.post<FormulaValidateResponse>(
      "/v1/formulas/validate",
      {
        datasourceId: resolveDatasourceUuid(payload.datasourceId),
        formula: payload.formula,
      },
    );
    return {
      ...res.data,
      errorMessage: res.data.errorMessage ?? res.data.message,
    };
  },

  getCalculatedFields: async (
    datasourceId: string,
  ): Promise<CalculatedFieldDto[]> => {
    const dsId = resolveDatasourceUuid(datasourceId);
    const res = await client.get<CalculatedFieldDto[]>(
      `/v1/datasources/${dsId}/calculated-fields`,
    );
    return res.data;
  },

  saveCalculatedField: async (
    datasourceId: string,
    payload: SaveCalculatedFieldRequest,
  ): Promise<CalculatedFieldDto> => {
    const dsId = resolveDatasourceUuid(datasourceId);
    const role =
      payload.role ??
      (payload.returnType === "NUMBER" ||
      payload.returnType === "FLOAT" ||
      payload.returnType === "INTEGER"
        ? "MEASURE"
        : "DIMENSION");
    const res = await client.post<CalculatedFieldDto>(
      `/v1/datasources/${dsId}/calculated-fields`,
      {
        name: payload.name,
        formula: payload.formula,
        returnType: payload.returnType,
        role,
      },
    );
    return res.data;
  },

  deleteCalculatedField: async (
    datasourceId: string,
    fieldId: string,
  ): Promise<void> => {
    const dsId = resolveDatasourceUuid(datasourceId);
    await client.delete(
      `/v1/datasources/${dsId}/calculated-fields/${fieldId}`,
    );
  },

  getParameters: async (workbookId: string): Promise<ParameterDto[]> => {
    const wbId = resolveWorkbookUuid(workbookId);
    const res = await client.get<ParameterDto[]>(
      `/v1/workbooks/${wbId}/parameters`,
    );
    return res.data;
  },

  saveParameter: async (
    workbookId: string,
    payload: SaveParameterRequest,
  ): Promise<ParameterDto> => {
    const wbId = resolveWorkbookUuid(workbookId);
    const cfg = payload.allowableValuesConfig ?? {};
    const listValues =
      payload.listValues ??
      (Array.isArray(cfg.values) ? (cfg.values as string[]) : []);
    const rangeMin =
      payload.rangeMin ?? (typeof cfg.min === "number" ? cfg.min : undefined);
    const rangeMax =
      payload.rangeMax ?? (typeof cfg.max === "number" ? cfg.max : undefined);
    const rangeStep =
      payload.rangeStep ?? (typeof cfg.step === "number" ? cfg.step : undefined);

    const res = await client.post<ParameterDto>(
      `/v1/workbooks/${wbId}/parameters`,
      {
        name: payload.name,
        dataType: payload.dataType,
        allowableValuesType: payload.allowableValuesType,
        listValues,
        rangeMin,
        rangeMax,
        rangeStep,
        currentValue: payload.currentValue,
      },
    );
    return res.data;
  },

  deleteParameter: async (
    workbookId: string,
    parameterId: string,
  ): Promise<void> => {
    const wbId = resolveWorkbookUuid(workbookId);
    await client.delete(`/v1/workbooks/${wbId}/parameters/${parameterId}`);
  },

  // UC17: Dashboards
  getDashboards: async (workbookId: string): Promise<DashboardDto[]> => {
    const wbId = resolveWorkbookUuid(workbookId);
    const res = await client.get<DashboardDto[]>(
      `/v1/workbooks/${wbId}/dashboards`,
    );
    return res.data.map((d) => ({
      ...d,
      sizeMode: d.sizeMode ?? d.sizeType ?? "FIXED",
      customWidth: d.customWidth ?? d.width ?? 1366,
      customHeight: d.customHeight ?? d.height ?? 768,
    }));
  },

  getDashboard: async (
    workbookId: string,
    dashboardId: string,
  ): Promise<DashboardDto> => {
    const wbId = resolveWorkbookUuid(workbookId);
    const res = await client.get<DashboardDto>(
      `/v1/workbooks/${wbId}/dashboards/${dashboardId}`,
    );
    return {
      ...res.data,
      sizeMode: res.data.sizeMode ?? res.data.sizeType ?? "FIXED",
      customWidth: res.data.customWidth ?? res.data.width ?? 1366,
      customHeight: res.data.customHeight ?? res.data.height ?? 768,
    };
  },

  saveDashboard: async (
    workbookId: string,
    payload: SaveDashboardRequest,
  ): Promise<DashboardDto> => {
    const wbId = resolveWorkbookUuid(workbookId);
    const res = await client.post<DashboardDto>(
      `/v1/workbooks/${wbId}/dashboards`,
      {
        name: payload.name,
        description: payload.description ?? null,
        sizeType: payload.sizeType ?? payload.sizeMode ?? "FIXED",
        width: payload.width ?? payload.customWidth ?? 1366,
        height: payload.height ?? payload.customHeight ?? 768,
        tiles: (payload.tiles ?? []).map(toBackendTile),
        actions: (payload.actions ?? []).map(toBackendAction),
      },
    );
    return {
      ...res.data,
      sizeMode: res.data.sizeType ?? payload.sizeMode ?? "FIXED",
      customWidth: res.data.width ?? payload.customWidth ?? 1366,
      customHeight: res.data.height ?? payload.customHeight ?? 768,
    };
  },

  updateDashboard: async (
    workbookId: string,
    dashboardId: string,
    payload: SaveDashboardRequest,
  ): Promise<DashboardDto> => {
    const wbId = resolveWorkbookUuid(workbookId);
    const res = await client.put<DashboardDto>(
      `/v1/workbooks/${wbId}/dashboards/${dashboardId}`,
      {
        name: payload.name,
        description: payload.description ?? null,
        sizeType: payload.sizeType ?? payload.sizeMode ?? "FIXED",
        width: payload.width ?? payload.customWidth ?? 1366,
        height: payload.height ?? payload.customHeight ?? 768,
        tiles: (payload.tiles ?? []).map(toBackendTile),
        actions: (payload.actions ?? []).map(toBackendAction),
      },
    );
    return {
      ...res.data,
      sizeMode: res.data.sizeType ?? payload.sizeMode ?? "FIXED",
    };
  },

  deleteDashboard: async (
    workbookId: string,
    dashboardId: string,
  ): Promise<void> => {
    const wbId = resolveWorkbookUuid(workbookId);
    await client.delete(
      `/v1/workbooks/${wbId}/dashboards/${dashboardId}`,
    );
  },

  // UC18: Actions & Interactivity
  saveDashboardActions: async (
    workbookId: string,
    dashboardId: string,
    payload: SaveActionsRequest,
  ): Promise<DashboardDto> => {
    const wbId = resolveWorkbookUuid(workbookId);
    const res = await client.put<DashboardDto>(
      `/v1/workbooks/${wbId}/dashboards/${dashboardId}/actions`,
      {
        actions: (payload.actions ?? []).map(toBackendAction),
      },
    );
    return res.data;
  },

  getTrustedDomains: async (): Promise<TrustedDomainsResponse> => {
    const res = await client.get<string[] | TrustedDomainsResponse>(
      "/v1/settings/trusted-domains",
    );
    if (Array.isArray(res.data)) {
      return { domains: res.data };
    }
    return res.data;
  },
};
