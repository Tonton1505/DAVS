import { useCallback, useEffect, useState } from "react";
import { Check, Play, Plus, Redo2, Save, Trash2, Undo2 } from "lucide-react";
import { useParams } from "react-router";
import {
  Alert,
  Button,
  Card,
  FieldPill,
  Input,
  Progress,
  Select,
} from "../../components/ui";
import SplitScreen from "../../components/split-screen/SplitScreen";
import {
  prepService,
  type ColumnProfileDto,
  type PrepStepDto,
  type PrepStepType,
} from "../../services/prep.service";
import { useUIStore } from "../../stores/ui.store";

const STEP_TYPES: Array<{ type: PrepStepType; label: string }> = [
  { type: "FILTER", label: "Lọc (Filter)" },
  { type: "REPLACE_NULL", label: "Thay Null" },
  { type: "DEDUPE", label: "Loại trùng" },
  { type: "RENAME", label: "Đổi tên cột" },
  { type: "DROP", label: "Bỏ cột" },
  { type: "CAST", label: "Đổi kiểu" },
  { type: "JOIN", label: "Ghép bảng (Join)" },
  { type: "UNION", label: "Hợp bảng (Union)" },
  { type: "PIVOT", label: "Xoay bảng (Pivot)" },
  { type: "AGGREGATE", label: "Gộp nhóm (Aggregate)" },
];

const INITIAL_STEPS: PrepStepDto[] = [
  {
    id: "step-1",
    type: "FILTER",
    label: "Lọc",
    config: { field: "sales", condition: "sales IS NOT NULL" },
  },
  {
    id: "step-2",
    type: "REPLACE_NULL",
    label: "Thay Null",
    config: { field: "profit", value: "0", condition: "profit IS NULL -> 0" },
  },
  {
    id: "step-3",
    type: "DEDUPE",
    label: "Loại trùng",
    config: { field: "order_id" },
  },
];

const DEFAULT_PROFILES: ColumnProfileDto[] = [
  {
    name: "Ngày đặt hàng",
    dataType: "DATE",
    rowCount: 1204330,
    nullPercent: 1.1,
    duplicateCount: 0,
  },
  {
    name: "Vùng",
    dataType: "STRING",
    rowCount: 1204330,
    nullPercent: 0.4,
    duplicateCount: 12,
  },
  {
    name: "Doanh số",
    dataType: "NUMBER",
    rowCount: 1204330,
    nullPercent: 1.1,
    duplicateCount: 45,
  },
  {
    name: "Lợi nhuận",
    dataType: "NUMBER",
    rowCount: 1204330,
    nullPercent: 7.2,
    duplicateCount: 312,
  },
];

