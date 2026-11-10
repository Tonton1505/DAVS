import client from "../lib/axios/client";
import { API } from "../common/constants/api-endpoints";
import type { Comment } from "../common/constants/types";
export const commentService = {
  list: async () => (await client.get<Comment[]>(API.comments)).data,
};
