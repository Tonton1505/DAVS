import client, { isOfflineOrMockError } from "../lib/axios/client";
import { offlineDemoStore } from "./offline-demo.store";

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
    try {
      const response = await client.get<ProjectDto[]>("/v1/projects");
      return response.data;
    } catch (err) {
      if (isOfflineOrMockError(err)) return offlineDemoStore.listProjects();
      throw err;
    }
  },

  async getById(id: string): Promise<ProjectDto> {
    try {
      const response = await client.get<ProjectDto>(`/v1/projects/${id}`);
      return response.data;
    } catch (err) {
      if (isOfflineOrMockError(err)) return offlineDemoStore.getProjectById(id);
      throw err;
    }
  },

  async getTree(): Promise<ProjectTreeNodeDto[]> {
    try {
      const response = await client.get<ProjectTreeNodeDto[]>("/v1/projects/tree");
      return response.data;
    } catch (err) {
      if (isOfflineOrMockError(err)) return offlineDemoStore.getProjectTree();
      throw err;
    }
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
    try {
      const response = await client.get<ContentListResponse>("/v1/content", {
        params: queryParams,
      });
      return response.data;
    } catch (err) {
      if (isOfflineOrMockError(err)) return offlineDemoStore.getContent(params);
      throw err;
    }
  },

  async create(payload: CreateProjectPayload): Promise<ProjectDto> {
    try {
      const response = await client.post<ProjectDto>("/v1/projects", payload);
      return response.data;
    } catch (err) {
      if (isOfflineOrMockError(err)) return offlineDemoStore.createProject(payload);
      throw err;
    }
  },

  async rename(id: string, payload: UpdateProjectPayload): Promise<ProjectDto> {
    try {
      const response = await client.patch<ProjectDto>(`/v1/projects/${id}`, payload);
      return response.data;
    } catch (err) {
      if (isOfflineOrMockError(err)) return offlineDemoStore.renameProject(id, payload);
      throw err;
    }
  },

  async move(id: string, targetId: string | null): Promise<ProjectDto> {
    try {
      const response = await client.post<ProjectDto>(`/v1/projects/${id}/move`, {
        targetId,
      });
      return response.data;
    } catch (err) {
      if (isOfflineOrMockError(err)) return offlineDemoStore.moveProject(id, targetId);
      throw err;
    }
  },

  async changeOwner(id: string, userId: string): Promise<ProjectDto> {
    try {
      const response = await client.post<ProjectDto>(`/v1/projects/${id}/owner`, {
        userId,
      });
      return response.data;
    } catch (err) {
      if (isOfflineOrMockError(err)) return offlineDemoStore.changeProjectOwner(id, userId);
      throw err;
    }
  },

  async deleteProject(id: string, confirmName: string): Promise<void> {
    try {
      await client.delete(`/v1/projects/${id}`, {
        data: { confirmName },
        params: { confirmName },
      });
    } catch (err) {
      if (isOfflineOrMockError(err)) {
        offlineDemoStore.deleteProject(id);
        return;
      }
      throw err;
    }
  },

  async restoreProject(id: string): Promise<ProjectDto> {
    try {
      const response = await client.post<ProjectDto>(`/v1/projects/${id}/restore`);
      return response.data;
    } catch (err) {
      if (isOfflineOrMockError(err)) return offlineDemoStore.restoreProject(id);
      throw err;
    }
  },

  async listCollections(): Promise<CollectionDto[]> {
    try {
      const response = await client.get<CollectionDto[]>("/v1/collections");
      return response.data;
    } catch (err) {
      if (isOfflineOrMockError(err)) return offlineDemoStore.listCollections();
      throw err;
    }
  },

  async createCollection(payload: {
    name: string;
    description?: string;
  }): Promise<CollectionDto> {
    try {
      const response = await client.post<CollectionDto>("/v1/collections", payload);
      return response.data;
    } catch (err) {
      if (isOfflineOrMockError(err)) {
        return offlineDemoStore.createCollection(payload.name, payload.description);
      }
      throw err;
    }
  },

  async addToCollection(
    collectionId: string,
    payload: { resourceType: string; resourceId: string },
  ): Promise<CollectionDto> {
    try {
      const response = await client.post<CollectionDto>(
        `/v1/collections/${collectionId}/items`,
        payload,
      );
      return response.data;
    } catch (err) {
      if (isOfflineOrMockError(err)) {
        return offlineDemoStore.addToCollection(
          collectionId,
          payload.resourceType === "PROJECT" ? "PROJECT" : "WORKBOOK",
          payload.resourceId,
        );
      }
      throw err;
    }
  },
};
