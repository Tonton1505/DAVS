import client from "../lib/axios/client";
export const integrationService = {
  connect: async (data: unknown) => (await client.post("/integrations", data)).data,
};
