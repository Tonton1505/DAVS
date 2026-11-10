import { useState } from "react";
import { CheckCircle2 } from "lucide-react";
import {
  Button,
  Dialog,
  Input,
  Label,
  Tabs,
  TabsContent,
  TabsList,
  TabsTrigger,
  Textarea,
} from "../../components/ui";
import { MESSAGES } from "../../common/constants/messages";
import { useUIStore } from "../../stores/ui.store";
export default function CalculatedFieldDialog({
  open,
  onOpenChange,
}: {
  open: boolean;
  onOpenChange: (v: boolean) => void;
}) {
  const [name, setName] = useState("Tỷ suất lợi nhuận"),
    [formula, setFormula] = useState("SUM([Doanh số]) /"),
    toast = useUIStore((s) => s.toast),
    valid = formula.trim().endsWith("])") || formula.includes("/ SUM");
  return (
    <Dialog
      open={open}
      onOpenChange={onOpenChange}
      title="Tạo trường tính toán"
      className="max-w-3xl"
    >
      <Tabs defaultValue="field">
        <TabsList>
          <TabsTrigger value="field">Trường tính toán</TabsTrigger>
          <TabsTrigger value="parameter">Tham số</TabsTrigger>
        </TabsList>
        <TabsContent value="field" className="pt-5">
          <div>
            <Label required>Tên trường</Label>
            <Input value={name} onChange={(e) => setName(e.target.value)} />
          </div>
          <div className="mt-4 grid gap-4 md:grid-cols-[1fr_190px]">
            <div>
              <Label required>Công thức</Label>
              <Textarea
                className="min-h-48 font-mono"
                value={formula}
                onChange={(e) => setFormula(e.target.value)}
                maxLength={8000}
              />
              <div className="mt-1 text-right text-xs text-muted-foreground">
                {formula.length} / 8.000
              </div>
            </div>
            <div className="rounded-lg border bg-muted p-3">
              <div className="mb-2 text-xs font-semibold">Hàm</div>
              {["SUM", "AVG", "COUNT", "IF / ELSE", "DATE", "{ FIXED … }"].map((x) => (
                <button
                  key={x}
                  className="block w-full rounded px-2 py-1.5 text-left text-xs hover:bg-white"
                >
                  {x}
                </button>
              ))}
            </div>
          </div>
          <div
            className={`mt-3 rounded-lg border p-3 text-sm ${valid ? "bg-muted" : "border-primary-border bg-primary-tint text-primary"}`}
          >
            {valid ? (
              <span className="flex items-center gap-2">
                <CheckCircle2 className="size-4" />
                Công thức hợp lệ
              </span>
            ) : (
              MESSAGES.formula
            )}
          </div>
        </TabsContent>
        <TabsContent value="parameter" className="space-y-4 pt-5">
          <div>
            <Label>Tên tham số</Label>
            <Input defaultValue="Top N" />
          </div>
          <div className="grid grid-cols-2 gap-3">
            <select className="h-10 rounded-lg border px-3">
              <option>Integer</option>
              <option>Float</option>
              <option>String</option>
            </select>
            <select className="h-10 rounded-lg border px-3">
              <option>Phạm vi</option>
              <option>Danh sách</option>
              <option>Tất cả</option>
            </select>
          </div>
          <div className="grid grid-cols-3 gap-3">
            <Input defaultValue="1" />
            <Input defaultValue="50" />
            <Input defaultValue="10" />
          </div>
        </TabsContent>
      </Tabs>
      <div className="mt-6 flex justify-end gap-2">
        <Button onClick={() => onOpenChange(false)}>Hủy</Button>
        <Button
          variant="primary"
          disabled={!valid}
          onClick={() => {
            toast("Đã tạo trường tính toán");
            onOpenChange(false);
          }}
        >
          Lưu
        </Button>
      </div>
    </Dialog>
  );
}
