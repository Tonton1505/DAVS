import { useCallback, useEffect, useState } from "react";
import { Edit2, Plus, Trash2, Users } from "lucide-react";
import {
  Alert,
  Button,
  DataTable,
  Dialog,
  Input,
  PageHeader,
  Td,
} from "../../components/ui";
import {
  groupService,
  type GroupDetailDto,
  type GroupDto,
} from "../../services/group.service";
import { userService, type UserManagementDto } from "../../services/user.service";
import { useUIStore } from "../../stores/ui.store";
import GroupDialog from "./GroupDialog";

const FALLBACK_GROUPS: GroupDto[] = [
  {
    id: "00000000-0000-0000-0000-000000000010",
    name: "All Users",
    description: "System default group containing all active users",
    minRole: null,
    system: true,
    memberCount: 5,
    createdAt: "2026-10-01T09:00:00Z",
    updatedAt: "2026-10-31T09:20:00Z",
  },
  {
    id: "1",
    name: "Phòng Kinh doanh",
    description: "Nhóm người dùng Công ty ABC",
    minRole: "VIEWER",
    system: false,
    memberCount: 32,
    createdAt: "2026-10-01T09:00:00Z",
    updatedAt: "2026-10-31T09:20:00Z",
  },
  {
    id: "2",
    name: "Phòng Tài chính",
    description: "Nhóm phân tích tài chính",
    minRole: "EXPLORER",
    system: false,
    memberCount: 18,
    createdAt: "2026-10-01T09:00:00Z",
    updatedAt: "2026-10-31T09:20:00Z",
  },
];

