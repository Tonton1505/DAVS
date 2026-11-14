import { useCallback, useEffect, useMemo, useState } from "react";
import { AlertCircle, Mail, Plus, Search, Shield, Trash2, UserCheck, UserX } from "lucide-react";
import { useNavigate } from "react-router";
import {
  Badge,
  Button,
  DataTable,
  Dialog,
  Input,
  Label,
  PageHeader,
  Select,
  StatusBadge,
  Td,
} from "../../components/ui";
import { extractApiError } from "../../lib/axios/client";
import type { BackendSiteRole } from "../../services/auth.service";
import {
  userService,
  type LicensePoolDto,
  type UserManagementDto,
} from "../../services/user.service";
import { useUIStore } from "../../stores/ui.store";

const SITE_ROLE_LABELS: Record<BackendSiteRole, string> = {
  SITE_ADMIN: "Quản trị viên",
  CREATOR: "Creator",
  EXPLORER: "Explorer",
  VIEWER: "Viewer",
};

const STATUS_LABELS: Record<string, string> = {
  ACTIVE: "Đang hoạt động",
  PENDING: "Chờ kích hoạt",
  UNINVITED: "Chưa gửi thư mời",
  DISABLED: "Vô hiệu hóa",
  LOCKED: "Tạm khóa",
};

