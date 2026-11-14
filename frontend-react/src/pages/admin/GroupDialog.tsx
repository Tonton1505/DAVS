import { useEffect, useState } from "react";
import { Alert, Button, Dialog, Input, Label, Select, Textarea } from "../../components/ui";
import {
  groupService,
  type GroupDto,
  type GroupMinRole,
} from "../../services/group.service";
import { userService, type UserManagementDto } from "../../services/user.service";
import { useUIStore } from "../../stores/ui.store";

const ROLE_WEIGHT: Record<string, number> = {
  VIEWER: 1,
  EXPLORER: 2,
  CREATOR: 3,
  ADMIN: 4,
};

export default function GroupDialog({
  open,
  onOpenChange,
  editingGroup,
  onSaved,
}: {
  open: boolean;
  onOpenChange: (v: boolean) => void;
  editingGroup?: GroupDto | null;
  onSaved?: () => void;
}) {
  const [name, setName] = useState("");
  const [description, setDescription] = useState("");
  const [minRole, setMinRole] = useState<GroupMinRole | "">("");
  const [searchQuery, setSearchQuery] = useState("");
  const [availableUsers, setAvailableUsers] = useState<UserManagementDto[]>([]);
  const [selectedUsers, setSelectedUsers] = useState<UserManagementDto[]>([]);
  const [submitting, setSubmitting] = useState(false);
  const [errorMessage, setErrorMessage] = useState<string | null>(null);
  const toast = useUIStore((s) => s.toast);

  useEffect(() => {
    if (!open) return;
    setErrorMessage(null);
    if (editingGroup) {
      setName(editingGroup.name);
      setDescription(editingGroup.description ?? "");
      setMinRole(editingGroup.minRole ?? "");
      groupService
        .getDetail(editingGroup.id)
        .then((detail) => {
          setSelectedUsers(
            detail.members.map((m) => ({
              id: m.id,
              email: m.email,
              fullName: m.fullName,
              role: m.role as UserManagementDto["role"],
              status: m.status as UserManagementDto["status"],
              lastLoginAt: null,
              createdAt: m.joinedAt,
            })),
          );
        })
        .catch(() => setSelectedUsers([]));
    } else {
      setName("");
      setDescription("");
      setMinRole("VIEWER");
      setSelectedUsers([]);
    }
  }, [open, editingGroup]);

  useEffect(() => {
    if (!open) return;
    let cancelled = false;
    userService
      .searchUsers({ q: searchQuery || undefined, page: 0, size: 20 })
      .then((res) => {
        if (!cancelled) setAvailableUsers(res.content ?? []);
      })
      .catch(() => {
        if (!cancelled) setAvailableUsers([]);
      });
    return () => {
      cancelled = true;
    };
  }, [open, searchQuery]);

  const trimmedName = name.trim();
  const isNameValid = trimmedName.length >= 3 && trimmedName.length <= 100;

  const promotedCount = minRole
    ? selectedUsers.filter(
        (u) => (ROLE_WEIGHT[u.role] ?? 1) < (ROLE_WEIGHT[minRole] ?? 1),
      ).length
    : 0;

  function addMemberChip(user: UserManagementDto) {
    if (selectedUsers.some((u) => u.id === user.id)) return;
    setSelectedUsers((prev) => [...prev, user]);
  }

  function removeMemberChip(userId: string) {
    setSelectedUsers((prev) => prev.filter((u) => u.id !== userId));
  }

  async function handleSubmit() {
    if (!isNameValid) {
      setErrorMessage("Tên nhóm phải từ 3 đến 100 ký tự (VAL-GRP-01).");
      return;
    }
    setSubmitting(true);
    setErrorMessage(null);
    try {
      const normalizedMinRole = minRole === "" ? null : minRole;
      if (editingGroup) {
        const res = await groupService.update(editingGroup.id, {
          name: trimmedName,
          description: description.trim() || null,
          minRole: normalizedMinRole,
        });
        toast(
          res.promotedCount > 0
            ? `Đã cập nhật nhóm · Tự động nâng vai trò cho ${res.promotedCount} thành viên`
            : "Đã cập nhật nhóm",
        );
      } else {
        const res = await groupService.create({
          name: trimmedName,
          description: description.trim() || null,
          minRole: normalizedMinRole,
          memberIds: selectedUsers.map((u) => u.id),
        });
        toast(
          res.promotedCount > 0
            ? `Đã tạo nhóm · Tự động nâng vai trò cho ${res.promotedCount} thành viên`
            : "Đã tạo nhóm",
        );
      }
      onSaved?.();
      onOpenChange(false);
    } catch (err) {
      setErrorMessage(err instanceof Error ? err.message : "Không thể lưu nhóm");
    } finally {
      setSubmitting(false);
    }
  }

  return (
    <Dialog
      open={open}
      onOpenChange={onOpenChange}
      title={editingGroup ? "Chỉnh sửa nhóm" : "Thêm nhóm"}
      className="max-w-xl"
    >
      <div className="space-y-4">
        {errorMessage && <Alert>{errorMessage}</Alert>}
        <div>
          <Label required>Tên nhóm</Label>
          <Input
            aria-label="Tên nhóm"
            value={name}
            onChange={(e) => setName(e.target.value)}
            maxLength={100}
            disabled={editingGroup?.system}
            placeholder="Nhập tên nhóm (3–100 ký tự)"
          />
          <div className="flex justify-between text-xs text-muted-foreground">
            <span>
              {trimmedName.length > 0 && trimmedName.length < 3
                ? "Tên nhóm cần tối thiểu 3 ký tự"
                : ""}
            </span>
            <span>{name.length} / 100</span>
          </div>
        </div>
        <div>
          <Label>Mô tả</Label>
          <Textarea
            aria-label="Mô tả nhóm"
            value={description}
            onChange={(e) => setDescription(e.target.value)}
            maxLength={500}
          />
        </div>
        <div>
          <Label>Vai trò đăng nhập tối thiểu</Label>
          <Select
            aria-label="Vai trò tối thiểu"
            className="w-full"
            value={minRole}
            onChange={(e) => setMinRole(e.target.value as GroupMinRole | "")}
          >
            <option value="">Không áp dụng</option>
            <option value="VIEWER">Viewer</option>
            <option value="EXPLORER">Explorer</option>
            <option value="CREATOR">Creator</option>
          </Select>
        </div>
        {!editingGroup && (
          <div>
            <Label>Thành viên</Label>
            <Input
              aria-label="Tìm người dùng thêm vào nhóm"
              placeholder="Tìm người dùng…"
              value={searchQuery}
              onChange={(e) => setSearchQuery(e.target.value)}
            />
            {availableUsers.length > 0 && (
              <div className="mt-2 max-h-32 overflow-y-auto rounded-lg border p-2 text-xs">
                {availableUsers.map((u) => {
                  const chosen = selectedUsers.some((s) => s.id === u.id);
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
                        disabled={chosen}
                        onClick={() => addMemberChip(u)}
                      >
                        {chosen ? "Đã chọn" : "Thêm"}
                      </Button>
                    </div>
                  );
                })}
              </div>
            )}
            <div className="mt-2 flex flex-wrap gap-2">
              {selectedUsers.map((u) => (
                <button
                  key={u.id}
                  type="button"
                  onClick={() => removeMemberChip(u.id)}
                  className="rounded-full bg-muted px-3 py-1 text-xs"
                >
                  {u.fullName} ({u.role}) ×
                </button>
              ))}
            </div>
          </div>
        )}
        {minRole && (
          <Alert>
            {promotedCount > 0
              ? `${promotedCount} người sẽ được nâng vai trò lên ${minRole} khi áp dụng mức tối thiểu này.`
              : `Thành viên có vai trò thấp hơn ${minRole} sẽ được tự động nâng lên ${minRole} (BUS-GRP-01).`}
          </Alert>
        )}
      </div>
      <div className="mt-6 flex justify-end gap-2">
        <Button onClick={() => onOpenChange(false)}>Hủy</Button>
        <Button
          variant="primary"
          disabled={!isNameValid || submitting}
          onClick={handleSubmit}
        >
          {submitting ? "Đang lưu…" : editingGroup ? "Lưu thay đổi" : "Tạo nhóm"}
        </Button>
      </div>
    </Dialog>
  );
}
