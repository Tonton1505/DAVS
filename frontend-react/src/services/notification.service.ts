import client from "../lib/axios/client";
import { API } from "../common/constants/api-endpoints";
import type { Notification } from "../common/constants/types";
export const notificationService = {
  list: async () => (await client.get<Notification[]>(API.notifications)).data,
};
