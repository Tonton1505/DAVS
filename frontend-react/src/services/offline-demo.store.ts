import type { DashboardItem } from "../common/constants/types";
import { buildValidPdfBlob, buildValidXlsxBlob } from "../utils/excel-parser";
import type { CommentDto, CreateCommentRequest, CreateReplyRequest } from "./comment.service";
import type { CreateCustomViewRequest, CustomViewDto, SaveAsWorkbookRequest } from "./custom-view.service";
import type { ExportLimitsDto, ExportRequest } from "./export.service";
import type { LabelCategoryDto, LabelDto, LabelTargetRef } from "./label.service";
import type {
  Capability,
  EffectivePermissionsResponse,
  PermissionRuleDto,
  PermissionSubjectDto,
  PermissionTemplateDto,
  ResourcePermissionsResponse,
  ResourceType,
  RuleInput,
  UpdateResourcePermissionsResponse,
} from "./permission.service";
import type {
  CollectionDto,
  ContentItemDto,
  ContentListResponse,
  CreateProjectPayload,
  ProjectDto,
  ProjectTreeNodeDto,
  UpdateProjectPayload,
} from "./project.service";
import type { LicensePoolDto } from "./user.service";
import type { UploadWorkbookInput, WorkbookDto, WorkbookVersionDto } from "./workbook.service";

const DEFAULT_PROJECT_ID = "00000000-0000-0000-0000-000000000001";
const SALES_PROJECT_ID = "00000000-0000-0000-0000-000000000002";
const DEFAULT_WORKBOOK_ID = "00000000-0000-0000-0000-000000000101";
const Q4_WORKBOOK_ID = "00000000-0000-0000-0000-000000000401";
const DEFAULT_DASHBOARD_ID = "00000000-0000-0000-0000-000000000501";

const ADMIN_USER_ID = "00000000-0000-0000-0000-000000000011";
const CREATOR_USER_ID = "00000000-0000-0000-0000-000000000012";
const EXPLORER_USER_ID = "00000000-0000-0000-0000-000000000013";
const VIEWER_USER_ID = "00000000-0000-0000-0000-000000000014";
const ALL_USERS_GROUP_ID = "00000000-0000-0000-0000-000000000021";

const VALID_1X1_PNG = new Uint8Array([
  0x89, 0x50, 0x4e, 0x47, 0x0d, 0x0a, 0x1a, 0x0a, 0x00, 0x00, 0x00, 0x0d, 0x49, 0x48, 0x44, 0x52,
  0x00, 0x00, 0x00, 0x01, 0x00, 0x00, 0x00, 0x01, 0x08, 0x02, 0x00, 0x00, 0x00, 0x90, 0x77, 0x53,
  0xde, 0x00, 0x00, 0x00, 0x0c, 0x49, 0x44, 0x41, 0x54, 0x08, 0xd7, 0x63, 0xf8, 0xff, 0xff, 0x3f,
  0x00, 0x05, 0xfe, 0x02, 0xfe, 0xdc, 0xcc, 0x59, 0xe7, 0x00, 0x00, 0x00, 0x00, 0x49, 0x45, 0x4e,
  0x44, 0xae, 0x42, 0x60, 0x82,
]);

function nextUuid(): string {
  if (typeof crypto !== "undefined" && typeof crypto.randomUUID === "function") {
    return crypto.randomUUID();
  }
  return `00000000-0000-4000-8000-${String(Date.now()).padStart(12, "0").slice(-12)}`;
}

interface StoredProject extends ProjectDto {
  deletedAt: string | null;
}

interface OfflineState {
  projects: StoredProject[];
  workbooks: WorkbookDto[];
  versions: Record<string, WorkbookVersionDto[]>;
  uploadedBlobs: Record<string, Blob>;
  collections: CollectionDto[];
  categories: LabelCategoryDto[];
  labels: LabelDto[];
  labelTargets: Record<string, Set<string>>;
  permissionRules: Record<string, PermissionRuleDto[]>;
  customViews: CustomViewDto[];
  comments: CommentDto[];
}

