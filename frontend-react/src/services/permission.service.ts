import client from "../lib/axios/client";
export const permissionService = { list: async () => (await client.get("/permissions")).data };
