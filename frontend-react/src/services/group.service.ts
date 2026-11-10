import client from "../lib/axios/client";
export const groupService = { list: async () => (await client.get("/groups")).data };
