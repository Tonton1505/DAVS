import { apiClient } from '../lib/axios';
import { API_ENDPOINTS } from '../common/constants/api-endpoints';

export interface CreateProjectPayload {
  projectName: string;
  description?: string;
  parentProjectId?: number | null;
  permissionTemplate: 'INHERIT' | 'CUSTOM' | 'LOCKED';
}

export const projectService = {
  async createProject(payload: CreateProjectPayload) {
    const response = await apiClient.post(API_ENDPOINTS.PROJECTS, payload);
    return response.data;
  },
};
