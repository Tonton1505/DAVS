import client from "../lib/axios/client";
export const labelService = { list: async () => (await client.get("/labels")).data };
