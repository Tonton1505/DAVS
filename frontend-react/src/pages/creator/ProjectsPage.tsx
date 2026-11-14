import { useCallback, useEffect, useState } from "react";
import {
  Archive,
  ChevronRight,
  Download,
  FileSpreadsheet,
  Folder,
  Grid2X2,
  History,
  List,
  MoreVertical,
  Move,
  Pencil,
  Plus,
  Search,
  Shield,
  Trash2,
  Upload,
  UserCheck,
} from "lucide-react";
import { useNavigate, useParams } from "react-router";
import {
  Badge,
  Button,
  Card,
  Dialog,
  Input,
  Label,
  PageHeader,
  Select,
  Textarea,
} from "../../components/ui";
import { useAuthStore } from "../../stores/auth.store";
import { useUIStore } from "../../stores/ui.store";
import {
  projectService,
  type ContentItemDto,
  type ContentListResponse,
  type ProjectDto,
  type ProjectTreeNodeDto,
  type CollectionDto,
} from "../../services/project.service";
import { workbookService } from "../../services/workbook.service";
import { PermissionsDialog } from "../../components/permissions/PermissionsDialog";
import { extractApiError } from "../../lib/api/error";

export default function ProjectsPage() {
  const { id: routeProjectId } = useParams<{ id?: string }>();
  const nav = useNavigate();
  const role = useAuthStore((s) => s.role);
  const toast = useUIStore((s) => s.toast);

  const [loading, setLoading] = useState(true);
  const [content, setContent] = useState<ContentListResponse>({
    projectId: null,
    projectName: "Tất cả dự án",
    breadcrumbs: [],
    items: [],
    totalCount: 0,
  });
  const [q, setQ] = useState("");
  const [sort, setSort] = useState<"updatedAt" | "name" | "owner">("updatedAt");
  const [grid, setGrid] = useState(true);

  // Dialog states
  const [permProjectId, setPermProjectId] = useState<string | null>(null);
  const [renameTarget, setRenameTarget] = useState<ContentItemDto | null>(null);
  const [renameName, setRenameName] = useState("");
  const [renameDesc, setRenameDesc] = useState("");
  const [renameSubmitting, setRenameSubmitting] = useState(false);

  const [moveTarget, setMoveTarget] = useState<ContentItemDto | null>(null);
  const [treeNodes, setTreeNodes] = useState<ProjectTreeNodeDto[]>([]);
  const [selectedTargetId, setSelectedTargetId] = useState<string>("");
  const [moveSubmitting, setMoveSubmitting] = useState(false);

  const [ownerTarget, setOwnerTarget] = useState<ContentItemDto | null>(null);
  const [newOwnerId, setNewOwnerId] = useState("");
  const [ownerSubmitting, setOwnerSubmitting] = useState(false);

  const [deleteTarget, setDeleteTarget] = useState<ContentItemDto | null>(null);
  const [confirmDeleteName, setConfirmDeleteName] = useState("");
  const [deleteSubmitting, setDeleteSubmitting] = useState(false);

  const [collectionTarget, setCollectionTarget] = useState<ContentItemDto | null>(null);
  const [collections, setCollections] = useState<CollectionDto[]>([]);
  const [selectedCollectionId, setSelectedCollectionId] = useState("");
  const [newColName, setNewColName] = useState("");
  const [creatingCol, setCreatingCol] = useState(false);

  const fetchContent = useCallback(() => {
    setLoading(true);
    projectService
      .getContent({
        projectId: routeProjectId || null,
        q,
        sort,
      })
      .then((data) => {
        setContent(data);
      })
      .catch((err) => {
        const parsed = extractApiError(err);
        toast(parsed?.message || "Không thể tải danh sách nội dung", "error");
      })
      .finally(() => {
        setLoading(false);
      });
  }, [routeProjectId, q, sort, toast]);

  useEffect(() => {
    fetchContent();
  }, [fetchContent]);

  // Handle Rename
  const handleRenameSubmit = async () => {
    if (!renameTarget) return;
    const trimmed = renameName.trim();
    if (!trimmed) {
      toast("Tên dự án không được để trống", "error");
      return;
    }
    setRenameSubmitting(true);
    try {
      await projectService.rename(renameTarget.id, {
        name: trimmed,
        description: renameDesc.trim() || undefined,
      });
      toast("Đổi tên dự án thành công");
      setRenameTarget(null);
      fetchContent();
    } catch (err) {
      const parsed = extractApiError(err);
      toast(parsed?.message || "Đổi tên dự án thất bại", "error");
    } finally {
      setRenameSubmitting(false);
    }
  };

  // Open Move Dialog
  const handleOpenMove = async (item: ContentItemDto) => {
    setMoveTarget(item);
    setSelectedTargetId("");
    try {
      const tree = await projectService.getTree();
      setTreeNodes(tree);
    } catch {
      setTreeNodes([]);
    }
  };

  const handleMoveSubmit = async () => {
    if (!moveTarget) return;
    setMoveSubmitting(true);
    try {
      await projectService.move(
        moveTarget.id,
        selectedTargetId ? selectedTargetId : null,
      );
      toast("Di chuyển dự án thành công");
      setMoveTarget(null);
      fetchContent();
    } catch (err) {
      const parsed = extractApiError(err);
      toast(parsed?.message || "Di chuyển dự án thất bại", "error");
    } finally {
      setMoveSubmitting(false);
    }
  };

  // Open Owner Dialog
  const handleOwnerSubmit = async () => {
    if (!ownerTarget) return;
    const trimmedId = newOwnerId.trim();
    if (!trimmedId) {
      toast("Vui lòng nhập ID người nhận quyền sở hữu", "error");
      return;
    }
    setOwnerSubmitting(true);
    try {
      await projectService.changeOwner(ownerTarget.id, trimmedId);
      toast("Chuyển quyền sở hữu thành công");
      setOwnerTarget(null);
      fetchContent();
    } catch (err) {
      const parsed = extractApiError(err);
      toast(parsed?.message || "Chuyển quyền sở hữu thất bại", "error");
    } finally {
      setOwnerSubmitting(false);
    }
  };

  // Delete Project Submit
  const handleDeleteSubmit = async () => {
    if (!deleteTarget) return;
    if (confirmDeleteName.trim() !== deleteTarget.name) {
      toast("Tên xác nhận phải trùng khớp với tên dự án", "error");
      return;
    }
    setDeleteSubmitting(true);
    try {
      await projectService.deleteProject(deleteTarget.id, confirmDeleteName.trim());
      toast("Đã chuyển dự án vào thùng rác");
      setDeleteTarget(null);
      fetchContent();
    } catch (err) {
      const parsed = extractApiError(err);
      toast(parsed?.message || "Xóa dự án thất bại", "error");
    } finally {
      setDeleteSubmitting(false);
    }
  };

  // Collections dialog
  const handleOpenCollection = async (item: ContentItemDto) => {
    setCollectionTarget(item);
    try {
      const cols = await projectService.listCollections();
      setCollections(cols);
      if (cols.length > 0) {
        setSelectedCollectionId(cols[0].id);
      }
    } catch {
      setCollections([]);
    }
  };

  const handleAddToCollectionSubmit = async () => {
    if (!collectionTarget || !selectedCollectionId) return;
    try {
      await projectService.addToCollection(selectedCollectionId, {
        resourceType: collectionTarget.resourceType,
        resourceId: collectionTarget.id,
      });
      toast("Đã thêm vào bộ sưu tập");
      setCollectionTarget(null);
    } catch (err) {
      const parsed = extractApiError(err);
      toast(parsed?.message || "Không thể thêm vào bộ sưu tập", "error");
    }
  };

  const handleCreateCollection = async () => {
    const trimmed = newColName.trim();
    if (!trimmed) return;
    try {
      const created = await projectService.createCollection({ name: trimmed });
      setCollections((prev) => [created, ...prev]);
      setSelectedCollectionId(created.id);
      setNewColName("");
      setCreatingCol(false);
      toast("Đã tạo bộ sưu tập mới");
    } catch (err) {
      const parsed = extractApiError(err);
      toast(parsed?.message || "Không thể tạo bộ sưu tập", "error");
    }
  };

  // Workbook download
  const handleDownloadWorkbook = async (wb: ContentItemDto) => {
    try {
      const blob = await workbookService.downloadVersion(wb.id, wb.currentVersion);
      const url = window.URL.createObjectURL(blob);
      const a = document.createElement("a");
      a.href = url;
      a.download = `${wb.name}.twb`;
      document.body.appendChild(a);
      a.click();
      a.remove();
      window.URL.revokeObjectURL(url);
      toast(`Đang tải về: ${wb.name}`);
    } catch (err) {
      const parsed = extractApiError(err);
      toast(parsed?.message || "Không thể tải về workbook", "error");
    }
  };

  const flattenTree = (
    nodes: ProjectTreeNodeDto[],
    depth = 0,
  ): Array<{ id: string; name: string; depth: number }> => {
    let result: Array<{ id: string; name: string; depth: number }> = [];
    for (const node of nodes) {
      result.push({ id: node.id, name: node.name, depth });
      if (node.children && node.children.length > 0) {
        result = result.concat(flattenTree(node.children, depth + 1));
      }
    }
    return result;
  };

  const isCreatorOrAdmin = role === "creator" || role === "admin";

  return (
    <>
      <PageHeader
        title="Dự án & Nội dung"
        actions={
          isCreatorOrAdmin ? (
            <div className="flex items-center gap-2">
              <Button
                variant="primary"
                onClick={() =>
                  nav(
                    routeProjectId
                      ? `/projects/${routeProjectId}/upload`
                      : "/workbooks/upload",
                  )
                }
              >
                <Upload className="size-4" />
                Tải lên workbook
              </Button>
              <Button onClick={() => nav("/projects/new")}>
                <Plus className="size-4" />
                Tạo dự án
              </Button>
            </div>
          ) : undefined
        }
      />

      {/* Breadcrumbs Navigation */}
      <div className="mb-4 flex flex-wrap items-center gap-2 text-sm text-muted-foreground">
        <button
          className="hover:text-ink hover:underline font-medium"
          onClick={() => nav("/projects")}
        >
          Tất cả dự án
        </button>
        {content.breadcrumbs?.map((bc) => (
          <span key={bc.id} className="flex items-center gap-2">
            <ChevronRight className="size-3.5 text-muted-foreground" />
            <button
              className={`hover:text-ink hover:underline ${
                bc.id === routeProjectId ? "font-semibold text-ink" : ""
              }`}
              onClick={() => nav(`/projects/${bc.id}`)}
            >
              {bc.name}
            </button>
          </span>
        ))}
      </div>

      {/* Search, Filter & View Mode Controls */}
      <div className="mb-4 flex flex-wrap gap-2">
        <div className="relative min-w-64 flex-1">
          <Search className="absolute left-3 top-3 size-4 text-muted-foreground" />
          <Input
            className="pl-9"
            maxLength={100}
            value={q}
            onChange={(e) => setQ(e.target.value)}
            placeholder="Tìm kiếm theo tên hoặc mô tả (tối đa 100 ký tự)…"
          />
        </div>
        <Select
          value={sort}
          onChange={(e) =>
            setSort(e.target.value as "updatedAt" | "name" | "owner")
          }
        >
          <option value="updatedAt">Sắp xếp: Ngày sửa</option>
          <option value="name">Tên A-Z</option>
          <option value="owner">Chủ sở hữu</option>
        </Select>
        <Button onClick={() => setGrid(!grid)}>
          {grid ? <List className="size-4" /> : <Grid2X2 className="size-4" />}
          {grid ? "Danh sách" : "Lưới"}
        </Button>
      </div>

      {loading ? (
        <Card className="text-center text-sm text-muted-foreground">
          Đang tải nội dung…
        </Card>
      ) : content.items.length === 0 ? (
        <Card className="text-center text-muted-foreground py-12">
          <Folder className="mx-auto size-10 text-border mb-2" />
          <div>Không có nội dung nào trong thư mục này.</div>
          {isCreatorOrAdmin && (
            <div className="mt-4 flex justify-center gap-2">
              <Button
                variant="primary"
                onClick={() =>
                  nav(
                    routeProjectId
                      ? `/projects/${routeProjectId}/upload`
                      : "/workbooks/upload",
                  )
                }
              >
                <Upload className="size-4" /> Tải lên workbook
              </Button>
              <Button onClick={() => nav("/projects/new")}>
                <Plus className="size-4" /> Tạo dự án mới
              </Button>
            </div>
          )}
        </Card>
      ) : (
        <div
          className={
            grid ? "grid gap-4 md:grid-cols-2 xl:grid-cols-3" : "space-y-2"
          }
        >
          {content.items.map((item) => {
            const isFolder = item.resourceType === "PROJECT";
            return (
              <div
                key={`${item.resourceType}-${item.id}`}
                className={`group text-left transition-colors border bg-white ${
                  grid
                    ? "rounded-xl p-5 hover:border-primary/50"
                    : "flex w-full items-center justify-between rounded-lg px-4 py-3 hover:bg-muted/40"
                }`}
              >
                <div className="flex w-full items-start gap-3 min-w-0">
                  <span
                    className={`rounded-lg p-3 shrink-0 cursor-pointer ${
                      isFolder
                        ? "bg-amber-50 text-amber-700"
                        : "bg-blue-50 text-blue-700"
                    }`}
                    onClick={() => {
                      if (isFolder) {
                        nav(`/projects/${item.id}`);
                      } else {
                        nav(`/workbooks/${item.id}/versions`);
                      }
                    }}
                  >
                    {isFolder ? (
                      <Folder className="size-5" />
                    ) : (
                      <FileSpreadsheet className="size-5" />
                    )}
                  </span>
                  <div className="min-w-0 flex-1">
                    <div
                      className="font-semibold text-ink truncate cursor-pointer hover:underline"
                      onClick={() => {
                        if (isFolder) {
                          nav(`/projects/${item.id}`);
                        } else {
                          nav(`/workbooks/${item.id}/versions`);
                        }
                      }}
                    >
                      {item.name}
                    </div>
                    {item.description && (
                      <div className="mt-0.5 truncate text-xs text-muted-foreground">
                        {item.description}
                      </div>
                    )}
                    <div className="mt-2 flex flex-wrap items-center gap-1.5 text-xs text-muted-foreground">
                      {isFolder ? (
                        <>
                          <Badge variant="muted">
                            {item.childCount} mục con
                          </Badge>
                          <Badge variant="muted">{item.permissionMode}</Badge>
                          {item.isDefault && (
                            <Badge variant="success">Mặc định</Badge>
                          )}
                        </>
                      ) : (
                        <>
                          <Badge variant="info">v{item.currentVersion}</Badge>
                          <span>Tác giả: {item.ownerName}</span>
                        </>
                      )}
                    </div>
                  </div>

                  {/* Actions Dropdown / Quick Buttons */}
                  <div className="flex items-center gap-1 shrink-0">
                    {item.allowedActions.includes("CHANGE_PERMISSIONS") && isFolder && (
                      <Button
                        type="button"
                        aria-label={`Phân quyền cho ${item.name}`}
                        onClick={() => setPermProjectId(item.id)}
                      >
                        <Shield className="size-3.5" />
                        <span className="hidden sm:inline">Quyền</span>
                      </Button>
                    )}

                    {/* Additional action triggers */}
                    <div className="flex items-center gap-1">
                      {item.allowedActions.includes("DOWNLOAD") && !isFolder && (
                        <button
                          type="button"
                          className="p-1.5 rounded hover:bg-muted text-muted-foreground hover:text-ink"
                          title="Tải về"
                          onClick={() => handleDownloadWorkbook(item)}
                        >
                          <Download className="size-4" />
                        </button>
                      )}
                      {item.allowedActions.includes("VERSION_HISTORY") && !isFolder && (
                        <button
                          type="button"
                          className="p-1.5 rounded hover:bg-muted text-muted-foreground hover:text-ink"
                          title="Lịch sử phiên bản"
                          onClick={() => nav(`/workbooks/${item.id}/versions`)}
                        >
                          <History className="size-4" />
                        </button>
                      )}
                      {item.allowedActions.includes("RENAME") && isFolder && (
                        <button
                          type="button"
                          className="p-1.5 rounded hover:bg-muted text-muted-foreground hover:text-ink"
                          title="Đổi tên"
                          onClick={() => {
                            setRenameTarget(item);
                            setRenameName(item.name);
                            setRenameDesc(item.description || "");
                          }}
                        >
                          <Pencil className="size-4" />
                        </button>
                      )}
                      {item.allowedActions.includes("MOVE") && isFolder && (
                        <button
                          type="button"
                          className="p-1.5 rounded hover:bg-muted text-muted-foreground hover:text-ink"
                          title="Di chuyển"
                          onClick={() => handleOpenMove(item)}
                        >
                          <Move className="size-4" />
                        </button>
                      )}
                      {item.allowedActions.includes("CHANGE_OWNER") && isFolder && (
                        <button
                          type="button"
                          className="p-1.5 rounded hover:bg-muted text-muted-foreground hover:text-ink"
                          title="Chuyển quyền sở hữu"
                          onClick={() => {
                            setOwnerTarget(item);
                            setNewOwnerId("");
                          }}
                        >
                          <UserCheck className="size-4" />
                        </button>
                      )}
                      {item.allowedActions.includes("ADD_TO_COLLECTION") && (
                        <button
                          type="button"
                          className="p-1.5 rounded hover:bg-muted text-muted-foreground hover:text-ink"
                          title="Thêm vào bộ sưu tập"
                          onClick={() => handleOpenCollection(item)}
                        >
                          <Archive className="size-4" />
                        </button>
                      )}
                      {item.allowedActions.includes("DELETE") && isFolder && (
                        <button
                          type="button"
                          className="p-1.5 rounded hover:bg-red-50 text-red-600"
                          title="Xóa dự án"
                          onClick={() => {
                            setDeleteTarget(item);
                            setConfirmDeleteName("");
                          }}
                        >
                          <Trash2 className="size-4" />
                        </button>
                      )}
                    </div>
                  </div>
                </div>
              </div>
            );
          })}
        </div>
      )}

      {/* Permissions Dialog */}
      <PermissionsDialog
        open={permProjectId !== null}
        resourceType="project"
        resourceId={permProjectId}
        onOpenChange={(open) => {
          if (!open) setPermProjectId(null);
        }}
      />

      {/* Rename Dialog */}
      <Dialog
        open={renameTarget !== null}
        onOpenChange={(open) => {
          if (!open) setRenameTarget(null);
        }}
        title="Đổi tên dự án"
      >
        <div className="space-y-4">
          <div>
            <Label required>Tên dự án mới</Label>
            <Input
              maxLength={128}
              value={renameName}
              onChange={(e) => setRenameName(e.target.value)}
              placeholder="1–128 ký tự, không chứa / \ : * ? &quot; < > |"
            />
          </div>
          <div>
            <Label>Mô tả</Label>
            <Textarea
              maxLength={500}
              value={renameDesc}
              onChange={(e) => setRenameDesc(e.target.value)}
            />
          </div>
          <div className="flex justify-end gap-2 mt-6">
            <Button onClick={() => setRenameTarget(null)}>Hủy</Button>
            <Button
              variant="primary"
              loading={renameSubmitting}
              onClick={handleRenameSubmit}
            >
              Lưu thay đổi
            </Button>
          </div>
        </div>
      </Dialog>

      {/* Move Dialog */}
      <Dialog
        open={moveTarget !== null}
        onOpenChange={(open) => {
          if (!open) setMoveTarget(null);
        }}
        title={`Di chuyển "${moveTarget?.name}"`}
      >
        <div className="space-y-4">
          <div>
            <Label>Chọn thư mục đích</Label>
            <Select
              className="w-full"
              value={selectedTargetId}
              onChange={(e) => setSelectedTargetId(e.target.value)}
            >
              <option value="">(Thư mục gốc - Root)</option>
              {flattenTree(treeNodes)
                .filter((n) => n.id !== moveTarget?.id)
                .map((n) => (
                  <option key={n.id} value={n.id}>
                    {"—".repeat(n.depth)} {n.name}
                  </option>
                ))}
            </Select>
            <div className="mt-1 text-xs text-muted-foreground">
              Lưu ý: Không thể di chuyển dự án vào chính nó hoặc dự án con của
              nó, và độ sâu tối đa không vượt quá 5 cấp.
            </div>
          </div>
          <div className="flex justify-end gap-2 mt-6">
            <Button onClick={() => setMoveTarget(null)}>Hủy</Button>
            <Button
              variant="primary"
              loading={moveSubmitting}
              onClick={handleMoveSubmit}
            >
              Di chuyển
            </Button>
          </div>
        </div>
      </Dialog>

      {/* Change Owner Dialog */}
      <Dialog
        open={ownerTarget !== null}
        onOpenChange={(open) => {
          if (!open) setOwnerTarget(null);
        }}
        title={`Chuyển quyền sở hữu "${ownerTarget?.name}"`}
      >
        <div className="space-y-4">
          <div>
            <Label required>ID người dùng nhận quyền</Label>
            <Input
              value={newOwnerId}
              onChange={(e) => setNewOwnerId(e.target.value)}
              placeholder="Nhập UUID của người dùng có vai trò Creator hoặc Admin"
            />
            <div className="mt-1 text-xs text-muted-foreground">
              Người nhận quyền sở hữu phải đang hoạt động (ACTIVE) và có vai trò
              Creator hoặc Site Admin.
            </div>
          </div>
          <div className="flex justify-end gap-2 mt-6">
            <Button onClick={() => setOwnerTarget(null)}>Hủy</Button>
            <Button
              variant="primary"
              loading={ownerSubmitting}
              onClick={handleOwnerSubmit}
            >
              Xác nhận chuyển
            </Button>
          </div>
        </div>
      </Dialog>

      {/* Delete Project Dialog */}
      <Dialog
        open={deleteTarget !== null}
        onOpenChange={(open) => {
          if (!open) setDeleteTarget(null);
        }}
        title={`Xóa dự án "${deleteTarget?.name}"`}
      >
        <div className="space-y-4">
          <div className="rounded-lg bg-amber-50 p-3 text-sm text-amber-800">
            Dự án này đang chứa <b>{deleteTarget?.childCount}</b> nội dung con
            (dự án và workbook). Việc xóa sẽ chuyển toàn bộ vào thùng rác.
          </div>
          <div>
            <Label required>
              Nhập chính xác tên dự án <b>"{deleteTarget?.name}"</b> để xác nhận
            </Label>
            <Input
              value={confirmDeleteName}
              onChange={(e) => setConfirmDeleteName(e.target.value)}
              placeholder={deleteTarget?.name}
            />
          </div>
          <div className="flex justify-end gap-2 mt-6">
            <Button onClick={() => setDeleteTarget(null)}>Hủy</Button>
            <Button
              variant="danger"
              loading={deleteSubmitting}
              disabled={confirmDeleteName.trim() !== deleteTarget?.name}
              onClick={handleDeleteSubmit}
            >
              Xác nhận xóa
            </Button>
          </div>
        </div>
      </Dialog>

      {/* Add to Collection Dialog */}
      <Dialog
        open={collectionTarget !== null}
        onOpenChange={(open) => {
          if (!open) setCollectionTarget(null);
        }}
        title="Thêm vào bộ sưu tập cá nhân"
      >
        <div className="space-y-4">
          {!creatingCol ? (
            <div>
              <Label>Chọn bộ sưu tập</Label>
              {collections.length === 0 ? (
                <div className="text-sm text-muted-foreground mb-2">
                  Bạn chưa có bộ sưu tập nào.
                </div>
              ) : (
                <Select
                  className="w-full mb-2"
                  value={selectedCollectionId}
                  onChange={(e) => setSelectedCollectionId(e.target.value)}
                >
                  {collections.map((col) => (
                    <option key={col.id} value={col.id}>
                      {col.name} ({col.itemCount} mục)
                    </option>
                  ))}
                </Select>
              )}
              <Button
                variant="ghost"
                className="text-xs text-primary"
                onClick={() => setCreatingCol(true)}
              >
                + Tạo bộ sưu tập mới
              </Button>
            </div>
          ) : (
            <div>
              <Label required>Tên bộ sưu tập mới</Label>
              <Input
                maxLength={128}
                value={newColName}
                onChange={(e) => setNewColName(e.target.value)}
                placeholder="Ví dụ: Báo cáo thường kỳ"
              />
              <div className="mt-2 flex gap-2">
                <Button variant="primary" onClick={handleCreateCollection}>
                  Lưu bộ sưu tập
                </Button>
                <Button onClick={() => setCreatingCol(false)}>Hủy</Button>
              </div>
            </div>
          )}

          <div className="flex justify-end gap-2 mt-6">
            <Button onClick={() => setCollectionTarget(null)}>Đóng</Button>
            {!creatingCol && collections.length > 0 && (
              <Button
                variant="primary"
                onClick={handleAddToCollectionSubmit}
              >
                Thêm vào
              </Button>
            )}
          </div>
        </div>
      </Dialog>
    </>
  );
}
