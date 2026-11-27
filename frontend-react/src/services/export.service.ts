import type { AxiosResponse } from "axios";
import client, { isOfflineOrMockError } from "../lib/axios/client";
import { offlineDemoStore } from "./offline-demo.store";

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
    try {
      const res = await client.get<ExportLimitsDto>("/v1/exports/limits");
      return res.data;
    } catch (err) {
      if (isOfflineOrMockError(err)) return offlineDemoStore.getExportLimits();
      throw err;
    }
  },

  download: async (data: ExportRequest) => {
    try {
      const res = await client.post("/v1/exports", data, {
        responseType: "blob",
      });
      return res;
    } catch (err) {
      if (isOfflineOrMockError(err)) {
        const built = offlineDemoStore.buildExportResponse(data);
        return {
          data: built.data,
          status: 200,
          statusText: "OK",
          headers: built.headers,
          config: {} as AxiosResponse["config"],
        } as AxiosResponse<Blob>;
      }
      throw err;
    }
  },
};