export default function UsersPage() {
  const nav = useNavigate();
  const toast = useUIStore((s) => s.toast);

  const [users, setUsers] = useState<UserManagementDto[]>([]);
  const [totalElements, setTotalElements] = useState(0);
  const [licenses, setLicenses] = useState<LicensePoolDto[]>([]);
  const [loading, setLoading] = useState(true);
  const [apiError, setApiError] = useState("");

  const [q, setQ] = useState("");
  const [roleFilter, setRoleFilter] = useState("ALL");
  const [statusFilter, setStatusFilter] = useState("ALL");
  const [page, setPage] = useState(1);
  const [selected, setSelected] = useState<string[]>([]);
  const [batchRole, setBatchRole] = useState<BackendSiteRole>("EXPLORER");

  const [deleteTarget, setDeleteTarget] = useState<UserManagementDto | null>(null);
  const [newOwnerId, setNewOwnerId] = useState("");
  const [deleting, setDeleting] = useState(false);

  const loadData = useCallback(async () => {
    setLoading(true);
    try {
      const [pageData, licenseList] = await Promise.all([
        userService.searchUsers({
          q: q.trim() || undefined,
          role: roleFilter !== "ALL" ? roleFilter : undefined,
          status: statusFilter !== "ALL" ? statusFilter : undefined,
          page: page - 1,
          size: 25,
        }),
        userService.listLicenses().catch(() => [] as LicensePoolDto[]),
      ]);
      setUsers(pageData.content ?? []);
      setTotalElements(pageData.totalElements ?? 0);
      setLicenses(licenseList);
    } catch (err) {
      const parsed = extractApiError(err);
      setApiError(parsed?.message || "Không thể tải danh sách người dùng.");
    } finally {
      setLoading(false);
    }
  }, [q, roleFilter, statusFilter, page]);

  useEffect(() => {
    void loadData();
  }, [loadData]);

  const eligibleOwners = useMemo(() => {
    return users.filter(
      (u) =>
        u.id !== deleteTarget?.id &&
        u.status !== "DISABLED" &&
        (u.siteRole === "SITE_ADMIN" || u.siteRole === "CREATOR"),
    );
  }, [users, deleteTarget]);

  const handleRoleChange = async (userId: string, newRole: BackendSiteRole) => {
    setApiError("");
    try {
      await userService.changeRole(userId, newRole);
      toast(`Đã cập nhật vai trò thành ${SITE_ROLE_LABELS[newRole]}`);
      await loadData();
    } catch (err) {
      const parsed = extractApiError(err);
      setApiError(parsed?.message || "Không thể đổi vai trò người dùng.");
    }
  };

  const handleBatchRoleChange = async () => {
    if (selected.length === 0) return;
    setApiError("");
    try {
      await userService.batchChangeRole(selected, batchRole);
      toast(`Đã đổi vai trò cho ${selected.length} người dùng`);
      setSelected([]);
      await loadData();
    } catch (err) {
      const parsed = extractApiError(err);
      setApiError(parsed?.message || "Không thể đổi vai trò hàng loạt.");
    }
  };

  const handleToggleActive = async (user: UserManagementDto) => {
    setApiError("");
    try {
      if (user.status === "DISABLED") {
        await userService.activateUser(user.id);
        toast(`Đã kích hoạt tài khoản ${user.email}`);
      } else {
        await userService.deactivateUser(user.id);
        toast(`Đã vô hiệu hóa tài khoản ${user.email}`);
      }
      await loadData();
    } catch (err) {
      const parsed = extractApiError(err);
      setApiError(parsed?.message || "Không thể cập nhật trạng thái tài khoản.");
    }
  };

  const handleResendInvite = async (user: UserManagementDto) => {
    setApiError("");
    try {
      await userService.resendInvite(user.id);
      toast(`Đã gửi lại lời mời tới ${user.email}`);
      await loadData();
    } catch (err) {
      const parsed = extractApiError(err);
      setApiError(parsed?.message || "Không thể gửi lại thư mời.");
    }
  };

  const openDeleteModal = (user: UserManagementDto) => {
    setApiError("");
    setDeleteTarget(user);
    const firstEligible = users.find(
      (u) =>
        u.id !== user.id &&
        u.status !== "DISABLED" &&
        (u.siteRole === "SITE_ADMIN" || u.siteRole === "CREATOR"),
    );
    setNewOwnerId(firstEligible?.id ?? "");
  };

  const confirmDeleteUser = async () => {
    if (!deleteTarget) return;
    setDeleting(true);
    setApiError("");
    try {
      await userService.deleteUser(
        deleteTarget.id,
        deleteTarget.ownedResourcesCount > 0 || newOwnerId ? newOwnerId || undefined : undefined,
      );
      toast(`Đã xóa người dùng ${deleteTarget.email}`);
      setDeleteTarget(null);
      await loadData();
    } catch (err) {
      const parsed = extractApiError(err);
      setApiError(parsed?.message || "Không thể xóa người dùng.");
    } finally {
      setDeleting(false);
    }
  };

  return (
    <>
      <PageHeader
        title="Người dùng"
        eyebrow="Quản trị"
        actions={
          <Button variant="primary" onClick={() => nav("/admin/users/add")}>
            <Plus className="size-4" />
            Thêm người dùng
          </Button>
        }
      />

      {licenses.length > 0 && (
        <div className="mb-4 grid grid-cols-2 gap-3 sm:grid-cols-4">
          {licenses.map((pool) => (
            <div key={pool.role} className="rounded-xl border bg-white p-3 text-xs">
              <div className="flex items-center justify-between text-muted-foreground">
                <span>{SITE_ROLE_LABELS[pool.role] ?? pool.role}</span>
                <Shield className="size-3.5" />
              </div>
              <div className="mt-1 text-base font-semibold text-ink">
                {pool.used} / {pool.total}{" "}
                <span className="text-xs font-normal text-muted-foreground">
                  (còn {pool.available})
                </span>
              </div>
            </div>
          ))}
        </div>
      )}

      {apiError && (
        <div
          role="alert"
          className="mb-4 flex items-start gap-2 rounded-lg border border-primary-border bg-primary-tint p-3 text-sm text-primary"
        >
          <AlertCircle className="mt-0.5 size-4 shrink-0" />
          <span>{apiError}</span>
        </div>
      )}

      <div className="mb-4 flex flex-wrap gap-2">
        <div className="relative min-w-60 flex-1">
          <Search className="absolute left-3 top-3 size-4 text-muted-foreground" />
          <Input
            className="pl-9"
            value={q}
            onChange={(e) => {
              setQ(e.target.value);
              setPage(1);
            }}
            placeholder="Tìm tên hoặc email…"
          />
        </div>
        <Select
          aria-label="Lọc theo vai trò"
          value={roleFilter}
          onChange={(e) => {
            setRoleFilter(e.target.value);
            setPage(1);
          }}
        >
          <option value="ALL">Tất cả vai trò</option>
          <option value="SITE_ADMIN">Quản trị viên</option>
          <option value="CREATOR">Creator</option>
          <option value="EXPLORER">Explorer</option>
          <option value="VIEWER">Viewer</option>
        </Select>
        <Select
          aria-label="Lọc theo trạng thái"
          value={statusFilter}
          onChange={(e) => {
            setStatusFilter(e.target.value);
            setPage(1);
          }}
        >
          <option value="ALL">Tất cả trạng thái</option>
          <option value="ACTIVE">Đang hoạt động</option>
          <option value="PENDING">Chờ kích hoạt</option>
          <option value="UNINVITED">Chưa gửi thư mời</option>
          <option value="DISABLED">Vô hiệu hóa</option>
        </Select>
      </div>

      {selected.length > 0 && (
        <div className="mb-3 flex flex-wrap items-center gap-3 rounded-lg bg-ink px-4 py-3 text-sm text-white">
          <b>Đã chọn {selected.length}</b>
          <Select
            aria-label="Vai trò hàng loạt"
            value={batchRole}
            onChange={(e) => setBatchRole(e.target.value as BackendSiteRole)}
            className="h-8 bg-white text-ink"
          >
            <option value="VIEWER">Viewer</option>
            <option value="EXPLORER">Explorer</option>
            <option value="CREATOR">Creator</option>
            <option value="SITE_ADMIN">Quản trị viên</option>
          </Select>
          <Button variant="primary" className="h-8 px-3 text-xs" onClick={handleBatchRoleChange}>
            Đổi vai trò
          </Button>
        </div>
      )}

      <DataTable
        headers={[
          "",
          "Tên",
          "Email",
          "Vai trò",
          "Nhóm",
          "Lần đăng nhập cuối",
          "Trạng thái",
          "Thao tác",
        ]}
        loading={loading}
        count={totalElements}
        page={page}
        pageSize={25}
        onPageChange={setPage}
        emptyTitle="Không tìm thấy người dùng"
        emptyDescription="Thử thay đổi bộ lọc hoặc thêm người dùng mới."
      >
        {users.map((x) => (
          <tr key={x.id} className="hover:bg-muted">
            <Td>
              <input
                type="checkbox"
                aria-label={`Chọn ${x.email}`}
                checked={selected.includes(x.id)}
                onChange={(e) =>
                  setSelected(
                    e.target.checked ? [...selected, x.id] : selected.filter((id) => id !== x.id),
                  )
                }
                className="accent-primary"
              />
            </Td>
            <Td>
              <div className="flex items-center gap-2">
                <span className="flex size-7 items-center justify-center rounded-full bg-ink text-[9px] text-white">
                  {(x.fullName || x.email).slice(0, 2).toUpperCase()}
                </span>
                <div>
                  <b>{x.fullName || x.email}</b>
                  {x.ownedResourcesCount > 0 && (
                    <div className="text-[11px] text-muted-foreground">
                      Sở hữu {x.ownedResourcesCount} nội dung
                    </div>
                  )}
                </div>
              </div>
            </Td>
            <Td>{x.email}</Td>
            <Td>
              <Select
                aria-label={`Vai trò của ${x.email}`}
                value={x.siteRole}
                onChange={(e) => handleRoleChange(x.id, e.target.value as BackendSiteRole)}
                className="h-8 text-xs"
              >
                <option value="VIEWER">Viewer</option>
                <option value="EXPLORER">Explorer</option>
                <option value="CREATOR">Creator</option>
                <option value="SITE_ADMIN">Quản trị viên</option>
              </Select>
            </Td>
            <Td>
              <div className="flex flex-wrap gap-1">
                {x.groups.map((g) => (
                  <Badge key={g}>{g}</Badge>
                ))}
              </div>
            </Td>
            <Td>
              {x.lastLoginAt
                ? new Date(x.lastLoginAt).toLocaleString("vi-VN")
                : "Chưa đăng nhập"}
            </Td>
            <Td>
              <StatusBadge status={STATUS_LABELS[x.status] ?? x.status} />
            </Td>
            <Td>
              <div className="flex items-center gap-1">
                {(x.status === "PENDING" || x.status === "UNINVITED") && (
                  <Button
                    type="button"
                    variant="ghost"
                    className="h-8 px-2 text-xs"
                    title="Gửi lại lời mời"
                    onClick={() => handleResendInvite(x)}
                  >
                    <Mail className="size-3.5" />
                    Mời lại
                  </Button>
                )}
                <Button
                  type="button"
                  variant="ghost"
                  className="h-8 px-2 text-xs"
                  onClick={() => handleToggleActive(x)}
                >
                  {x.status === "DISABLED" ? (
                    <>
                      <UserCheck className="size-3.5" />
                      Kích hoạt
                    </>
                  ) : (
                    <>
                      <UserX className="size-3.5" />
                      Vô hiệu hóa
                    </>
                  )}
                </Button>
                <Button
                  type="button"
                  variant="danger"
                  className="h-8 px-2 text-xs"
                  aria-label={`Xóa ${x.email}`}
                  onClick={() => openDeleteModal(x)}
                >
                  <Trash2 className="size-3.5" />
                  Xóa
                </Button>
              </div>
            </Td>
          </tr>
        ))}
      </DataTable>

      <Dialog
        open={!!deleteTarget}
        onOpenChange={(open) => !open && setDeleteTarget(null)}
        title="Xóa người dùng"
        description={
          deleteTarget
            ? `Xác nhận xóa tài khoản ${deleteTarget.email} khỏi hệ thống.`
            : undefined
        }
      >
        {deleteTarget && (
          <div className="space-y-4">
            {deleteTarget.ownedResourcesCount > 0 && (
              <div className="rounded-lg border border-primary-border bg-primary-tint p-3 text-xs text-primary">
                Người dùng này đang sở hữu <b>{deleteTarget.ownedResourcesCount}</b> nội dung
                (Project/Workbook/Nguồn dữ liệu). Vui lòng chọn người nhận quyền sở hữu trước khi
                xóa.
              </div>
            )}
            <div>
              <Label htmlFor="transfer-owner-select" required={deleteTarget.ownedResourcesCount > 0}>
                Người nhận quyền sở hữu nội dung
              </Label>
              <Select
                id="transfer-owner-select"
                value={newOwnerId}
                onChange={(e) => setNewOwnerId(e.target.value)}
                className="w-full"
              >
                <option value="">-- Chọn người nhận quyền sở hữu --</option>
                {eligibleOwners.map((owner) => (
                  <option key={owner.id} value={owner.id}>
                    {owner.fullName} ({owner.email}) · {SITE_ROLE_LABELS[owner.siteRole]}
                  </option>
                ))}
              </Select>
            </div>
            <div className="flex justify-end gap-2 pt-2">
              <Button type="button" onClick={() => setDeleteTarget(null)} disabled={deleting}>
                Hủy
              </Button>
              <Button
                type="button"
                variant="danger"
                loading={deleting}
                onClick={confirmDeleteUser}
              >
                Xác nhận xóa
              </Button>
            </div>
          </div>
        )}
      </Dialog>
    </>
  );
}
