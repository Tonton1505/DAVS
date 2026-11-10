import client from "../lib/axios/client";
export const exportService = {
  create: async (data: unknown) => (await client.post("/export", data)).data,
};
