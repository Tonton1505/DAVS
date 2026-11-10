import client from "../lib/axios/client";
export const vizService = { save: async (data: unknown) => (await client.post("/viz", data)).data };