export default function GroupsPage() {
  const [groups, setGroups] = useState<GroupDto[]>([]);
  const [loading, setLoading] = useState(true);
  const [errorMessage, setErrorMessage] = useState<string | null>(null);
  const [dialogOpen, setDialogOpen] = useState(false);
  const [editingGroup, setEditingGroup] = useState<GroupDto | null>(null);

  const [memberModalGroup, setMemberModalGroup] = useState<GroupDetailDto | null>(null);
  const [userSearch, setUserSearch] = useState("");
  const [candidateUsers, setCandidateUsers] = useState<UserManagementDto[]>([]);
  const toast = useUIStore((s) => s.toast);

  const loadGroups = useCallback(async () => {
    setLoading(true);
    try {
      const list = await groupService.list();
      setGroups(list);
    } catch {
      setGroups(FALLBACK_GROUPS);
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => {
    void loadGroups();
  }, [loadGroups]);

  async function openMembersModal(group: GroupDto) {
    setErrorMessage(null);
    try {
      const detail = await groupService.getDetail(group.id);
      setMemberModalGroup(detail);
      const usersPage = await userService.searchUsers({ page: 0, size: 50 });
      setCandidateUsers(usersPage.content ?? []);
    } catch (err) {
      setErrorMessage(
        err instanceof Error ? err.message : "Không thể tải danh sách thành viên",
      );
    }
  }

  async function handleAddMember(userId: string) {
    if (!memberModalGroup) return;
    try {
      const res = await groupService.addMembers(memberModalGroup.id, [userId]);
      toast(
        res.promotedCount > 0
          ? `Đã thêm thành viên · Tự động nâng vai trò cho ${res.promotedCount} người`
          : "Đã thêm thành viên vào nhóm",
      );
      const refreshed = await groupService.getDetail(memberModalGroup.id);
      setMemberModalGroup(refreshed);
      await loadGroups();
    } catch (err) {
      setErrorMessage(
        err instanceof Error ? err.message : "Không thể thêm thành viên",
      );
    }
  }

  async function handleRemoveMember(userId: string) {
    if (!memberModalGroup) return;
    if (
      memberModalGroup.system ||
      memberModalGroup.name.toLowerCase() === "all users"
    ) {
      setErrorMessage(
        "Không thể gỡ thành viên khỏi nhóm mặc định 'All Users' (BUS-GRP-02).",
      );
      return;
    }
    try {
      await groupService.removeMember(memberModalGroup.id, userId);
      toast("Đã gỡ thành viên khỏi nhóm");
      const refreshed = await groupService.getDetail(memberModalGroup.id);
      setMemberModalGroup(refreshed);
      await loadGroups();
    } catch (err) {
      setErrorMessage(
        err instanceof Error ? err.message : "Không thể gỡ thành viên",
      );
    }
  }

  async function handleDeleteGroup(group: GroupDto) {
    if (group.system || group.name.toLowerCase() === "all users") {
      setErrorMessage("Không thể xóa nhóm hệ thống 'All Users' (BUS-GRP-02).");
      return;
    }
    try {
      await groupService.remove(group.id);
      toast(`Đã xóa nhóm ${group.name}`);
      await loadGroups();
    } catch (err) {
      setErrorMessage(
        err instanceof Error ? err.message : "Không thể xóa nhóm",
      );
    }
  }

  return (
    <>
      <PageHeader
        title="Nhóm"
        eyebrow="Quản trị"
        actions={
          <Button
            variant="primary"
            onClick={() => {
              setEditingGroup(null);
              setDialogOpen(true);
            }}
          >
            <Plus className="size-4" />
            Thêm nhóm
          </Button>
        }
      />

      {errorMessage && (
        <div className="mb-4">
          <Alert>{errorMessage}</Alert>
        </div>
      )}

      <DataTable
        headers={[
          "Tên nhóm",
          "Mô tả",
          "Vai trò tối thiểu",
          "Số thành viên",
          "Cập nhật",
          "Thao tác",
        ]}
        loading={loading}
        count={groups.length}
      >
        {groups.map((x) => {
          const isAllUsers = x.system || x.name.toLowerCase() === "all users";
          return (
            <tr key={x.id}>
              <Td>
                <div className="flex items-center gap-2">
                  <b>{x.name}</b>
                  {isAllUsers && (
                    <span className="rounded bg-muted px-2 py-0.5 text-[10px] font-medium">
                      Hệ thống
                    </span>
                  )}
                </div>
              </Td>
              <Td>{x.description || "Nhóm người dùng Công ty ABC"}</Td>
              <Td>{x.minRole ?? "Viewer"}</Td>
              <Td>{x.memberCount}</Td>
              <Td>
                {x.updatedAt
                  ? new Date(x.updatedAt).toLocaleDateString("vi-VN")
                  : "Hôm nay"}
              </Td>
              <Td>
                <div className="flex items-center gap-1">
                  <Button
                    size="sm"
                    onClick={() => void openMembersModal(x)}
                    aria-label={`Thành viên ${x.name}`}
                  >
                    <Users className="size-3.5" />
                    Thành viên
                  </Button>
                  <Button
                    size="sm"
                    onClick={() => {
                      setEditingGroup(x);
                      setDialogOpen(true);
                    }}
                    aria-label={`Sửa ${x.name}`}
                  >
                    <Edit2 className="size-3.5" />
                  </Button>
                  <Button
                    size="sm"
                    disabled={isAllUsers}
                    title={
                      isAllUsers
                        ? "Không thể xóa nhóm All Users (BUS-GRP-02)"
                        : "Xóa nhóm"
                    }
                    onClick={() => void handleDeleteGroup(x)}
                    aria-label={`Xóa ${x.name}`}
                  >
                    <Trash2 className="size-3.5" />
                  </Button>
                </div>
              </Td>
            </tr>
          );
        })}
      </DataTable>

      <GroupDialog
        open={dialogOpen}
        onOpenChange={setDialogOpen}
        editingGroup={editingGroup}
        onSaved={() => void loadGroups()}
      />

      <Dialog
        open={!!memberModalGroup}
        onOpenChange={(v) => !v && setMemberModalGroup(null)}
        title={`Thành viên · ${memberModalGroup?.name ?? ""}`}
        className="max-w-xl"
      >
        {memberModalGroup && (
          <div className="space-y-4">
            <div>
              <Input
                placeholder="Tìm người dùng để thêm vào nhóm…"
                value={userSearch}
                onChange={(e) => setUserSearch(e.target.value)}
              />
              <div className="mt-2 max-h-36 overflow-y-auto rounded-lg border p-2 text-xs">
                {candidateUsers
                  .filter(
                    (u) =>
                      !userSearch ||
                      u.fullName.toLowerCase().includes(userSearch.toLowerCase()) ||
                      u.email.toLowerCase().includes(userSearch.toLowerCase()),
                  )
                  .map((u) => {
                    const alreadyMember = memberModalGroup.members.some(
                      (m) => m.id === u.id,
                    );
                    return (
                      <div
                        key={u.id}
                        className="flex items-center justify-between py-1"
                      >
                        <span>
                          {u.fullName} ({u.email}) · {u.role}
                        </span>
                        <Button
                          size="sm"
                          disabled={alreadyMember}
                          onClick={() => void handleAddMember(u.id)}
                        >
                          {alreadyMember ? "Đã trong nhóm" : "Thêm"}
                        </Button>
                      </div>
                    );
                  })}
              </div>
            </div>

            <div>
              <div className="mb-2 text-xs font-semibold">
                Danh sách thành viên hiện tại ({memberModalGroup.members.length})
              </div>
              <div className="max-h-48 divide-y overflow-y-auto rounded-lg border text-xs">
                {memberModalGroup.members.map((m) => {
                  const isAllUsers =
                    memberModalGroup.system ||
                    memberModalGroup.name.toLowerCase() === "all users";
                  return (
                    <div
                      key={m.id}
                      className="flex items-center justify-between p-2.5"
                    >
                      <div>
                        <div className="font-medium">{m.fullName}</div>
                        <div className="text-muted-foreground">
                          {m.email} · {m.role}
                        </div>
                      </div>
                      <Button
                        size="sm"
                        disabled={isAllUsers}
                        onClick={() => void handleRemoveMember(m.id)}
                      >
                        {isAllUsers ? "Mặc định" : "Gỡ"}
                      </Button>
                    </div>
                  );
                })}
              </div>
            </div>
          </div>
        )}
      </Dialog>
    </>
  );
}
