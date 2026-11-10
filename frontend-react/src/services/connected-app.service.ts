import client from "../lib/axios/client";
export const connectedAppService = {
  create: async (data: unknown) => (await client.post("/connected-apps", data)).data,
};