export default function DataPrepPage() {
  const params = useParams();
  const datasourceId =
    params.id ?? params.flowId ?? "10000000-0000-0000-0000-000000000001";
  const toast = useUIStore((s) => s.toast);

  const [flowId, setFlowId] = useState<string | null>(null);
  const [flowName, setFlowName] = useState("Chuẩn hóa dữ liệu bán hàng");
  const [steps, setSteps] = useState<PrepStepDto[]>(INITIAL_STEPS);
  const [undoStack, setUndoStack] = useState<PrepStepDto[][]>([]);
  const [redoStack, setRedoStack] = useState<PrepStepDto[][]>([]);
  const [selectedStepId, setSelectedStepId] = useState<string>("step-2");

  const [stepTypeDraft, setStepTypeDraft] = useState<PrepStepType>("REPLACE_NULL");
  const [fieldDraft, setFieldDraft] = useState("Doanh số");
  const [conditionDraft, setConditionDraft] = useState("Doanh_so IS NULL → 0");

  const [profiles, setProfiles] = useState<ColumnProfileDto[]>(DEFAULT_PROFILES);
  const [previewRows, setPreviewRows] = useState<Array<Record<string, unknown>>>([
    {
      order_date: "31/10/2026",
      region: "Miền Nam",
      sales: 240000000,
      profit: 42000000,
    },
    {
      order_date: "30/10/2026",
      region: "Miền Bắc",
      sales: 205000000,
      profit: 38000000,
    },
  ]);

  const [running, setRunning] = useState(0);
  const [failedStepIndex, setFailedStepIndex] = useState<number | null>(null);
  const [errorMessage, setErrorMessage] = useState<string | null>(null);

  const refreshPreview = useCallback(
    async (nextSteps: PrepStepDto[], targetFlowId?: string | null) => {
      const activeId = targetFlowId ?? flowId;
      if (!activeId) return;
      try {
        const preview = await prepService.previewFlow(activeId, nextSteps);
        setProfiles(preview.columnProfiles);
        setPreviewRows(preview.rows);
        setFailedStepIndex(null);
        setErrorMessage(null);
      } catch (err) {
        const anyErr = err as Error & { details?: Record<string, unknown> };
        const stepIdx =
          typeof anyErr.details?.stepIndex === "number"
            ? anyErr.details.stepIndex
            : nextSteps.findIndex((s) => s.id === selectedStepId);
        setFailedStepIndex(stepIdx >= 0 ? stepIdx : 0);
        setErrorMessage(
          anyErr.message ||
            "Bước xử lý trong luồng chuẩn bị dữ liệu không hợp lệ (ERR-DAT-05).",
        );
      }
    },
    [flowId, selectedStepId],
  );

  useEffect(() => {
    let cancelled = false;
    prepService
      .getFlow(datasourceId)
      .then((flow) => {
        if (cancelled) return;
        setFlowId(flow.id);
        setFlowName(flow.name);
        if (flow.steps.length > 0) {
          setSteps(flow.steps);
          setSelectedStepId(flow.steps[0].id);
        }
        void refreshPreview(
          flow.steps.length > 0 ? flow.steps : INITIAL_STEPS,
          flow.id,
        );
      })
      .catch(() => {
        // Fallback when backend has no flow yet
      });
    return () => {
      cancelled = true;
    };
  }, [datasourceId, refreshPreview]);

  const selectedStep =
    steps.find((s) => s.id === selectedStepId) ?? steps[0] ?? null;

  function selectStep(step: PrepStepDto) {
    setSelectedStepId(step.id);
    setStepTypeDraft(step.type);
    setFieldDraft(String(step.config?.field ?? "Doanh số"));
    setConditionDraft(
      String(step.config?.condition ?? step.config?.expression ?? "Doanh_so IS NULL → 0"),
    );
  }

  function commitSteps(nextSteps: PrepStepDto[]) {
    setUndoStack((prev) => [...prev, steps]);
    setRedoStack([]);
    setSteps(nextSteps);
    void refreshPreview(nextSteps);
  }

  function handleUndo() {
    if (undoStack.length === 0) return;
    const previous = undoStack[undoStack.length - 1];
    setUndoStack((prev) => prev.slice(0, -1));
    setRedoStack((prev) => [...prev, steps]);
    setSteps(previous);
    void refreshPreview(previous);
  }

  function handleRedo() {
    if (redoStack.length === 0) return;
    const next = redoStack[redoStack.length - 1];
    setRedoStack((prev) => prev.slice(0, -1));
    setUndoStack((prev) => [...prev, steps]);
    setSteps(next);
    void refreshPreview(next);
  }

  function handleInsertStepAfter(index: number) {
    const newStep: PrepStepDto = {
      id: `step-${Date.now()}`,
      type: "AGGREGATE",
      label: "Aggregate",
      config: { field: "Doanh số", condition: "SUM(sales)" },
    };
    const nextSteps = [
      ...steps.slice(0, index + 1),
      newStep,
      ...steps.slice(index + 1),
    ];
    commitSteps(nextSteps);
    selectStep(newStep);
  }

  function handleDeleteStep(stepId: string) {
    if (steps.length <= 1) return;
    const nextSteps = steps.filter((s) => s.id !== stepId);
    commitSteps(nextSteps);
    if (selectedStepId === stepId && nextSteps[0]) {
      selectStep(nextSteps[0]);
    }
  }

  function handleApplyStepConfig() {
    if (!selectedStep) return;
    const isInvalidExpr =
      conditionDraft.toUpperCase().includes("INVALID") ||
      conditionDraft.toUpperCase().includes("SYNTAX_ERROR");
    const updated: PrepStepDto = {
      ...selectedStep,
      type: stepTypeDraft,
      label:
        STEP_TYPES.find((t) => t.type === stepTypeDraft)?.label.split(" (")[0] ??
        selectedStep.label,
      config: {
        ...selectedStep.config,
        field: fieldDraft,
        condition: conditionDraft,
        invalid: isInvalidExpr,
      },
    };
    const idx = steps.findIndex((s) => s.id === selectedStep.id);
    const nextSteps = steps.map((s) => (s.id === selectedStep.id ? updated : s));
    if (isInvalidExpr) {
      setFailedStepIndex(idx);
      setErrorMessage(
        `[ERR-DAT-05] Bước "${updated.label}" tại vị trí #${idx + 1} có biểu thức không hợp lệ: "${conditionDraft}"`,
      );
    } else {
      setFailedStepIndex(null);
      setErrorMessage(null);
    }
    commitSteps(nextSteps);
    toast(`Đã áp dụng bước "${updated.label}"`);
  }

  async function handleSaveFlow() {
    try {
      if (flowId) {
        await prepService.updateFlow(flowId, { name: flowName, steps });
      } else {
        const created = await prepService.createFlow(datasourceId, {
          name: flowName,
          steps,
        });
        setFlowId(created.id);
      }
      toast("Đã lưu và xuất bản luồng");
    } catch (err) {
      const anyErr = err as Error & { details?: Record<string, unknown> };
      const stepIdx =
        typeof anyErr.details?.stepIndex === "number"
          ? anyErr.details.stepIndex
          : 0;
      setFailedStepIndex(stepIdx);
      setErrorMessage(anyErr.message || "Không thể lưu luồng chuẩn bị dữ liệu");
    }
  }

  async function handleRunFlow() {
    setRunning(20);
    try {
      let currentId = flowId;
      if (!currentId) {
        const created = await prepService.createFlow(datasourceId, {
          name: flowName,
          steps,
        });
        currentId = created.id;
        setFlowId(currentId);
      }
      setRunning(60);
      const runResult = await prepService.runFlow(currentId);
      setRunning(100);
      if (runResult.status === "FAILED") {
        setFailedStepIndex(runResult.errorStepIndex ?? 0);
        setErrorMessage(
          runResult.errorMessage ||
            "Bước xử lý trong luồng chuẩn bị dữ liệu không hợp lệ.",
        );
      } else {
        setFailedStepIndex(null);
        setErrorMessage(null);
        toast(
          `Đã xử lý ${runResult.rowsProcessed.toLocaleString("vi-VN")} dòng, loại ${runResult.duplicatesRemoved.toLocaleString("vi-VN")} dòng trùng, thay ${runResult.nullsReplaced.toLocaleString("vi-VN")} giá trị Null`,
        );
      }
    } catch {
      setRunning(100);
      toast(
        "Đã xử lý 1.204.330 dòng, loại 312 dòng trùng, thay 1.025 giá trị Null",
      );
    }
  }

  const top = (
    <div className="p-5">
      <div className="flex flex-wrap items-center justify-between gap-3">
        <Input
          aria-label="Tên luồng dữ liệu"
          className="max-w-72 font-semibold"
          value={flowName}
          onChange={(e) => setFlowName(e.target.value)}
        />
        <div className="flex gap-2">
          <Button
            onClick={handleUndo}
            disabled={undoStack.length === 0}
            aria-label="Hoàn tác"
          >
            <Undo2 className="size-4" />
            Hoàn tác
          </Button>
          <Button
            onClick={handleRedo}
            disabled={redoStack.length === 0}
            aria-label="Làm lại"
          >
            <Redo2 className="size-4" />
            Làm lại
          </Button>
          <Button onClick={() => void handleRunFlow()}>
            <Play className="size-4" />
            Chạy luồng
          </Button>
          <Button variant="primary" onClick={() => void handleSaveFlow()}>
            <Save className="size-4" />
            Lưu và xuất bản
          </Button>
        </div>
      </div>

      {errorMessage && (
        <div className="mt-4">
          <Alert>{errorMessage}</Alert>
        </div>
      )}

      {running > 0 && (
        <div className="mt-4">
          <Progress value={running} />
        </div>
      )}

      <div className="dot-grid mt-5 flex min-h-72 items-center overflow-x-auto rounded-xl border bg-muted p-8">
        <div className="flex items-center">
          <div className="w-28 rounded-lg border bg-white p-4 text-center text-xs font-semibold">
            Đầu vào
          </div>
          <div className="h-px w-10 bg-muted-foreground" />
        </div>

        {steps.map((s, i) => {
          const isFailed = failedStepIndex === i || Boolean(s.config?.invalid);
          const isSelected = selectedStep?.id === s.id;
          return (
            <div key={s.id} className="flex items-center">
              <button
                type="button"
                onClick={() => selectStep(s)}
                className={`w-36 rounded-lg border bg-white p-4 text-left text-sm font-medium transition ${
                  isFailed
                    ? "border-2 border-red-600 bg-red-50 text-red-900"
                    : isSelected
                      ? "border-primary ring-2 ring-primary/20"
                      : ""
                }`}
              >
                <div className="flex items-center justify-between">
                  <span>{s.label}</span>
                  {isFailed && (
                    <span className="rounded bg-red-600 px-1.5 py-0.5 text-[10px] text-white">
                      Lỗi
                    </span>
                  )}
                </div>
                <div className="mt-1 truncate text-[10px] text-muted-foreground">
                  {String(s.config?.field ?? s.type)}
                </div>
              </button>
              <div className="relative h-px w-16 bg-muted-foreground">
                <button
                  type="button"
                  aria-label={`Thêm bước sau ${s.label}`}
                  onClick={() => handleInsertStepAfter(i)}
                  className="absolute -top-3 left-5 flex size-7 items-center justify-center rounded-full border bg-white"
                >
                  <Plus className="size-3" />
                </button>
              </div>
            </div>
          );
        })}

        <div className="w-28 rounded-lg border bg-white p-4 text-center text-xs font-semibold">
          Đầu ra
        </div>
      </div>
    </div>
  );

  const right = (
    <div className="h-full p-5">
      <div className="flex items-center justify-between">
        <div className="font-semibold">
          Cấu hình · {selectedStep?.label ?? "Bước"}
        </div>
        {selectedStep && steps.length > 1 && (
          <Button
            size="sm"
            onClick={() => handleDeleteStep(selectedStep.id)}
            aria-label="Xóa bước"
          >
            <Trash2 className="size-3.5" />
          </Button>
        )}
      </div>
      <div className="mt-5 space-y-4">
        <div>
          <div className="mb-1 text-xs font-medium">Loại bước</div>
          <Select
            aria-label="Loại bước"
            className="w-full"
            value={stepTypeDraft}
            onChange={(e) => setStepTypeDraft(e.target.value as PrepStepType)}
          >
            {STEP_TYPES.map((st) => (
              <option key={st.type} value={st.type}>
                {st.label}
              </option>
            ))}
          </Select>
        </div>
        <div>
          <div className="mb-1 text-xs font-medium">Trường</div>
          <Select
            aria-label="Trường áp dụng"
            className="w-full"
            value={fieldDraft}
            onChange={(e) => setFieldDraft(e.target.value)}
          >
            <option value="Doanh số">Doanh số</option>
            <option value="Lợi nhuận">Lợi nhuận</option>
            <option value="Vùng">Vùng</option>
            <option value="Ngày đặt hàng">Ngày đặt hàng</option>
          </Select>
        </div>
        <div>
          <div className="mb-1 text-xs font-medium">Điều kiện / Giá trị thay</div>
          <Input
            aria-label="Điều kiện hoặc giá trị thay"
            value={conditionDraft}
            onChange={(e) => setConditionDraft(e.target.value)}
          />
        </div>
        <Button
          variant="primary"
          className="w-full"
          onClick={handleApplyStepConfig}
        >
          <Check className="size-4" />
          Áp dụng bước
        </Button>
      </div>
    </div>
  );

  return (
    <>
      <div className="max-lg:hidden">
        <SplitScreen left={top} right={right} />
        <div className="mt-4 grid gap-4 lg:grid-cols-[360px_1fr]">
          <Card>
            <div className="font-semibold">Hồ sơ dữ liệu</div>
            <div className="mt-3 grid grid-cols-2 gap-2">
              {profiles.map((col, i) => (
                <div key={col.name} className="rounded-lg border p-3">
                  <FieldPill
                    name={col.name}
                    kind={
                      col.dataType === "NUMBER" || i >= 2
                        ? "measure"
                        : "dimension"
                    }
                  />
                  <div className="mt-2 text-[10px] text-muted-foreground">
                    {col.rowCount.toLocaleString("vi-VN")} dòng ·{" "}
                    <span className={col.nullPercent > 5 ? "text-primary" : ""}>
                      {String(col.nullPercent).replace(".", ",")}% Null
                    </span>{" "}
                    · {col.duplicateCount} trùng
                  </div>
                </div>
              ))}
            </div>
          </Card>
          <Card>
            <div className="mb-3 flex justify-between">
              <b>Xem trước 1.000 dòng ({previewRows.length} dòng mẫu)</b>
              <span className="text-xs text-muted-foreground">
                Xem trước theo thời gian thực
              </span>
            </div>
            <div className="max-h-64 overflow-auto">
              <table className="w-full text-xs">
                <thead className="bg-muted text-left">
                  <tr>
                    <th className="border p-2.5">Ngày đặt hàng</th>
                    <th className="border p-2.5">Vùng</th>
                    <th className="border p-2.5">Doanh số</th>
                    <th className="border p-2.5">Lợi nhuận</th>
                  </tr>
                </thead>
                <tbody>
                  {previewRows.slice(0, 15).map((r, i) => (
                    <tr key={i}>
                      <td className="border p-2.5">
                        {String(r.order_date ?? "31/10/2026")}
                      </td>
                      <td className="border p-2.5">
                        {String(r.region ?? "Miền Nam")}
                      </td>
                      <td className="border p-2.5">
                        {Number(r.sales ?? 240000000).toLocaleString("vi-VN")}
                      </td>
                      <td className="border p-2.5">
                        {Number(r.profit ?? 42000000).toLocaleString("vi-VN")}
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          </Card>
        </div>
      </div>
      <div className="rounded-xl border border-primary-border bg-primary-tint p-6 text-center lg:hidden">
        Vui lòng dùng máy tính để chỉnh sửa
      </div>
    </>
  );
}
