import { useState } from "react";
import { RotateCcw } from "lucide-react";
import {
  Badge,
  Button,
  DataTable,
  Dialog,
  Label,
  PageHeader,
  Td,
  Textarea,
} from "../../components/ui";
import { useUIStore } from "../../stores/ui.store";
export default function VersionHistoryPage() {
  const [restore, setRestore] = useState(false),
    [note, setNote] = useState(""),
    toast = useUIStore((s) => s.toast);
  const versions = Array.from({ length: 10 }, (_, i) => ({
    v: `v${25 - i}`,
    date: `${31 - i}/10/2026 09:20`,
    user: i % 2 ? "Nguyễn Văn An" : "Lê Thu Hà",
    size: `${24 + i} MB`,
  }));
  return (
    <>
      <PageHeader title="Lịch sử phiên bản" eyebrow="Tổng quan kinh doanh › Phiên bản" />
      <div className="mb-3 text-sm text-muted-foreground">
        Hệ thống chỉ giữ tối đa 25 phiên bản gần nhất.
      </div>
      <DataTable
        headers={["Phiên bản", "Ngày giờ", "Người xuất bản", "Dung lượng", "Ghi chú", "Thao tác"]}
        count={versions.length}
      >
        {versions.map((x, i) => (
          <tr key={x.v}>
            <Td>
              <b>{x.v}</b> {i === 0 && <Badge>Hiện hành</Badge>}
            </Td>
            <Td>{x.date}</Td>
            <Td>{x.user}</Td>
            <Td>{x.size}</Td>
            <Td>{i === 0 ? "Cập nhật tháng 10" : "Tự động lưu"}</Td>
            <Td>
              <Button variant="ghost" disabled={i === 0} onClick={() => setRestore(true)}>
                <RotateCcw className="size-4" />
                Khôi phục
              </Button>
            </Td>
          </tr>
        ))}
      </DataTable>
      <Dialog open={restore} onOpenChange={setRestore} title="Khôi phục phiên bản">
        <Label required>Ghi chú khôi phục</Label>
        <Textarea value={note} onChange={(e) => setNote(e.target.value)} maxLength={250} />
        <div className="mt-1 text-right text-xs text-muted-foreground">{note.length} / 250</div>
        <div className="mt-6 flex justify-end gap-2">
          <Button onClick={() => setRestore(false)}>Hủy</Button>
          <Button
            variant="primary"
            disabled={!note.trim()}
            onClick={() => {
              toast("Đã khôi phục và tạo phiên bản mới");
              setRestore(false);
            }}
          >
            Khôi phục
          </Button>
        </div>
      </Dialog>
    </>
  );
}
