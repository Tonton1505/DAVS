import client from "../lib/axios/client";

export type SubjectType = "USER" | "GROUP";
export type Effect = "ALLOW" | "DENY";
export type UiEffect = Effect | "UNSPECIFIED";
export type ResourceType = "project" | "workbook" | "datasource" | "dashboard";

export const ALL_CAPABILITIES = [
  { code: "VIEW", label: "Xem" },
  { code: "INTERACT", label: "Tương tác" },
  { code: "FILTER", label: "Lọc" },
  { code: "DOWNLOAD", label: "Tải xuống" },
  { code: "COMMENT", label: "Bình luận" },
  { code: "WEB_EDIT", label: "Chỉnh sửa web" },
  { code: "SAVE", label: "Lưu" },
  { code: "SHARE", label: "Chia sẻ" },
  { code: "DELETE", label: "Xóa" },
  { code: "CHANGE_PERMISSIONS", label: "Đổi quyền" },
  { code: "PUBLISH", label: "Xuất bản" },
  { code: "CONNECT", label: "Kết nối" },
] as const;

export type Capability = (typeof ALL_CAPABILITIES)[number]["code"];

export interface PermissionRuleDto {
  id: string;
  subjectType: SubjectType;
  subjectId: string;
  subjectName: string;
  subjectDetail: string | null;
  capability: Capability;
  effect: Effect;
}

export interface ResourcePermissionsResponse {
  resourceType: string;
  resourceId: string;
  resourceName: string;
  permissionMode: string;
  lockedByParent: boolean;
  canEdit: boolean;
  rules: PermissionRuleDto[];
}

export interface RuleInput {
  subjectType: SubjectType;
  subjectId: string;
  capability: Capability;
  effect: Effect;
}

export interface UpdateResourcePermissionsResponse {
  permissions: ResourcePermissionsResponse;
  warnings: string[];
}

export interface EffectivePermissionsResponse {
  userId: string;
  displayName: string;
  siteRole: string;
  resourceType: string;
  resourceId: string;
  lockedByParent: boolean;
  effectiveCapabilities: Capability[];
}

export interface PermissionTemplateDto {
  id: string;
  name: string;
  description: string;
  capabilities: Capability[];
}

export interface PermissionSubjectDto {
  subjectType: SubjectType;
  id: string;
  name: string;
  detail: string | null;
}

export const permissionService = {
  async listTemplates(): Promise<PermissionTemplateDto[]> {
    const { data } = await client.get<PermissionTemplateDto[]>("/v1/permission-templates");
    return data;
  },

  async getPermissions(
    resourceType: ResourceType,
    resourceId: string,
  ): Promise<ResourcePermissionsResponse> {
    const { data } = await client.get<ResourcePermissionsResponse>(
      `/v1/resources/${resourceType}/${resourceId}/permissions`,
    );
    return data;
  },

  async updatePermissions(
    resourceType: ResourceType,
    resourceId: string,
    rules: RuleInput[],
  ): Promise<UpdateResourcePermissionsResponse> {
    const { data } = await client.put<UpdateResourcePermissionsResponse>(
      `/v1/resources/${resourceType}/${resourceId}/permissions`,
      { rules },
    );
    return data;
  },

  async getEffective(
    resourceType: ResourceType,
    resourceId: string,
    userId: string,
  ): Promise<EffectivePermissionsResponse> {
    const { data } = await client.get<EffectivePermissionsResponse>(
      `/v1/resources/${resourceType}/${resourceId}/permissions/effective`,
      { params: { userId } },
    );
    return data;
  },

  async listSubjects(
    resourceType: ResourceType,
    resourceId: string,
  ): Promise<PermissionSubjectDto[]> {
    const { data } = await client.get<PermissionSubjectDto[]>(
      `/v1/resources/${resourceType}/${resourceId}/permissions/subjects`,
    );
    return data;
  },
};
