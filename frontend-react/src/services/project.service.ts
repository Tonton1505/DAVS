import client from "../lib/axios/client";

export type PermissionMode = "INHERIT" | "CUSTOM" | "LOCKED";

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

export interface CreateProjectPayload {
  name: string;
  description?: string;
  parentId?: string | null;
  permissionMode: PermissionMode;
}

export const projectService = {
  async list(): Promise<ProjectDto[]> {
    const { data } = await client.get<ProjectDto[]>("/v1/projects");
    return data;
  },

  async get(id: string): Promise<ProjectDto> {
    const { data } = await client.get<ProjectDto>(`/v1/projects/${id}`);
    return data;
  },

  async create(payload: CreateProjectPayload): Promise<ProjectDto> {
    const { data } = await client.post<ProjectDto>("/v1/projects", payload);
    return data;
  },
};
