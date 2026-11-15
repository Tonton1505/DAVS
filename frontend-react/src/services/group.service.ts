import client from "../lib/axios/client";
import type { BackendSiteRole } from "./auth.service";

export interface GroupDto {
  id: string;
  name: string;
  description: string | null;
  minRole: BackendSiteRole | null;
  isSystem: boolean;
  memberCount: number;
  createdAt: string;
  updatedAt: string;
}

export interface GroupMemberItem {
  userId: string;
  email: string;
  fullName: string;
  siteRole: BackendSiteRole;
  status: string;
  joinedAt: string;
}

export interface GroupDetailDto extends GroupDto {
  members: GroupMemberItem[];
}

export interface CreateGroupPayload {
  name: string;
  description?: string;
  minRole?: BackendSiteRole | null;
  userIds?: string[];
}

export interface UpdateGroupPayload {
  name?: string;
  description?: string;
  minRole?: BackendSiteRole | null;
}

export const groupService = {
  async list(q?: string): Promise<GroupDto[]> {
    const { data } = await client.get<GroupDto[]>("/v1/groups", {
      params: q ? { q } : undefined,
    });
    return data;
  },

  async getDetail(id: string): Promise<GroupDetailDto> {
    const { data } = await client.get<GroupDetailDto>(`/v1/groups/${id}`);
    return data;
  },

  async create(payload: CreateGroupPayload): Promise<GroupDetailDto> {
    const { data } = await client.post<GroupDetailDto>("/v1/groups", payload);
    return data;
  },

  async update(id: string, payload: UpdateGroupPayload): Promise<GroupDetailDto> {
    const { data } = await client.patch<GroupDetailDto>(`/v1/groups/${id}`, payload);
    return data;
  },

  async remove(id: string): Promise<void> {
    await client.delete(`/v1/groups/${id}`);
  },

  async addMembers(groupId: string, userIds: string[]): Promise<GroupDetailDto> {
    const { data } = await client.post<GroupDetailDto>(`/v1/groups/${groupId}/members`, {
      userIds,
    });
    return data;
  },

  async removeMember(groupId: string, userId: string): Promise<GroupDetailDto> {
    const { data } = await client.delete<GroupDetailDto>(`/v1/groups/${groupId}/members/${userId}`);
    return data;
  },
};
