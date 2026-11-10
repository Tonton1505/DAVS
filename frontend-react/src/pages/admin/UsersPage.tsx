import { useEffect, useMemo, useState } from "react";
import { Plus, Search } from "lucide-react";
import { useNavigate } from "react-router";
import {
  Badge,
  Button,
  DataTable,
  Input,
  PageHeader,
  RowMenu,
  Select,
  StatusBadge,
  Td,
} from "../../components/ui";
import { useUserStore } from "../../stores/user.store";
import { ROLE_LABELS } from "../../common/constants/app-constants";
export default function UsersPage() {
  const nav = useNavigate(),
    { items, loading, load } = useUserStore(),
    [q, setQ] = useState(""),
    [role, setRole] = useState("all"),
    [selected, setSelected] = useState<string[]>([]);
  useEffect(() => {
    load();
  }, [load]);
  const shown = useMemo(
    () =>
      items
        .filter(
          (x) =>
            (x.name.toLowerCase().includes(q.toLowerCase()) || x.email.includes(q)) &&
            (role === "all" || x.role === role),
        )
        .slice(0, 25),
    [items, q, role],
  );
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
      <div className="mb-4 flex gap-2">
        <div className="relative flex-1">
          <Search className="absolute left-3 top-3 size-4 text-muted-foreground" />
          <Input
            className="pl-9"
            value={q}
            onChange={(e) => setQ(e.target.value)}
            placeholder="Tìm tên hoặc email…"
          />
        </div>
        <Select value={role} onChange={(e) => setRole(e.target.value)}>
          <option value="all">Tất cả vai trò</option>
          <option value="admin">Quản trị viên</option>
          <option value="creator">Creator</option>
          <option value="explorer">Explorer</option>
          <option value="viewer">Viewer</option>
        </Select>
        <Select>
          <option>Tất cả trạng thái</option>
          <option>Đang hoạt động</option>
          <option>Vô hiệu hóa</option>
        </Select>
      </div>
      {selected.length > 0 && (
        <div className="mb-3 flex items-center gap-4 rounded-lg bg-ink px-4 py-3 text-sm text-white">
          <b>Đã chọn {selected.length}</b>
          <button>Đổi vai trò</button>
          <button>Vô hiệu hóa</button>
          <button>Xóa</button>
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
        count={items.length}
      >
        {shown.map((x) => (
          <tr key={x.id} className="hover:bg-muted">
            <Td>
              <input
                type="checkbox"
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
                  {x.name
                    .split(" ")
                    .slice(-2)
                    .map((n) => n[0])
                    .join("")}
                </span>
                <b>{x.name}</b>
              </div>
            </Td>
            <Td>{x.email}</Td>
            <Td>
              <Badge>{ROLE_LABELS[x.role]}</Badge>
            </Td>
            <Td>
              <div className="flex gap-1">
                {x.groups.slice(0, 2).map((g) => (
                  <span key={g} className="rounded bg-muted px-2 py-1 text-[10px]">
                    {g}
                  </span>
                ))}
              </div>
            </Td>
            <Td>{x.lastLogin}</Td>
            <Td>
              <StatusBadge status={x.status === "active" ? "Đang hoạt động" : "Vô hiệu hóa"} />
            </Td>
            <Td>
              <RowMenu />
            </Td>
          </tr>
        ))}
      </DataTable>
    </>
  );
}
