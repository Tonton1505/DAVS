import client, { isOfflineOrMockError } from "../lib/axios/client";
import type { Comment } from "../common/constants/types";
import { offlineDemoStore } from "./offline-demo.store";

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
  editable?: boolean;
  deletable?: boolean;
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

function normalizeComment(raw: CommentDto): CommentDto {
  return {
    ...raw,
    canEdit: raw.canEdit ?? raw.editable ?? false,
    canDelete: raw.canDelete ?? raw.deletable ?? false,
    replies: Array.isArray(raw.replies) ? raw.replies.map(normalizeComment) : [],
  };
}

export const commentService = {
  list: async (): Promise<Comment[]> => {
    try {
      const list = await commentService.listByResource(
        "DASHBOARD",
        "00000000-0000-0000-0000-000000000501",
      );
      return list.map((c) => ({
        id: c.id,
        author: c.authorName || c.authorEmail,
        body: c.text,
        time: new Date(c.createdAt).toLocaleTimeString("vi-VN", {
          hour: "2-digit",
          minute: "2-digit",
        }),
        mine: c.canEdit,
        snapshot: !!c.snapshotUrl,
      }));
    } catch {
      return [];
    }
  },

  listByResource: async (resourceType: string, resourceId: string) => {
    try {
      const res = await client.get<CommentDto[]>(
        `/v1/resources/${resourceType}/${resourceId}/comments`,
      );
      return res.data.map(normalizeComment);
    } catch (err) {
      if (isOfflineOrMockError(err)) {
        return offlineDemoStore.listComments(resourceType, resourceId);
      }
      throw err;
    }
  },

  create: async (resourceType: string, resourceId: string, data: CreateCommentRequest) => {
    try {
      const res = await client.post<CommentDto>(
        `/v1/resources/${resourceType}/${resourceId}/comments`,
        data,
      );
      return normalizeComment(res.data);
    } catch (err) {
      if (isOfflineOrMockError(err)) {
        return offlineDemoStore.createComment(resourceType, resourceId, data);
      }
      throw err;
    }
  },

  update: async (commentId: string, data: UpdateCommentRequest) => {
    try {
      await client.put(`/v1/comments/${commentId}`, data);
    } catch (err) {
      if (isOfflineOrMockError(err)) {
        offlineDemoStore.updateComment(commentId, data.text);
        return;
      }
      throw err;
    }
  },

  delete: async (commentId: string) => {
    try {
      await client.delete(`/v1/comments/${commentId}`);
    } catch (err) {
      if (isOfflineOrMockError(err)) {
        offlineDemoStore.deleteComment(commentId);
        return;
      }
      throw err;
    }
  },

  reply: async (commentId: string, data: CreateReplyRequest) => {
    try {
      const res = await client.post<CommentDto>(`/v1/comments/${commentId}/replies`, data);
      return normalizeComment(res.data);
    } catch (err) {
      if (isOfflineOrMockError(err)) {
        return offlineDemoStore.replyComment(commentId, data);
      }
      throw err;
    }
  },
};
