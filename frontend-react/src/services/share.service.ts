import client from "../lib/axios/client";

export interface ShareRequest {
  resourceType: "WORKBOOK" | "DASHBOARD" | "VIEW";
  resourceId: string;
  recipients: string[];
  permissionLevel: "VIEW" | "INTERACT" | "EDIT";
  message?: string;
  sendEmailNotification?: boolean;
}

export interface ShareResponse {
  success: boolean;
  sharedCount: number;
  shareLink: string;
  embedCode: string;
  recipients: string[];
}

export interface DirectoryItem {
  id: string;
  type: "USER" | "GROUP";
  name: string;
  email?: string;
  avatar?: string;
  isExternal?: boolean;
}

export const shareService = {
  shareContent: async (data: ShareRequest): Promise<ShareResponse> => {
    const res = await client.post<ShareResponse>("/v1/shares", data);
    return res.data;
  },
  send: async (data: unknown) => (await client.post("/v1/shares", data)).data,
  getShareLink: async (
    resourceType: string,
    resourceId: string,
  ): Promise<{ shareLink: string; embedCode: string }> => {
    const res = await client.get<{ shareLink: string; embedCode: string }>(
      `/v1/shares/link?resourceType=${resourceType}&resourceId=${resourceId}`,
    );
    return res.data;
  },
  searchDirectory: async (query?: string): Promise<DirectoryItem[]> => {
    const res = await client.get<DirectoryItem[]>(
      `/v1/directory${query ? `?q=${encodeURIComponent(query)}` : ""}`,
    );
    return res.data;
  },
};
