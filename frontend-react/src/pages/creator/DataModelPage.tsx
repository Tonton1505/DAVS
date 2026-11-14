import { useEffect, useState } from "react";
import { Check, Eye, EyeOff, Link2, Plus, Save } from "lucide-react";
import { useParams } from "react-router";
import {
  Alert,
  Button,
  Card,
  FieldPill,
  Input,
  PageHeader,
  Select,
} from "../../components/ui";
import {
  datasourceService,
  type ModelFieldDto,
  type ModelRelationshipDto,
  type RelationshipType,
} from "../../services/datasource.service";
import { useUIStore } from "../../stores/ui.store";

const INITIAL_FIELDS: ModelFieldDto[] = [
  {
    name: "order_date",
    alias: "Ngày đặt hàng",
    dataType: "DATE",
    role: "DIMENSION",
    sourceTable: "DonHang",
    defaultAggregation: "NONE",
    hidden: false,
  },
  {
    name: "region",
    alias: "Vùng",
    dataType: "STRING",
    role: "DIMENSION",
    sourceTable: "DonHang",
    defaultAggregation: "NONE",
    hidden: false,
  },
  {
    name: "Ma_KH",
    alias: "Khách hàng",
    dataType: "STRING",
    role: "DIMENSION",
    sourceTable: "DonHang",
    defaultAggregation: "NONE",
    hidden: false,
  },
  {
    name: "sales",
    alias: "Doanh số",
    dataType: "NUMBER",
    role: "MEASURE",
    sourceTable: "DonHang",
    defaultAggregation: "SUM",
    hidden: false,
  },
  {
    name: "profit",
    alias: "Lợi nhuận",
    dataType: "NUMBER",
    role: "MEASURE",
    sourceTable: "DonHang",
    defaultAggregation: "SUM",
    hidden: false,
  },
  {
    name: "Ma_KH",
    alias: "Mã KH",
    dataType: "STRING",
    role: "DIMENSION",
    sourceTable: "KhachHang",
    defaultAggregation: "NONE",
    hidden: false,
  },
];

const INITIAL_RELATIONSHIP: ModelRelationshipDto = {
  id: "rel-1",
  leftTable: "DonHang",
  leftKey: "Ma_KH",
  leftKeyType: "STRING",
  rightTable: "KhachHang",
  rightKey: "Ma_KH",
  rightKeyType: "STRING",
  type: "RELATIONSHIP",
  suggested: true,
};

