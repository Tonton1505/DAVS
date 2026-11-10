import client from "../lib/axios/client";
export const prepService = { run: async () => (await client.post("/prep/run")).data };