const state: OfflineState = {
  projects: [
    {
      id: DEFAULT_PROJECT_ID,
      name: "Dự án Mặc định (Default)",
      description: "Thư mục dự án hệ thống mặc định cho người dùng",
      parentId: null,
      ownerId: ADMIN_USER_ID,
      permissionMode: "CUSTOM",
      isDefault: true,
      depth: 1,
      createdAt: "2026-01-01T00:00:00Z",
      updatedAt: "2026-01-01T00:00:00Z",
      deletedAt: null,
    },
    {
      id: SALES_PROJECT_ID,
      name: "Phân tích Bán hàng & Thị trường",
      description: "Dự án lưu trữ các báo cáo doanh số và phân tích thị trường",
      parentId: null,
      ownerId: CREATOR_USER_ID,
      permissionMode: "CUSTOM",
      isDefault: false,
      depth: 1,
      createdAt: "2026-01-02T00:00:00Z",
      updatedAt: "2026-01-02T00:00:00Z",
      deletedAt: null,
    },
  ],
  workbooks: [
    {
      id: DEFAULT_WORKBOOK_ID,
      projectId: DEFAULT_PROJECT_ID,
      projectName: "Dự án Mặc định (Default)",
      name: "Phân tích Doanh thu Bán hàng 2026",
      description: "Workbook phân tích số liệu tài chính theo vùng miền",
      currentVersion: 2,
      ownerId: CREATOR_USER_ID,
      ownerName: "Demo Creator",
      importStatus: "IMPORTED",
      needsReconnect: false,
      sizeBytes: 24_576,
      fileName: "doanh_thu_2026.xlsx",
      labels: [],
      createdAt: "2026-01-03T00:00:00Z",
      updatedAt: "2026-01-03T00:00:00Z",
    },
    {
      id: Q4_WORKBOOK_ID,
      projectId: SALES_PROJECT_ID,
      projectName: "Phân tích Bán hàng & Thị trường",
      name: "Báo cáo bán hàng Q4",
      description: "Dashboard phân tích doanh số và hiệu suất sản phẩm quý 4",
      currentVersion: 1,
      ownerId: CREATOR_USER_ID,
      ownerName: "Demo Creator",
      importStatus: "IMPORTED",
      needsReconnect: false,
      sizeBytes: 18_432,
      fileName: "bao_cao_q4.xlsx",
      labels: [],
      createdAt: "2026-01-04T00:00:00Z",
      updatedAt: "2026-01-04T00:00:00Z",
    },
  ],
  versions: {
    [DEFAULT_WORKBOOK_ID]: [
      {
        id: "00000000-0000-0000-0000-000000000802",
        workbookId: DEFAULT_WORKBOOK_ID,
        versionNumber: 2,
        fileName: "doanh_thu_2026_v2.xlsx",
        sizeBytes: 24_576,
        note: "Cập nhật dữ liệu Quý 4",
        createdBy: CREATOR_USER_ID,
        createdByName: "Demo Creator",
        createdAt: "2026-01-03T00:00:00Z",
        current: true,
        purged: false,
      },
      {
        id: "00000000-0000-0000-0000-000000000801",
        workbookId: DEFAULT_WORKBOOK_ID,
        versionNumber: 1,
        fileName: "doanh_thu_2026_v1.xlsx",
        sizeBytes: 20_480,
        note: "Phiên bản tải lên ban đầu",
        createdBy: CREATOR_USER_ID,
        createdByName: "Demo Creator",
        createdAt: "2026-01-01T00:00:00Z",
        current: false,
        purged: false,
      },
    ],
    [Q4_WORKBOOK_ID]: [
      {
        id: "00000000-0000-0000-0000-000000000803",
        workbookId: Q4_WORKBOOK_ID,
        versionNumber: 1,
        fileName: "bao_cao_q4.xlsx",
        sizeBytes: 18_432,
        note: "Phiên bản khởi tạo",
        createdBy: CREATOR_USER_ID,
        createdByName: "Demo Creator",
        createdAt: "2026-01-04T00:00:00Z",
        current: true,
        purged: false,
      },
    ],
  },
  uploadedBlobs: {},
  collections: [
    {
      id: "00000000-0000-0000-0000-000000000901",
      name: "Báo cáo quan trọng",
      description: "Danh sách các báo cáo thường xuyên theo dõi",
      ownerId: CREATOR_USER_ID,
      itemCount: 1,
      items: [
        {
          collectionId: "00000000-0000-0000-0000-000000000901",
          resourceType: "WORKBOOK",
          resourceId: DEFAULT_WORKBOOK_ID,
          resourceName: "Phân tích Doanh thu Bán hàng 2026",
          addedAt: "2026-01-03T00:00:00Z",
        },
      ],
      createdAt: "2026-01-03T00:00:00Z",
      updatedAt: "2026-01-03T00:00:00Z",
    },
  ],
  categories: [
    {
      id: "00000000-0000-0000-0000-000000000301",
      name: "Độ nhạy cảm",
      description: "Phân loại mức độ nhạy cảm và bảo mật dữ liệu",
      isWarning: false,
      createdBy: ADMIN_USER_ID,
      createdAt: "2026-01-01T00:00:00Z",
    },
    {
      id: "00000000-0000-0000-0000-000000000302",
      name: "Chứng nhận",
      description: "Chứng nhận nguồn dữ liệu và báo cáo chính thức",
      isWarning: false,
      createdBy: ADMIN_USER_ID,
      createdAt: "2026-01-01T00:00:00Z",
    },
    {
      id: "00000000-0000-0000-0000-000000000303",
      name: "Cảnh báo chất lượng",
      description: "Cảnh báo chất lượng dữ liệu hiển thị nổi bật cho người xem",
      isWarning: true,
      createdBy: ADMIN_USER_ID,
      createdAt: "2026-01-01T00:00:00Z",
    },
  ],
  labels: [
    {
      id: "00000000-0000-0000-0000-000000000311",
      categoryId: "00000000-0000-0000-0000-000000000302",
      categoryName: "Chứng nhận",
      value: "Dữ liệu chuẩn hóa",
      description: "Đã đối soát bởi phòng Tài chính",
      isWarning: false,
      assignedCount: 1,
      createdBy: ADMIN_USER_ID,
      createdAt: "2026-01-01T00:00:00Z",
      updatedAt: "2026-01-01T00:00:00Z",
    },
    {
      id: "00000000-0000-0000-0000-000000000312",
      categoryId: "00000000-0000-0000-0000-000000000303",
      categoryName: "Cảnh báo chất lượng",
      value: "Đang rà soát Q4",
      description: "Số liệu vùng Miền Trung đang cập nhật bổ sung",
      isWarning: true,
      assignedCount: 0,
      createdBy: CREATOR_USER_ID,
      createdAt: "2026-01-02T00:00:00Z",
      updatedAt: "2026-01-02T00:00:00Z",
    },
  ],
  labelTargets: {
    "00000000-0000-0000-0000-000000000311": new Set([`WORKBOOK:${DEFAULT_WORKBOOK_ID}`]),
  },
  permissionRules: {},
  customViews: [
    {
      id: "00000000-0000-0000-0000-000000000601",
      dashboardId: DEFAULT_DASHBOARD_ID,
      name: "Tổng quan Miền Bắc",
      filters: { region: "Miền Bắc" },
      parameters: { top: 5 },
      isDefault: false,
      isShared: true,
      ownerId: CREATOR_USER_ID,
      ownerName: "Demo Creator",
      createdAt: "2026-01-05T00:00:00Z",
      updatedAt: "2026-01-05T00:00:00Z",
    },
  ],
  comments: [
    {
      id: "00000000-0000-0000-0000-000000000701",
      resourceType: "DASHBOARD",
      resourceId: DEFAULT_DASHBOARD_ID,
      parentId: null,
      authorId: CREATOR_USER_ID,
      authorName: "Demo Creator",
      authorEmail: "creator@davs.local",
      text: "Doanh thu tháng 11–12 tăng trưởng vượt kế hoạch quý @viewer",
      mentions: ["viewer"],
      snapshotUrl: "/snapshots/current_view.png",
      canEdit: true,
      canDelete: true,
      createdAt: new Date(Date.now() - 15 * 60 * 1000).toISOString(),
      updatedAt: new Date(Date.now() - 15 * 60 * 1000).toISOString(),
      replies: [],
    },
  ],
};

