import client from "../lib/axios/client";
import { API } from "../common/constants/api-endpoints";
import type { DataSource } from "../common/constants/types";
export const datasourceService = {
  list: async () => (await client.get<DataSource[]>(API.sources)).data,
};
