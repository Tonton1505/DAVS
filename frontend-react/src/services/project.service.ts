import client from "../lib/axios/client";

export type PermissionMode = "INHERIT" | "CUSTOM" | "LOCKED";
export type ProjectPermissionMode = PermissionMode | "CUSTOMIZABLE";

export interface ProjectDto {
  id: string;
  name: string;
  description: string | null;
  parentId: string | null;
  ownerId: string | null;
  permissionMode: string;
  isDefault: boolean;
  depth: number;
  createdAt: string;
  updatedAt: string;
}

export interface ProjectTreeNodeDto {
  id: string;
  name: string;
  description: string | null;
  parentId: string | null;
  ownerId: string | null;
  permissionMode: string;
  isDefault: boolean;
  depth: number;
  children: ProjectTreeNodeDto[];
}

export interface ContentItemDto {
  id: string;
  resourceType: "PROJECT" | "WORKBOOK";
  name: string;
  description: string | null;
  projectId: string | null;
  ownerId: string | null;
  ownerName: string;
  permissionMode: string;
  isDefault: boolean;
  currentVersion: number;
  childCount: number;
  allowedActions: string[];
  updatedAt: string;
}

export interface ContentListResponse {
  projectId: string | null;
  projectName: string;
  breadcrumbs: ProjectDto[];
  items: ContentItemDto[];
  totalCount: number;
}

export interface CollectionItemDto {
  collectionId: string;
  resourceType: string;
  resourceId: string;
  resourceName: string;
  addedAt: string;
}

export interface CollectionDto {
  id: string;
  name: string;
  description: string | null;
  ownerId: string;
  itemCount: number;
  items: CollectionItemDto[];
  createdAt: string;
  updatedAt: string;
}

export interface CreateProjectPayload {
  name: string;
  description?: string;
  parentId?: string | null;
  permissionMode: "INHERIT" | "CUSTOM" | "LOCKED";
}

export interface UpdateProjectPayload {
  name: string;
  description?: string;
}

export const projectService = {
  async list(): Promise<ProjectDto[]> {
    const response = await client.get<ProjectDto[]>("/v1/projects");
    return response.data;
  },

  async getById(id: string): Promise<ProjectDto> {
    const response = await client.get<ProjectDto>(`/v1/projects/${id}`);
    return response.data;
  },

  async getTree(): Promise<ProjectTreeNodeDto[]> {
    const response = await client.get<ProjectTreeNodeDto[]>("/v1/projects/tree");
    return response.data;
  },

  async getContent(params?: {
    projectId?: string | null;
    q?: string;
    sort?: "updatedAt" | "name" | "owner";
  }): Promise<ContentListResponse> {
    const queryParams: Record<string, string> = {};
    if (params?.projectId) {
      queryParams.projectId = params.projectId;
    }
    if (params?.q && params.q.trim()) {
      queryParams.q = params.q.trim();
    }
    if (params?.sort) {
      queryParams.sort = params.sort;
    }
    const response = await client.get<ContentListResponse>("/v1/content", {
      params: queryParams,
    });
    return response.data;
  },

  async create(payload: CreateProjectPayload): Promise<ProjectDto> {
    const response = await client.post<ProjectDto>("/v1/projects", payload);
    return response.data;
  },

  async rename(id: string, payload: UpdateProjectPayload): Promise<ProjectDto> {
    const response = await client.patch<ProjectDto>(`/v1/projects/${id}`, payload);
    return response.data;
  },

  async move(id: string, targetId: string | null): Promise<ProjectDto> {
    const response = await client.post<ProjectDto>(`/v1/projects/${id}/move`, {
      targetId,
    });
    return response.data;
  },

  async changeOwner(id: string, userId: string): Promise<ProjectDto> {
    const response = await client.post<ProjectDto>(`/v1/projects/${id}/owner`, {
      userId,
    });
    return response.data;
  },

  async deleteProject(id: string, confirmName: string): Promise<void> {
    await client.delete(`/v1/projects/${id}`, {
      data: { confirmName },
      params: { confirmName },
    });
  },

  async restoreProject(id: string): Promise<ProjectDto> {
    const response = await client.post<ProjectDto>(`/v1/projects/${id}/restore`);
    return response.data;
  },

  async listCollections(): Promise<CollectionDto[]> {
    const response = await client.get<CollectionDto[]>("/v1/collections");
    return response.data;
  },

  async createCollection(payload: {
    name: string;
    description?: string;
  }): Promise<CollectionDto> {
    const response = await client.post<CollectionDto>("/v1/collections", payload);
    return response.data;
  },

  async addToCollection(
    collectionId: string,
    payload: { resourceType: string; resourceId: string },
  ): Promise<CollectionDto> {
    const response = await client.post<CollectionDto>(
      `/v1/collections/${collectionId}/items`,
      payload,
    );
    return response.data;
  },
};