const SUBJECTS: PermissionSubjectDto[] = [
  { subjectType: "GROUP", id: ALL_USERS_GROUP_ID, name: "All Users", detail: "GROUP" },
  { subjectType: "USER", id: ADMIN_USER_ID, name: "Site Administrator", detail: "SITE_ADMIN" },
  { subjectType: "USER", id: CREATOR_USER_ID, name: "Demo Creator", detail: "CREATOR" },
  { subjectType: "USER", id: EXPLORER_USER_ID, name: "Demo Explorer", detail: "EXPLORER" },
  { subjectType: "USER", id: VIEWER_USER_ID, name: "Demo Viewer", detail: "VIEWER" },
];

const TEMPLATES: PermissionTemplateDto[] = [
  {
    id: "VIEWER",
    name: "Người xem (Viewer)",
    description: "Xem, lọc, tương tác, bình luận và tải xuống báo cáo",
    capabilities: ["VIEW", "INTERACT", "FILTER", "DOWNLOAD", "COMMENT"],
  },
  {
    id: "EXPLORER",
    name: "Người khám phá (Explorer)",
    description: "Bao gồm quyền Viewer kèm chỉnh sửa web và chia sẻ",
    capabilities: ["VIEW", "INTERACT", "FILTER", "DOWNLOAD", "COMMENT", "WEB_EDIT", "SHARE"],
  },
  {
    id: "AUTHOR",
    name: "Tác giả (Author / Creator)",
    description: "Toàn quyền biên tập, lưu, xuất bản và kết nối dữ liệu",
    capabilities: [
      "VIEW",
      "INTERACT",
      "FILTER",
      "DOWNLOAD",
      "COMMENT",
      "WEB_EDIT",
      "SAVE",
      "SHARE",
      "PUBLISH",
      "CONNECT",
    ],
  },
  {
    id: "PROJECT_LEADER",
    name: "Trưởng dự án (Project Leader)",
    description: "Toàn quyền quản lý dự án và phân quyền",
    capabilities: [
      "VIEW",
      "INTERACT",
      "FILTER",
      "DOWNLOAD",
      "COMMENT",
      "WEB_EDIT",
      "SAVE",
      "SHARE",
      "DELETE",
      "CHANGE_PERMISSIONS",
      "PUBLISH",
      "CONNECT",
    ],
  },
];

function computeProjectDepth(projectId: string | null): number {
  let depth = 0;
  let cur = projectId;
  const visited = new Set<string>();
  while (cur && !visited.has(cur)) {
    visited.add(cur);
    depth++;
    const found = state.projects.find((p) => p.id === cur && !p.deletedAt);
    if (!found) break;
    cur = found.parentId;
  }
  return Math.max(1, depth);
}

function toProjectDto(p: StoredProject): ProjectDto {
  return {
    id: p.id,
    name: p.name,
    description: p.description,
    parentId: p.parentId,
    ownerId: p.ownerId,
    permissionMode: p.permissionMode,
    isDefault: p.isDefault,
    depth: computeProjectDepth(p.id),
    createdAt: p.createdAt,
    updatedAt: p.updatedAt,
  };
}

function buildSampleWorkbookXlsx(title: string): Blob {
  return buildValidXlsxBlob(
    title,
    ["Tháng", "Khu vực", "Doanh số (triệu VND)"],
    [
      ["T1", "Miền Bắc", 120],
      ["T2", "Miền Nam", 165],
      ["T3", "Miền Trung", 142],
      ["T4", "Miền Bắc", 198],
    ],
  );
}

