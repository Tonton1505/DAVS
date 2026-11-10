import client from "../lib/axios/client";
import { API } from "../common/constants/api-endpoints";
import type { User } from "../common/constants/types";
export const userService = { list: async () => (await client.get<User[]>(API.users)).data };
