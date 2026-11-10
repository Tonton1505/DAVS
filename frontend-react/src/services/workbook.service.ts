import client from "../lib/axios/client";
import { API } from "../common/constants/api-endpoints";
import type { DashboardItem } from "../common/constants/types";
export const workbookService = {
  list: async () => (await client.get<DashboardItem[]>(API.dashboards)).data,
};
