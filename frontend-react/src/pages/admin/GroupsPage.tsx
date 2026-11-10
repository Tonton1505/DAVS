import { useState } from "react";
import { Plus } from "lucide-react";
import { Button, DataTable, PageHeader, RowMenu, Td } from "../../components/ui";
import { useGroupStore } from "../../stores/group.store";
import GroupDialog from "./GroupDialog";
export default function GroupsPage() {
  const items = useGroupStore((s) => s.items),
    [open, setOpen] = useState(false);
  return (
    <>
      <PageHeader
        title="Nhóm"
        eyebrow="Quản trị"
        actions={
          <Button variant="primary" onClick={() => setOpen(true)}>
            <Plus className="size-4" />
            Thêm nhóm
          </Button>
        }
      />
      <DataTable
        headers={[
          "Tên nhóm",
          "Mô tả",
          "Vai trò tối thiểu",
          "Số thành viên",
          "Cập nhật",
          "Thao tác",
        ]}
        count={items.length}
      >
        {items.map((x, i) => (
          <tr key={x.id}>
            <Td>
              <b>{x.name}</b>
            </Td>
            <Td>Nhóm người dùng Công ty ABC</Td>
            <Td>{i === 2 ? "Creator" : i === 1 ? "Explorer" : "Viewer"}</Td>
            <Td>{x.members}</Td>
            <Td>Hôm nay, 09:20</Td>
            <Td>
              <RowMenu />
            </Td>
          </tr>
        ))}
      </DataTable>
      <GroupDialog open={open} onOpenChange={setOpen} />
    </>
  );
}