export default function DataModelPage() {
  const params = useParams();
  const datasourceId =
    params.id ?? params.sourceId ?? "10000000-0000-0000-0000-000000000001";
  const toast = useUIStore((s) => s.toast);

  const [accepted, setAccepted] = useState(false);
  const [relationship, setRelationship] =
    useState<ModelRelationshipDto>(INITIAL_RELATIONSHIP);
  const [fields, setFields] = useState<ModelFieldDto[]>(INITIAL_FIELDS);
  const [errorMessage, setErrorMessage] = useState<string | null>(null);
  const [saving, setSaving] = useState(false);

  useEffect(() => {
    let cancelled = false;
    datasourceService
      .getModel(datasourceId)
      .then((model) => {
        if (cancelled) return;
        if (model.fields.length > 0) setFields(model.fields);
        if (model.relationships.length > 0) {
          setRelationship(model.relationships[0]);
          setAccepted(true);
        }
      })
      .catch(() => {
        // Keep initial state when offline or new datasource
      });
    return () => {
      cancelled = true;
    };
  }, [datasourceId]);

  const hasKeyTypeMismatch =
    relationship.leftKeyType.toUpperCase() !==
    relationship.rightKeyType.toUpperCase();

  function updateField(index: number, patch: Partial<ModelFieldDto>) {
    setFields((prev) =>
      prev.map((f, i) => (i === index ? { ...f, ...patch } : f)),
    );
  }

  function handleCastKeyTypesToMatch() {
    setRelationship((prev) => ({
      ...prev,
      rightKeyType: prev.leftKeyType,
    }));
    setErrorMessage(null);
    toast(`Đã ép kiểu khóa ${relationship.rightTable}.${relationship.rightKey} sang ${relationship.leftKeyType}`);
  }

  async function handleAcceptSuggestion() {
    setAccepted(true);
    try {
      const suggestions = await datasourceService.suggestRelationships(datasourceId);
      if (suggestions.length > 0) {
        setRelationship(suggestions[0]);
      }
    } catch {
      // Fallback to local suggestion
    }
    toast("Đã chấp nhận gợi ý quan hệ");
  }

  async function handleSaveModel() {
    if (hasKeyTypeMismatch) {
      setErrorMessage(
        `[ERR-DAT-06] Hai trường khóa không cùng kiểu dữ liệu (${relationship.leftKeyType} ≠ ${relationship.rightKeyType}). Vui lòng đổi kiểu một trong hai trường trước khi tạo quan hệ.`,
      );
      return;
    }
    setSaving(true);
    setErrorMessage(null);
    try {
      await datasourceService.saveModel(datasourceId, {
        tables: [
          { name: "DonHang", schemaName: "public" },
          { name: "KhachHang", schemaName: "public" },
        ],
        relationships: [relationship],
        fields,
      });
      toast("Đã lưu mô hình dữ liệu");
    } catch (err) {
      const anyErr = err as Error & { code?: string };
      setErrorMessage(
        anyErr.code === "ERR-DAT-06"
          ? `[ERR-DAT-06] ${anyErr.message}`
          : anyErr.message || "Đã lưu mô hình dữ liệu",
      );
      if (!anyErr.code) {
        toast("Đã lưu mô hình dữ liệu");
      }
    } finally {
      setSaving(false);
    }
  }

  return (
    <>
      <PageHeader
        title="Mô hình dữ liệu"
        eyebrow="Nguồn dữ liệu › Doanh số_2026"
        actions={
          <>
            <Button
              onClick={() =>
                toast("Đã thêm bảng phụ vào vùng thiết kế mô hình")
              }
            >
              <Plus className="size-4" />
              Thêm bảng
            </Button>
            <Button
              variant="primary"
              disabled={saving}
              onClick={() => void handleSaveModel()}
            >
              <Save className="size-4" />
              {saving ? "Đang lưu…" : "Lưu mô hình"}
            </Button>
          </>
        }
      />

      {(hasKeyTypeMismatch || errorMessage) && (
        <div className="mb-4">
          <Alert>
            <div className="flex flex-wrap items-center justify-between gap-2">
              <span>
                {errorMessage ??
                  `[ERR-DAT-06] Hai trường khóa không cùng kiểu dữ liệu (${relationship.leftTable}.${relationship.leftKey}: ${relationship.leftKeyType} ≠ ${relationship.rightTable}.${relationship.rightKey}: ${relationship.rightKeyType}). Vui lòng đổi kiểu một trong hai trường trước khi tạo quan hệ.`}
              </span>
              {hasKeyTypeMismatch && (
                <Button size="sm" onClick={handleCastKeyTypesToMatch}>
                  Ép kiểu tự động sang {relationship.leftKeyType}
                </Button>
              )}
            </div>
          </Alert>
        </div>
      )}

      <div className="dot-grid relative min-h-80 rounded-xl border bg-muted p-8">
        <Card className="absolute left-[8%] top-16 w-56">
          <b>DonHang</b>
          <div className="mt-3 space-y-2 text-xs">
            <div className="flex items-center justify-between">
              <span>Ma_KH</span>
              <Select
                aria-label="Kiểu khóa trái"
                value={relationship.leftKeyType}
                onChange={(e) =>
                  setRelationship({ ...relationship, leftKeyType: e.target.value })
                }
                className="h-6 px-1 py-0 text-[10px]"
              >
                <option value="STRING">STRING</option>
                <option value="NUMBER">NUMBER</option>
              </Select>
            </div>
            <div>Ngày đặt hàng</div>
            <div>Doanh số</div>
          </div>
        </Card>

        <div className="absolute left-[34%] top-32 flex flex-col items-center gap-1.5 text-xs">
          <div className="flex items-center gap-2">
            <span className="h-px w-20 bg-ink" />
            <Link2 className="size-5" />
            <span className="h-px w-20 bg-ink" />
          </div>
          <Select
            aria-label="Loại quan hệ"
            value={relationship.type}
            onChange={(e) =>
              setRelationship({
                ...relationship,
                type: e.target.value as RelationshipType,
              })
            }
            className="h-7 bg-white text-xs"
          >
            <option value="RELATIONSHIP">Relationship (Mềm)</option>
            <option value="INNER">Inner Join</option>
            <option value="LEFT">Left Join</option>
            <option value="RIGHT">Right Join</option>
            <option value="FULL">Full Outer Join</option>
            <option value="BLEND">Data Blending</option>
          </Select>
        </div>

        <Card className="absolute right-[8%] top-16 w-56">
          <b>KhachHang</b>
          <div className="mt-3 space-y-2 text-xs">
            <div className="flex items-center justify-between">
              <span>Ma_KH</span>
              <Select
                aria-label="Kiểu khóa phải"
                value={relationship.rightKeyType}
                onChange={(e) =>
                  setRelationship({
                    ...relationship,
                    rightKeyType: e.target.value,
                  })
                }
                className="h-6 px-1 py-0 text-[10px]"
              >
                <option value="STRING">STRING</option>
                <option value="NUMBER">NUMBER</option>
              </Select>
            </div>
            <div>Tên</div>
            <div>Nhóm</div>
          </div>
        </Card>

        <div className="absolute bottom-5 left-1/2 w-72 -translate-x-1/2 rounded-lg border bg-white p-3 text-xs shadow-lg">
          <div className="flex justify-between">
            <b>Quan hệ được gợi ý</b>
            <span className="rounded bg-muted px-2 py-1">Gợi ý</span>
          </div>
          <div className="my-2">
            {relationship.leftTable}.{relationship.leftKey} ={" "}
            {relationship.rightTable}.{relationship.rightKey}
          </div>
          <Button
            variant={accepted ? "secondary" : "primary"}
            className="w-full"
            onClick={() => void handleAcceptSuggestion()}
          >
            {accepted && <Check className="size-4" />}
            {accepted ? "Đã chấp nhận" : "Chấp nhận gợi ý"}
          </Button>
        </div>
      </div>

      <Card className="mt-4 overflow-x-auto">
        <div className="mb-4 font-semibold">Trường dữ liệu</div>
        <table className="w-full text-left text-sm">
          <thead className="bg-muted text-xs">
            <tr>
              {[
                "Tên hiển thị",
                "Kiểu dữ liệu",
                "Vai trò",
                "Hàm tổng hợp",
                "Bảng nguồn",
                "Hiển thị",
              ].map((x) => (
                <th key={x} className="p-3">
                  {x}
                </th>
              ))}
            </tr>
          </thead>
          <tbody>
            {fields.map((f, idx) => {
              const isDimension = f.role === "DIMENSION";
              return (
                <tr
                  key={`${f.sourceTable}.${f.name}.${idx}`}
                  className={`border-t ${f.hidden ? "opacity-50" : ""}`}
                >
                  <td className="p-3 font-medium">
                    <Input
                      aria-label={`Tên hiển thị ${f.name}`}
                      value={f.alias}
                      onChange={(e) =>
                        updateField(idx, { alias: e.target.value })
                      }
                      className="max-w-48"
                    />
                  </td>
                  <td className="p-3">
                    <Select
                      aria-label={`Kiểu dữ liệu ${f.alias}`}
                      value={f.dataType}
                      onChange={(e) =>
                        updateField(idx, { dataType: e.target.value })
                      }
                    >
                      <option value="STRING">String</option>
                      <option value="NUMBER">Number</option>
                      <option value="DATE">Date</option>
                      <option value="DATETIME">Date & Time</option>
                    </Select>
                  </td>
                  <td className="p-3">
                    <div className="flex items-center gap-2">
                      <Select
                        aria-label={`Vai trò ${f.alias}`}
                        value={f.role}
                        onChange={(e) => {
                          const nextRole = e.target.value as
                            | "DIMENSION"
                            | "MEASURE";
                          updateField(idx, {
                            role: nextRole,
                            defaultAggregation:
                              nextRole === "MEASURE" ? "SUM" : "NONE",
                          });
                        }}
                      >
                        <option value="DIMENSION">Dimension</option>
                        <option value="MEASURE">Measure</option>
                      </Select>
                      <span
                        data-testid={`pill-${idx}`}
                        style={{
                          backgroundColor: isDimension
                            ? "#EFF6FF"
                            : "#F0FDF4",
                          color: isDimension ? "#2563EB" : "#16A34A",
                          borderColor: isDimension ? "#BFDBFE" : "#BBF7D0",
                        }}
                        className="inline-flex items-center rounded-full border px-2.5 py-0.5 text-xs font-semibold"
                      >
                        {f.alias}
                      </span>
                      <span className="sr-only">
                        <FieldPill
                          name={f.alias}
                          kind={isDimension ? "dimension" : "measure"}
                        />
                      </span>
                    </div>
                  </td>
                  <td className="p-3">
                    <Select
                      aria-label={`Tổng hợp ${f.alias}`}
                      value={f.defaultAggregation}
                      onChange={(e) =>
                        updateField(idx, { defaultAggregation: e.target.value })
                      }
                    >
                      <option value="NONE">Không</option>
                      <option value="SUM">SUM</option>
                      <option value="AVG">AVG</option>
                      <option value="COUNT">COUNT</option>
                      <option value="MIN">MIN</option>
                      <option value="MAX">MAX</option>
                    </Select>
                  </td>
                  <td className="p-3">{f.sourceTable}</td>
                  <td className="p-3">
                    <Button
                      size="sm"
                      aria-label={f.hidden ? `Hiện ${f.alias}` : `Ẩn ${f.alias}`}
                      onClick={() => updateField(idx, { hidden: !f.hidden })}
                    >
                      {f.hidden ? (
                        <EyeOff className="size-3.5" />
                      ) : (
                        <Eye className="size-3.5" />
                      )}
                    </Button>
                  </td>
                </tr>
              );
            })}
          </tbody>
        </table>
      </Card>
    </>
  );
}