export const offlineDemoStore = {
  // ---- Licenses ----
  listLicenses(): LicensePoolDto[] {
    return [
      { role: "SITE_ADMIN", total: 5, used: 1, available: 4 },
      { role: "CREATOR", total: 10, used: 2, available: 8 },
      { role: "EXPLORER", total: 30, used: 5, available: 25 },
      { role: "VIEWER", total: 200, used: 18, available: 182 },
    ];
  },

  // ---- Home Dashboards ----
  listDashboardItems(): DashboardItem[] {
    return [
      {
        id: DEFAULT_DASHBOARD_ID,
        name: "Tổng quan kinh doanh",
        project: "Phân tích Bán hàng & Thị trường",
        owner: "Demo Creator",
        updated: "Hôm nay",
        favorite: true,
      },
      ...state.workbooks.map((wb) => ({
        id: DEFAULT_DASHBOARD_ID,
        name: wb.name,
        project: wb.projectName,
        owner: wb.ownerName,
        updated: wb.updatedAt.slice(0, 10),
        favorite: false,
      })),
    ].filter((item, index, arr) => arr.findIndex((x) => x.name === item.name) === index);
  },

  // ---- Projects & Content ----
  listProjects(): ProjectDto[] {
    return state.projects.filter((p) => !p.deletedAt).map(toProjectDto);
  },

  getProjectById(id: string): ProjectDto {
    const found = state.projects.find((p) => p.id === id && !p.deletedAt) ?? state.projects[0];
    return toProjectDto(found);
  },

  getProjectTree(): ProjectTreeNodeDto[] {
    const active = state.projects.filter((p) => !p.deletedAt);
    const buildNode = (proj: StoredProject): ProjectTreeNodeDto => ({
      ...toProjectDto(proj),
      children: active.filter((c) => c.parentId === proj.id).map(buildNode),
    });
    return active.filter((p) => p.parentId === null).map(buildNode);
  },

  getContent(params?: {
    projectId?: string | null;
    q?: string;
    sort?: "updatedAt" | "name" | "owner";
  }): ContentListResponse {
    const pid = params?.projectId || null;
    const keyword = (params?.q || "").trim().toLowerCase();
    const activeProjects = state.projects.filter((p) => !p.deletedAt);
    const current = pid ? activeProjects.find((p) => p.id === pid) ?? null : null;

    const breadcrumbs: ProjectDto[] = [];
    let cursor = current;
    const seen = new Set<string>();
    while (cursor && !seen.has(cursor.id)) {
      seen.add(cursor.id);
      breadcrumbs.unshift(toProjectDto(cursor));
      cursor = cursor.parentId
        ? activeProjects.find((p) => p.id === cursor!.parentId) ?? null
        : null;
    }

    const items: ContentItemDto[] = [];

    for (const proj of activeProjects) {
      const matchFolder = keyword
        ? !pid || proj.parentId === pid || proj.id === pid
        : proj.parentId === pid;
      if (!matchFolder) continue;
      if (
        keyword &&
        !proj.name.toLowerCase().includes(keyword) &&
        !(proj.description || "").toLowerCase().includes(keyword)
      ) {
        continue;
      }
      const subProjects = activeProjects.filter((c) => c.parentId === proj.id).length;
      const wbCount = state.workbooks.filter((w) => w.projectId === proj.id).length;
      items.push({
        id: proj.id,
        resourceType: "PROJECT",
        name: proj.name,
        description: proj.description,
        projectId: proj.parentId,
        ownerId: proj.ownerId,
        ownerName: proj.isDefault ? "System Admin" : "Demo Creator",
        permissionMode: proj.permissionMode,
        isDefault: proj.isDefault,
        currentVersion: 1,
        childCount: subProjects + wbCount,
        allowedActions: proj.isDefault
          ? ["VIEW", "ADD_TO_COLLECTION"]
          : ["VIEW", "RENAME", "MOVE", "CHANGE_OWNER", "DELETE", "ADD_TO_COLLECTION"],
        updatedAt: proj.updatedAt,
      });
    }

    for (const wb of state.workbooks) {
      if (pid && wb.projectId !== pid) continue;
      if (!pid && !keyword && wb.projectId !== DEFAULT_PROJECT_ID) continue;
      if (
        keyword &&
        !wb.name.toLowerCase().includes(keyword) &&
        !(wb.description || "").toLowerCase().includes(keyword)
      ) {
        continue;
      }
      items.push({
        id: wb.id,
        resourceType: "WORKBOOK",
        name: wb.name,
        description: wb.description,
        projectId: wb.projectId,
        ownerId: wb.ownerId,
        ownerName: wb.ownerName,
        permissionMode: "INHERIT",
        isDefault: false,
        currentVersion: wb.currentVersion,
        childCount: 0,
        allowedActions: ["VIEW", "DOWNLOAD", "VERSION_HISTORY", "ADD_TO_COLLECTION"],
        updatedAt: wb.updatedAt,
      });
    }

    if (params?.sort === "name") {
      items.sort((a, b) => a.name.localeCompare(b.name, "vi"));
    } else if (params?.sort === "owner") {
      items.sort((a, b) => a.ownerName.localeCompare(b.ownerName, "vi"));
    } else {
      items.sort((a, b) => b.updatedAt.localeCompare(a.updatedAt));
    }

    return {
      projectId: pid,
      projectName: current ? current.name : "Tất cả dự án",
      breadcrumbs,
      items,
      totalCount: items.length,
    };
  },

  createProject(payload: CreateProjectPayload): ProjectDto {
    const now = new Date().toISOString();
    const created: StoredProject = {
      id: nextUuid(),
      name: payload.name.trim(),
      description: payload.description?.trim() || null,
      parentId: payload.parentId || null,
      ownerId: CREATOR_USER_ID,
      permissionMode: payload.permissionMode || "CUSTOM",
      isDefault: false,
      depth: 1,
      createdAt: now,
      updatedAt: now,
      deletedAt: null,
    };
    state.projects.push(created);
    return toProjectDto(created);
  },

  renameProject(id: string, payload: UpdateProjectPayload): ProjectDto {
    const found = state.projects.find((p) => p.id === id && !p.deletedAt);
    if (!found) {
      return this.getProjectById(id);
    }
    found.name = payload.name.trim();
    found.description = payload.description?.trim() || null;
    found.updatedAt = new Date().toISOString();
    return toProjectDto(found);
  },

  moveProject(id: string, targetId: string | null): ProjectDto {
    const found = state.projects.find((p) => p.id === id && !p.deletedAt);
    if (!found) {
      return this.getProjectById(id);
    }
    if (targetId !== id) {
      found.parentId = targetId || null;
      found.updatedAt = new Date().toISOString();
    }
    return toProjectDto(found);
  },

  changeProjectOwner(id: string, userId: string): ProjectDto {
    const found = state.projects.find((p) => p.id === id && !p.deletedAt);
    if (!found) {
      return this.getProjectById(id);
    }
    found.ownerId = userId;
    found.updatedAt = new Date().toISOString();
    return toProjectDto(found);
  },

  deleteProject(id: string): void {
    const found = state.projects.find((p) => p.id === id && !p.isDefault);
    if (found) {
      found.deletedAt = new Date().toISOString();
    }
  },

  restoreProject(id: string): ProjectDto {
    const found = state.projects.find((p) => p.id === id);
    if (found) {
      found.deletedAt = null;
      found.updatedAt = new Date().toISOString();
      return toProjectDto(found);
    }
    return this.getProjectById(DEFAULT_PROJECT_ID);
  },

  listCollections(): CollectionDto[] {
    return [...state.collections];
  },

  createCollection(name: string, description?: string): CollectionDto {
    const now = new Date().toISOString();
    const created: CollectionDto = {
      id: nextUuid(),
      name: name.trim(),
      description: description?.trim() || null,
      ownerId: CREATOR_USER_ID,
      itemCount: 0,
      items: [],
      createdAt: now,
      updatedAt: now,
    };
    state.collections.unshift(created);
    return created;
  },

  addToCollection(
    collectionId: string,
    resourceType: "PROJECT" | "WORKBOOK",
    resourceId: string,
  ): CollectionDto {
    const col = state.collections.find((c) => c.id === collectionId) ?? state.collections[0];
    if (!col) {
      return this.createCollection("Bộ sưu tập cá nhân");
    }
    const exists = col.items.some(
      (i) => i.resourceType === resourceType && i.resourceId === resourceId,
    );
    if (!exists) {
      const resourceName =
        resourceType === "PROJECT"
          ? state.projects.find((p) => p.id === resourceId)?.name || resourceId
          : state.workbooks.find((w) => w.id === resourceId)?.name || resourceId;
      col.items.unshift({
        collectionId: col.id,
        resourceType,
        resourceId,
        resourceName,
        addedAt: new Date().toISOString(),
      });
      col.itemCount = col.items.length;
      col.updatedAt = new Date().toISOString();
    }
    return { ...col, items: [...col.items] };
  },

  // ---- Workbooks & Versions ----
  uploadWorkbook(input: UploadWorkbookInput): WorkbookDto {
    input.onProgress?.(60);
    const now = new Date().toISOString();
    const project =
      state.projects.find((p) => p.id === input.projectId && !p.deletedAt) ?? state.projects[0];
    const trimmedName = input.name.trim();
    const existing = state.workbooks.find(
      (w) => w.projectId === project.id && w.name.toLowerCase() === trimmedName.toLowerCase(),
    );

    if (existing) {
      const nextVer = existing.currentVersion + 1;
      existing.currentVersion = nextVer;
      existing.description = input.description?.trim() ?? existing.description;
      existing.fileName = input.file.name;
      existing.sizeBytes = input.file.size;
      existing.updatedAt = now;

      const verList = state.versions[existing.id] ?? [];
      for (const v of verList) v.current = false;
      const newVer: WorkbookVersionDto = {
        id: nextUuid(),
        workbookId: existing.id,
        versionNumber: nextVer,
        fileName: input.file.name,
        sizeBytes: input.file.size,
        note: input.note?.trim() || "Ghi đè phiên bản mới",
        createdBy: CREATOR_USER_ID,
        createdByName: "Demo Creator",
        createdAt: now,
        current: true,
        purged: false,
      };
      verList.unshift(newVer);
      state.versions[existing.id] = verList;
      state.uploadedBlobs[`${existing.id}:${nextVer}`] = input.file;
      input.onProgress?.(100);
      return { ...existing };
    }

    const wbId = nextUuid();
    const created: WorkbookDto = {
      id: wbId,
      projectId: project.id,
      projectName: project.name,
      name: trimmedName,
      description: input.description?.trim() || null,
      currentVersion: 1,
      ownerId: CREATOR_USER_ID,
      ownerName: "Demo Creator",
      importStatus: "IMPORTED",
      needsReconnect: false,
      sizeBytes: input.file.size,
      fileName: input.file.name,
      labels: [],
      createdAt: now,
      updatedAt: now,
    };
    state.workbooks.unshift(created);
    state.versions[wbId] = [
      {
        id: nextUuid(),
        workbookId: wbId,
        versionNumber: 1,
        fileName: input.file.name,
        sizeBytes: input.file.size,
        note: input.note?.trim() || "Phiên bản tải lên ban đầu",
        createdBy: CREATOR_USER_ID,
        createdByName: "Demo Creator",
        createdAt: now,
        current: true,
        purged: false,
      },
    ];
    state.uploadedBlobs[`${wbId}:1`] = input.file;
    input.onProgress?.(100);
    return created;
  },

  getWorkbookById(id: string): WorkbookDto {
    return state.workbooks.find((w) => w.id === id) ?? state.workbooks[0];
  },

  listWorkbooksByProject(projectId: string): WorkbookDto[] {
    return state.workbooks.filter((w) => w.projectId === projectId);
  },

  listVersions(workbookId: string): WorkbookVersionDto[] {
    return [...(state.versions[workbookId] ?? state.versions[DEFAULT_WORKBOOK_ID] ?? [])];
  },

  restoreVersion(workbookId: string, versionNumber: number, note: string): WorkbookVersionDto {
    const wb = this.getWorkbookById(workbookId);
    const list = state.versions[wb.id] ?? [];
    const source = list.find((v) => v.versionNumber === versionNumber) ?? list[0];
    const nextNum = wb.currentVersion + 1;
    wb.currentVersion = nextNum;
    wb.updatedAt = new Date().toISOString();
    for (const v of list) v.current = false;
    const restored: WorkbookVersionDto = {
      id: nextUuid(),
      workbookId: wb.id,
      versionNumber: nextNum,
      fileName: source?.fileName || `${wb.name}_v${nextNum}.xlsx`,
      sizeBytes: source?.sizeBytes || wb.sizeBytes,
      note: note.trim(),
      createdBy: CREATOR_USER_ID,
      createdByName: "Demo Creator",
      createdAt: wb.updatedAt,
      current: true,
      purged: false,
    };
    list.unshift(restored);
    state.versions[wb.id] = list;
    const blob = state.uploadedBlobs[`${wb.id}:${versionNumber}`];
    if (blob) {
      state.uploadedBlobs[`${wb.id}:${nextNum}`] = blob;
    }
    return restored;
  },

  downloadVersion(workbookId: string, versionNumber: number): Blob {
    const stored = state.uploadedBlobs[`${workbookId}:${versionNumber}`];
    if (stored) {
      return stored;
    }
    const wb = this.getWorkbookById(workbookId);
    return buildSampleWorkbookXlsx(wb.name);
  },

  deleteVersion(workbookId: string, versionNumber: number): void {
    const list = state.versions[workbookId];
    if (!list) return;
    state.versions[workbookId] = list.filter((v) => v.versionNumber !== versionNumber || v.current);
  },

  // ---- Labels ----
  listCategories(): LabelCategoryDto[] {
    return [...state.categories];
  },

  createCategory(payload: {
    name: string;
    description?: string;
    isWarning?: boolean;
  }): LabelCategoryDto {
    const created: LabelCategoryDto = {
      id: nextUuid(),
      name: payload.name.trim(),
      description: payload.description?.trim() || null,
      isWarning: Boolean(payload.isWarning),
      createdBy: CREATOR_USER_ID,
      createdAt: new Date().toISOString(),
    };
    state.categories.push(created);
    return created;
  },

  listLabels(params?: { categoryId?: string; q?: string }): LabelDto[] {
    const keyword = (params?.q || "").trim().toLowerCase();
    return state.labels.filter((l) => {
      if (params?.categoryId && l.categoryId !== params.categoryId) return false;
      if (
        keyword &&
        !l.value.toLowerCase().includes(keyword) &&
        !(l.description || "").toLowerCase().includes(keyword)
      ) {
        return false;
      }
      return true;
    });
  },

  createLabel(payload: {
    categoryId: string;
    value: string;
    description?: string;
    isWarning?: boolean;
  }): LabelDto {
    const cat = state.categories.find((c) => c.id === payload.categoryId) ?? state.categories[0];
    const now = new Date().toISOString();
    const created: LabelDto = {
      id: nextUuid(),
      categoryId: cat.id,
      categoryName: cat.name,
      value: payload.value.trim(),
      description: payload.description?.trim() || null,
      isWarning: payload.isWarning ?? cat.isWarning,
      assignedCount: 0,
      createdBy: CREATOR_USER_ID,
      createdAt: now,
      updatedAt: now,
    };
    state.labels.unshift(created);
    state.labelTargets[created.id] = new Set();
    return created;
  },

  updateLabel(
    id: string,
    payload: { value?: string; description?: string; isWarning?: boolean },
  ): LabelDto {
    const found = state.labels.find((l) => l.id === id) ?? state.labels[0];
    if (payload.value !== undefined) found.value = payload.value.trim();
    if (payload.description !== undefined) found.description = payload.description.trim() || null;
    if (payload.isWarning !== undefined) found.isWarning = payload.isWarning;
    found.updatedAt = new Date().toISOString();
    return { ...found };
  },

  deleteLabel(id: string): { deletedId: string; unassignedCount: number } {
    const count = state.labelTargets[id]?.size ?? 0;
    delete state.labelTargets[id];
    state.labels = state.labels.filter((l) => l.id !== id);
    return { deletedId: id, unassignedCount: count };
  },

  assignLabel(labelId: string, targets: LabelTargetRef[]): LabelDto {
    const found = state.labels.find((l) => l.id === labelId) ?? state.labels[0];
    const set = state.labelTargets[found.id] ?? new Set<string>();
    for (const t of targets) {
      set.add(`${t.type}:${t.id}`);
    }
    state.labelTargets[found.id] = set;
    found.assignedCount = set.size;
    return { ...found };
  },

  unassignLabel(labelId: string, targets: LabelTargetRef[]): LabelDto {
    const found = state.labels.find((l) => l.id === labelId) ?? state.labels[0];
    const set = state.labelTargets[found.id] ?? new Set<string>();
    for (const t of targets) {
      set.delete(`${t.type}:${t.id}`);
    }
    state.labelTargets[found.id] = set;
    found.assignedCount = set.size;
    return { ...found };
  },

  listLabelsForTarget(type: string, id: string): LabelDto[] {
    const key = `${type.toUpperCase()}:${id}`;
    return state.labels.filter((l) => state.labelTargets[l.id]?.has(key));
  },

  // ---- Permissions ----
  listPermissionTemplates(): PermissionTemplateDto[] {
    return TEMPLATES;
  },

  listPermissionSubjects(): PermissionSubjectDto[] {
    return SUBJECTS;
  },

  getResourcePermissions(
    resourceType: ResourceType,
    resourceId: string,
  ): ResourcePermissionsResponse {
    const key = `${resourceType}:${resourceId}`;
    const rules = state.permissionRules[key] ?? [
      {
        id: "00000000-0000-0000-0000-000000000951",
        subjectType: "GROUP",
        subjectId: ALL_USERS_GROUP_ID,
        subjectName: "All Users",
        subjectDetail: "GROUP",
        capability: "VIEW",
        effect: "ALLOW",
      },
    ];
    const proj =
      resourceType === "project" ? state.projects.find((p) => p.id === resourceId) : undefined;
    return {
      resourceType: resourceType.toUpperCase(),
      resourceId,
      resourceName: proj?.name || "Tài nguyên DAVS",
      permissionMode: proj?.permissionMode || "CUSTOM",
      lockedByParent: false,
      canEdit: true,
      rules: [...rules],
    };
  },

  updateResourcePermissions(
    resourceType: ResourceType,
    resourceId: string,
    inputs: RuleInput[],
  ): UpdateResourcePermissionsResponse {
    const key = `${resourceType}:${resourceId}`;
    state.permissionRules[key] = inputs.map((r) => {
      const subj = SUBJECTS.find((s) => s.id === r.subjectId);
      return {
        id: nextUuid(),
        subjectType: r.subjectType,
        subjectId: r.subjectId,
        subjectName: subj?.name || r.subjectId,
        subjectDetail: subj?.detail || r.subjectType,
        capability: r.capability,
        effect: r.effect,
      };
    });
    return {
      permissions: this.getResourcePermissions(resourceType, resourceId),
      warnings: [],
    };
  },

  getEffectivePermissions(
    resourceType: ResourceType,
    resourceId: string,
    userId: string,
  ): EffectivePermissionsResponse {
    const subj = SUBJECTS.find((s) => s.id === userId);
    const caps: Capability[] = ["VIEW", "INTERACT", "FILTER", "DOWNLOAD", "COMMENT"];
    return {
      userId,
      displayName: subj?.name || "Người dùng",
      siteRole: subj?.detail || "VIEWER",
      resourceType: resourceType.toUpperCase(),
      resourceId,
      lockedByParent: false,
      effectiveCapabilities: caps,
    };
  },

  // ---- Custom Views & Save As ----
  listCustomViews(dashboardId: string): CustomViewDto[] {
    return state.customViews.filter((v) => v.dashboardId === dashboardId);
  },

  createCustomView(dashboardId: string, req: CreateCustomViewRequest): CustomViewDto {
    const now = new Date().toISOString();
    if (req.isDefault) {
      for (const v of state.customViews) {
        if (v.dashboardId === dashboardId) v.isDefault = false;
      }
    }
    const created: CustomViewDto = {
      id: nextUuid(),
      dashboardId,
      name: req.name.trim(),
      filters: req.filters ?? {},
      parameters: req.parameters ?? {},
      isDefault: Boolean(req.isDefault),
      isShared: Boolean(req.isShared),
      ownerId: CREATOR_USER_ID,
      ownerName: "Demo User",
      createdAt: now,
      updatedAt: now,
    };
    state.customViews.unshift(created);
    return created;
  },

  setDefaultCustomView(id: string): void {
    const target = state.customViews.find((v) => v.id === id);
    if (!target) return;
    for (const v of state.customViews) {
      if (v.dashboardId === target.dashboardId) {
        v.isDefault = v.id === id;
      }
    }
  },

  setSharedCustomView(id: string, shared: boolean): void {
    const target = state.customViews.find((v) => v.id === id);
    if (target) {
      target.isShared = shared;
      target.updatedAt = new Date().toISOString();
    }
  },

  deleteCustomView(id: string): void {
    state.customViews = state.customViews.filter((v) => v.id !== id);
  },

  saveAsWorkbook(req: SaveAsWorkbookRequest): WorkbookDto {
    const project =
      state.projects.find((p) => p.id === req.targetProjectId && !p.deletedAt) ?? state.projects[0];
    const now = new Date().toISOString();
    const created: WorkbookDto = {
      id: nextUuid(),
      projectId: project.id,
      projectName: project.name,
      name: req.newName.trim(),
      description: req.description?.trim() || null,
      currentVersion: 1,
      ownerId: CREATOR_USER_ID,
      ownerName: "Demo Creator",
      importStatus: "IMPORTED",
      needsReconnect: false,
      sizeBytes: 18_432,
      fileName: `${req.newName.trim()}.xlsx`,
      labels: [],
      createdAt: now,
      updatedAt: now,
    };
    state.workbooks.unshift(created);
    return created;
  },

  // ---- Comments ----
  listComments(resourceType: string, resourceId: string): CommentDto[] {
    const normalized = resourceType.toUpperCase();
    return state.comments
      .filter((c) => c.resourceType === normalized && c.resourceId === resourceId)
      .map((c) => ({ ...c, replies: [...c.replies] }));
  },

  createComment(resourceType: string, resourceId: string, req: CreateCommentRequest): CommentDto {
    const now = new Date().toISOString();
    const created: CommentDto = {
      id: nextUuid(),
      resourceType: resourceType.toUpperCase(),
      resourceId,
      parentId: req.parentId ?? null,
      authorId: CREATOR_USER_ID,
      authorName: "Bạn (Demo)",
      authorEmail: "demo@davs.local",
      text: req.text.trim(),
      mentions: req.mentions ?? [],
      snapshotUrl: req.snapshotUrl ?? null,
      canEdit: true,
      canDelete: true,
      createdAt: now,
      updatedAt: now,
      replies: [],
    };
    if (req.parentId) {
      const parent = state.comments.find((c) => c.id === req.parentId);
      if (parent) {
        parent.replies.push(created);
        return created;
      }
    }
    state.comments.push(created);
    return created;
  },

  replyComment(parentId: string, req: CreateReplyRequest): CommentDto {
    const parent = state.comments.find((c) => c.id === parentId) ?? state.comments[0];
    return this.createComment(parent?.resourceType || "DASHBOARD", parent?.resourceId || DEFAULT_DASHBOARD_ID, {
      text: req.text,
      mentions: req.mentions,
      parentId,
    });
  },

  updateComment(commentId: string, text: string): void {
    for (const c of state.comments) {
      if (c.id === commentId) {
        c.text = text.trim();
        c.updatedAt = new Date().toISOString();
        return;
      }
      const reply = c.replies.find((r) => r.id === commentId);
      if (reply) {
        reply.text = text.trim();
        reply.updatedAt = new Date().toISOString();
        return;
      }
    }
  },

  deleteComment(commentId: string): void {
    state.comments = state.comments.filter((c) => c.id !== commentId);
    for (const c of state.comments) {
      c.replies = c.replies.filter((r) => r.id !== commentId);
    }
  },

  // ---- Exports ----
  getExportLimits(): ExportLimitsDto {
    return {
      maxRows: 500_000,
      maxBytes: 100 * 1024 * 1024,
      timeoutSeconds: 30,
    };
  },

  buildExportResponse(req: ExportRequest): {
    data: Blob;
    headers: Record<string, string>;
  } {
    const headers = ["Tháng", "Doanh số (triệu VND)", "Khu vực"];
    const rows: Array<Array<string | number>> = [
      ["T1", 82, "Miền Bắc"],
      ["T2", 96, "Miền Nam"],
      ["T3", 105, "Miền Trung"],
      ["T4", 118, "Miền Bắc"],
      ["T5", 126, "Miền Nam"],
      ["T6", 138, "Miền Trung"],
      ["T7", 148, "Miền Bắc"],
      ["T8", 156, "Miền Nam"],
      ["T9", 172, "Miền Bắc"],
      ["T10", 188, "Miền Trung"],
      ["T11", 205, "Miền Nam"],
      ["T12", 240, "Miền Bắc"],
    ];

    const ts = Date.now();
    if (req.format === "EXCEL") {
      const blob = buildValidXlsxBlob("Tong quan kinh doanh", headers, rows);
      return {
        data: blob,
        headers: { "content-disposition": `attachment; filename="davs_report_${ts}.xlsx"` },
      };
    }
    if (req.format === "PDF") {
      const blob = buildValidPdfBlob("Tong quan kinh doanh", headers, rows);
      return {
        data: blob,
        headers: { "content-disposition": `attachment; filename="davs_report_${ts}.pdf"` },
      };
    }
    if (req.format === "PNG") {
      const blob = new Blob([VALID_1X1_PNG.buffer as ArrayBuffer], { type: "image/png" });
      return {
        data: blob,
        headers: { "content-disposition": `attachment; filename="davs_report_${ts}.png"` },
      };
    }
    const csvLines = [
      "\uFEFFBáo cáo: Tổng quan kinh doanh",
      headers.join(","),
      ...rows.map((r) => r.join(",")),
    ];
    const csvBlob = new Blob([csvLines.join("\n")], { type: "text/csv;charset=utf-8" });
    return {
      data: csvBlob,
      headers: { "content-disposition": `attachment; filename="davs_report_${ts}.csv"` },
    };
  },
};
