import client from "../lib/axios/client";

export interface PublishWorkbookRequest {
  destination: "INTERNAL_SERVER" | "CLOUD" | "PUBLIC";
  projectId: string;
  name: string;
  description?: string;
  visibleSheets?: string[];
  dataSourceMode?: "EMBEDDED" | "SEPARATE";
  packagedTwbx?: boolean;
  hasEmbeddedCredentials?: boolean;
  overwrite?: boolean;
}

export interface PublishWorkbookResponse {
  id: string;
  workbookId: string;
  projectId: string;
  destination: string;
  publishedName: string;
  url: string;
  version: number;
  warnings?: string[];
  publishedAt: string;
}

export interface PublishTargetsResponse {
  workbookId: string;
  currentName: string;
  currentProjectId: string;
  projects: Array<{ id: string; name: string; description?: string }>;
  destinations: string[];
}

export const publishService = {
  publishWorkbook: async (
    workbookId: string,
    data: PublishWorkbookRequest,
  ): Promise<PublishWorkbookResponse> => {
    const res = await client.post<PublishWorkbookResponse>(
      `/v1/workbooks/${workbookId}/publish`,
      data,
    );
    return res.data;
  },
  getPublishTargets: async (workbookId: string): Promise<PublishTargetsResponse> => {
    const res = await client.get<PublishTargetsResponse>(
      `/v1/workbooks/${workbookId}/publish/targets`,
    );
    return res.data;
  },
};
