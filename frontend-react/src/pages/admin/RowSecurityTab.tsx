import { useEffect, useState } from "react";
import { Plus, Shield, Trash2 } from "lucide-react";
import { Button, Input, Label, Select } from "../../components/ui";
import {
  rlsService,
  type DatasourceRlsStatusDto,
  type RowFilterDto,
} from "../../services/rls.service";
import { useUIStore } from "../../stores/ui.store";

export default function RowSecurityTab() {
  const toast = useUIStore((s) => s.toast);
  const [datasources, setDatasources] = useState<DatasourceRlsStatusDto[]>([]);
  const [policies, setPolicies] = useState<RowFilterDto[]>([]);
  const [loading, setLoading] = useState(true);

  const [datasourceId, setDatasourceId] = useState("");
  const [subjectType, setSubjectType] = useState<"USER" | "GROUP">("USER");
  const [subjectId, setSubjectId] = useState("");
  const [fieldName, setFieldName] = useState("Vung");
  const [allowedValuesText, setAllowedValuesText] = useState("Miền Bắc");
  const [saving, setSaving] = useState(false);

  useEffect(() => {
    let cancelled = false;
    Promise.all([rlsService.listDatasources(), rlsService.listPolicies()])
      .then(([dsList, policyList]) => {
        if (cancelled) return;
        setDatasources(dsList);
        setPolicies(policyList);
        if (dsList.length > 0) {
          setDatasourceId((prev) => prev || dsList[0].datasourceId);
        }
      })
      .catch(() => {
        if (cancelled) return;
        setDatasources([]);
        setPolicies([]);
      })
      .finally(() => {
        if (!cancelled) setLoading(false);
      });
    return () => {
      cancelled = true;
    };
  }, []);

  const handleToggleDatasource = async (ds: DatasourceRlsStatusDto) => {
    try {
      const updated = await rlsService.toggleDatasource(ds.datasourceId, !ds.rlsEnabled);
      setDatasources((prev) =>
        prev.map((item) => (item.datasourceId === ds.datasourceId ? updated : item)),
      );
      toast(
        `Đã ${updated.rlsEnabled ? "bật" : "tắt"} bảo mật dòng (RLS) cho "${ds.datasourceName}"`,
      );
    } catch {
      toast("Không thể cập nhật trạng thái RLS", "error");
    }
  };

  const handleSavePolicy = async () => {
    const values = allowedValuesText
      .split(",")
      .map((s) => s.trim())
      .filter(Boolean);
    if (!datasourceId || !subjectId.trim() || !fieldName.trim() || values.length === 0) {
      toast("Vui lòng nhập đầy đủ nguồn dữ liệu, UUID đối tượng, trường lọc và giá trị.", "error");
      return;
    }
    setSaving(true);
    try {
      const saved = await rlsService.savePolicy({
        datasourceId,
        subjectType,
        subjectId: subjectId.trim(),
        fieldName: fieldName.trim(),
        allowedValues: values,
      });
      setPolicies((prev) => [saved, ...prev.filter((p) => p.id !== saved.id)]);
      toast("Đã lưu quy tắc bảo mật cấp dòng (RLS)");
      setSubjectId("");
    } catch {
      toast("Lỗi khi lưu quy tắc RLS (kiểm tra lại UUID đối tượng)", "error");
    } finally {
      setSaving(false);
    }
  };

  const handleDeletePolicy = async (id: string) => {
    try {
      await rlsService.deletePolicy(id);
      setPolicies((prev) => prev.filter((p) => p.id !== id));
      toast("Đã xóa quy tắc RLS");
    } catch {
      toast("Không thể xóa quy tắc RLS", "error");
    }
  };

  return (
    <div className="space-y-6">
      <div className="rounded-xl border p-6">
        <div className="flex items-center gap-2 text-base font-semibold text-ink">
          <Shield className="size-4 text-primary" />
          Bật / tắt Bảo mật cấp dòng (RLS) theo nguồn dữ liệu
        </div>
        <p className="mt-1 text-xs text-muted-foreground">
          Khi bật RLS, mọi truy vấn xem dashboard, tương tác và xuất CSV/XLSX đều tự động lọc theo ánh xạ của người dùng/nhóm.
        </p>

        <div className="mt-4 space-y-2">
          {loading ? (
            <div className="text-sm text-muted-foreground">Đang tải danh sách nguồn dữ liệu…</div>
          ) : datasources.length === 0 ? (
            <div className="text-sm text-muted-foreground">Chưa có nguồn dữ liệu nào.</div>
          ) : (
            datasources.map((ds) => (
              <div
                key={ds.datasourceId}
                className="flex items-center justify-between rounded-lg border px-4 py-3"
              >
                <div>
                  <div className="text-sm font-medium text-ink">{ds.datasourceName}</div>
                  <div className="text-xs text-muted-foreground">{ds.datasourceId}</div>
                </div>
                <Button
                  variant={ds.rlsEnabled ? "primary" : "secondary"}
                  onClick={() => handleToggleDatasource(ds)}
                >
                  {ds.rlsEnabled ? "Đang bật RLS" : "Đang tắt RLS"}
                </Button>
              </div>
            ))
          )}
        </div>
      </div>

      <div className="rounded-xl border p-6">
        <div className="text-base font-semibold text-ink">Thêm ánh xạ lọc theo dòng</div>
        <div className="mt-4 grid gap-4 md:grid-cols-2">
          <div>
            <Label>Nguồn dữ liệu</Label>
            <Select value={datasourceId} onChange={(e) => setDatasourceId(e.target.value)}>
              {datasources.map((ds) => (
                <option key={ds.datasourceId} value={ds.datasourceId}>
                  {ds.datasourceName}
                </option>
              ))}
            </Select>
          </div>
          <div>
            <Label>Loại đối tượng</Label>
            <Select
              value={subjectType}
              onChange={(e) => setSubjectType(e.target.value as "USER" | "GROUP")}
            >
              <option value="USER">Người dùng (USER)</option>
              <option value="GROUP">Nhóm (GROUP)</option>
            </Select>
          </div>
          <div>
            <Label>UUID Người dùng / Nhóm</Label>
            <Input
              placeholder="Ví dụ: 00000000-0000-0000-0000-000000000001"
              value={subjectId}
              onChange={(e) => setSubjectId(e.target.value)}
            />
          </div>
          <div>
            <Label>Trường lọc (Field)</Label>
            <Input
              placeholder="Ví dụ: Vung"
              value={fieldName}
              onChange={(e) => setFieldName(e.target.value)}
            />
          </div>
          <div className="md:col-span-2">
            <Label>Giá trị được phép xem (phân cách bằng dấu phẩy)</Label>
            <Input
              placeholder="Ví dụ: Miền Bắc, Miền Trung"
              value={allowedValuesText}
              onChange={(e) => setAllowedValuesText(e.target.value)}
            />
          </div>
        </div>
        <div className="mt-4 flex justify-end">
          <Button variant="primary" disabled={saving} onClick={handleSavePolicy}>
            <Plus className="size-4" />
            {saving ? "Đang lưu…" : "Lưu ánh xạ RLS"}
          </Button>
        </div>
      </div>

      <div className="overflow-hidden rounded-xl border">
        <table className="w-full text-left text-sm">
          <thead className="bg-muted">
            <tr>
              <th className="p-3">Nguồn dữ liệu</th>
              <th className="p-3">Đối tượng</th>
              <th className="p-3">Trường lọc</th>
              <th className="p-3">Giá trị cho phép</th>
              <th className="p-3 text-right">Thao tác</th>
            </tr>
          </thead>
          <tbody>
            {policies.length === 0 ? (
              <tr>
                <td colSpan={5} className="p-4 text-center text-muted-foreground">
                  Chưa có ánh xạ bảo mật dòng nào.
                </td>
              </tr>
            ) : (
              policies.map((p) => (
                <tr key={p.id} className="border-t">
                  <td className="p-3 font-medium">{p.datasourceName}</td>
                  <td className="p-3">
                    {p.subjectType} · {p.subjectName}
                  </td>
                  <td className="p-3 font-mono text-xs">{p.fieldName}</td>
                  <td className="p-3">{p.allowedValues.join(", ")}</td>
                  <td className="p-3 text-right">
                    <button
                      type="button"
                      onClick={() => handleDeletePolicy(p.id)}
                      className="rounded p-1 text-muted-foreground hover:text-primary"
                      title="Xóa quy tắc RLS"
                    >
                      <Trash2 className="size-4" />
                    </button>
                  </td>
                </tr>
              ))
            )}
          </tbody>
        </table>
      </div>
    </div>
  );
}
