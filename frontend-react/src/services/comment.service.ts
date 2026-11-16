import client from "../lib/axios/client";
import type { Comment } from "../common/constants/types";

export interface CommentDto {
  id: string;
  resourceType: string;
  resourceId: string;
  parentId: string | null;
  authorId: string;
  authorName: string;
  authorEmail: string;
  text: string;
  mentions: string[];
  snapshotUrl: string | null;
  canEdit: boolean;
  canDelete: boolean;
  createdAt: string;
  updatedAt: string;
  replies: CommentDto[];
}

export interface CreateCommentRequest {
  text: string;
  mentions?: string[];
  snapshot?: boolean;
  snapshotUrl?: string | null;
  parentId?: string | null;
}

export interface UpdateCommentRequest {
  text: string;
}

export interface CreateReplyRequest {
  text: string;
  mentions?: string[];
}

export const commentService = {
  list: async (): Promise<Comment[]> => {
    try {
      const res = await client.get<CommentDto[]>(
        "/api/v1/resources/DASHBOARD/00000000-0000-0000-0000-000000000501/comments"
      );
      return res.data.map((c) => ({
        id: c.id,
        author: c.authorName || c.authorEmail,
        body: c.text,
        time: new Date(c.createdAt).toLocaleTimeString("vi-VN", { hour: "2-digit", minute: "2-digit" }),
        mine: c.canEdit,
        snapshot: !!c.snapshotUrl,
      }));
    } catch {
      return [];
    }
  },

  listByResource: async (resourceType: string, resourceId: string) => {
    const res = await client.get<CommentDto[]>(`/api/v1/resources/${resourceType}/${resourceId}/comments`);
    return res.data;
  },

  create: async (resourceType: string, resourceId: string, data: CreateCommentRequest) => {
    const res = await client.post<CommentDto>(`/api/v1/resources/${resourceType}/${resourceId}/comments`, data);
    return res.data;
  },

  update: async (commentId: string, data: UpdateCommentRequest) => {
    await client.put(`/api/v1/comments/${commentId}`, data);
  },

  delete: async (commentId: string) => {
    await client.delete(`/api/v1/comments/${commentId}`);
  },

  reply: async (commentId: string, data: CreateReplyRequest) => {
    const res = await client.post<CommentDto>(`/api/v1/comments/${commentId}/replies`, data);
    return res.data;
  },
};
