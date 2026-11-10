import client from "../lib/axios/client";
export const shareService = {
  send: async (data: unknown) => (await client.post("/share", data)).data,
};
