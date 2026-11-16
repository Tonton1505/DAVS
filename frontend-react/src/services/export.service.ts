import client from "../lib/axios/client";

export interface ExportLimitsDto {
  maxRows: number;
  maxBytes: number;
  timeoutSeconds: number;
}

export interface ExportRequest {
  dashboardId: string;
  workbookId?: string | null;
  format: "CSV" | "EXCEL" | "PDF" | "PNG";
  scope: "view" | "all_sheets" | "full_data";
  paperOrientation?: string | null;
  paperSize?: string | null;
  customParameters?: Record<string, unknown>;
}

export const exportService = {
  getLimits: async () => {
    const res = await client.get<ExportLimitsDto>("/v1/exports/limits");
    return res.data;
  },

  download: async (data: ExportRequest) => {
    const res = await client.post("/v1/exports", data, {
      responseType: "blob",
    });
    return res;
  },
};
