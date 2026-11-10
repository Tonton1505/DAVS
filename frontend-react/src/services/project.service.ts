import client from "../lib/axios/client";
import { API } from "../common/constants/api-endpoints";
export const projectService = { list: async () => (await client.get(API.projects)).data };
