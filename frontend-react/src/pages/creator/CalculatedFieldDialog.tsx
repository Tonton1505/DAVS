import { useEffect, useMemo, useState } from "react";
import { AlertCircle, CheckCircle2 } from "lucide-react";
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
import { vizService } from "../../services/viz.service";
import { useUIStore } from "../../stores/ui.store";

interface CalculatedFieldDialogProps {
  open: boolean;
  onOpenChange: (v: boolean) => void;
  datasourceId?: string;
  workbookId?: string;
  onSuccess?: () => void;
}

export default function CalculatedFieldDialog({
  open,
  onOpenChange,
  datasourceId = "ds-sales-demo",
  workbookId = "wb-default",
  onSuccess,
}: CalculatedFieldDialogProps) {
  const [activeTab, setActiveTab] = useState<"field" | "parameter">("field");
  const toast = useUIStore((s) => s.toast);

  // Calculated Field states
  const [fieldName, setFieldName] = useState("Tỷ suất lợi nhuận");
  const [formula, setFormula] = useState("SUM([Doanh số]) / SUM([Lợi nhuận])");
  const [returnType, setReturnType] = useState("FLOAT");
  const isFormulaEmpty = !formula.trim();
  const [asyncFieldValid, setAsyncFieldValid] = useState(true);
  const [asyncFieldErrorMessage, setAsyncFieldErrorMessage] = useState<string | null>(null);
  const [errorPos, setErrorPos] = useState<number | null>(null);
  const [validating, setValidating] = useState(false);

  // Derived field validation state
  const fieldValid = !isFormulaEmpty && asyncFieldValid;
  const fieldErrorMessage = isFormulaEmpty
    ? "ERR-VIZ-01: Công thức không được để trống"
    : asyncFieldErrorMessage;

  // Parameter states
  const [paramName, setParamName] = useState("Top N");
  const [dataType, setDataType] = useState("INTEGER");
  const [allowableType, setAllowableType] = useState<"ALL" | "LIST" | "RANGE">("RANGE");
  const [rangeMin, setRangeMin] = useState("1");
  const [rangeMax, setRangeMax] = useState("50");
  const [rangeStep, setRangeStep] = useState("1");
  const [listValues, setListValues] = useState("5, 10, 20, 50");
  const [paramCurrentValue, setParamCurrentValue] = useState("10");

  // Derived parameter validation state
  const { paramValid, paramError } = useMemo(() => {
    if (!paramCurrentValue.trim()) {
      return {
        paramValid: false,
        paramError: "ERR-VIZ-02: Vui lòng nhập giá trị hiện tại của tham số.",
      };
    }

    if (allowableType === "RANGE") {
      const min = parseFloat(rangeMin);
      const max = parseFloat(rangeMax);
      const curr = parseFloat(paramCurrentValue);

      if (isNaN(curr) || isNaN(min) || isNaN(max)) {
        return {
          paramValid: false,
          paramError: "ERR-VIZ-02: Giá trị tham số và giới hạn phải là số hợp lệ.",
        };
      }
      if (curr < min || curr > max) {
        return {
          paramValid: false,
          paramError: `ERR-VIZ-02: Giá trị ${curr} nằm ngoài phạm vi cho phép [${min}, ${max}].`,
        };
      }
      return { paramValid: true, paramError: null };
    }

    if (allowableType === "LIST") {
      const allowedList = listValues
        .split(",")
        .map((s) => s.trim())
        .filter(Boolean);
      if (allowedList.length > 0 && !allowedList.includes(paramCurrentValue.trim())) {
        return {
          paramValid: false,
          paramError: `ERR-VIZ-02: Giá trị "${paramCurrentValue}" không nằm trong danh sách giá trị cho phép.`,
        };
      }
      return { paramValid: true, paramError: null };
    }

    return { paramValid: true, paramError: null };
  }, [paramCurrentValue, rangeMin, rangeMax, listValues, allowableType]);

  // Validate formula whenever it changes (with debounce)
  useEffect(() => {
    if (!open || activeTab !== "field" || !formula.trim()) return;

    const timer = setTimeout(async () => {
      setValidating(true);
      try {
        const res = await vizService.validateFormula({
          datasourceId,
          formula,
          currentFieldName: fieldName,
        });
        if (res.valid) {
          setAsyncFieldValid(true);
          setAsyncFieldErrorMessage(null);
          setErrorPos(null);
        } else {
          setAsyncFieldValid(false);
          const posText = res.errorPosition != null ? ` tại vị trí ${res.errorPosition}` : "";
          setAsyncFieldErrorMessage(
            `${res.errorCode || "ERR-VIZ-01"}: ${res.errorMessage || "Công thức sai cú pháp"}${posText}`,
          );
          setErrorPos(res.errorPosition ?? null);
        }
      } catch {
        // Fallback client-side syntax checks (missing brackets, empty expression, etc.)
        const openBrackets = (formula.match(/\[/g) || []).length;
        const closeBrackets = (formula.match(/\]/g) || []).length;
        const openParens = (formula.match(/\(/g) || []).length;
        const closeParens = (formula.match(/\)/g) || []).length;

        if (openBrackets !== closeBrackets || openParens !== closeParens) {
          setAsyncFieldValid(false);
          setAsyncFieldErrorMessage("ERR-VIZ-01: Công thức thiếu ngoặc đóng/mở");
          setErrorPos(formula.length);
        } else if (
          formula.trim().endsWith("/") ||
          formula.trim().endsWith("+") ||
          formula.trim().endsWith("-") ||
          formula.trim().endsWith("*")
        ) {
          setAsyncFieldValid(false);
          setAsyncFieldErrorMessage("ERR-VIZ-01: Công thức kết thúc bằng toán tử thiếu toán hạng");
          setErrorPos(formula.length);
        } else {
          setAsyncFieldValid(true);
          setAsyncFieldErrorMessage(null);
          setErrorPos(null);
        }
      } finally {
        setValidating(false);
      }
    }, 250);

    return () => clearTimeout(timer);
  }, [formula, fieldName, datasourceId, open, activeTab]);

  const handleSaveField = async () => {
    if (!fieldValid || !fieldName.trim()) return;
    try {
      await vizService.saveCalculatedField(datasourceId, {
        name: fieldName.trim(),
        formula: formula.trim(),
        returnType,
      });
      toast(`Đã tạo trường tính toán "${fieldName}"`);
      onSuccess?.();
      onOpenChange(false);
    } catch {
      toast("Lỗi lưu trường tính toán", "error");
    }
  };

  const handleSaveParameter = async () => {
    if (!paramValid || !paramName.trim()) return;
    try {
      const config: Record<string, unknown> =
        allowableType === "RANGE"
          ? { min: parseFloat(rangeMin), max: parseFloat(rangeMax), step: parseFloat(rangeStep) }
          : allowableType === "LIST"
            ? { values: listValues.split(",").map((s) => s.trim()) }
            : {};

      await vizService.saveParameter(workbookId, {
        name: paramName.trim(),
        dataType,
        allowableValuesType: allowableType,
        allowableValuesConfig: config,
        currentValue: paramCurrentValue.trim(),
      });
      toast(`Đã tạo tham số "${paramName}"`);
      onSuccess?.();
      onOpenChange(false);
    } catch {
      toast("Lỗi lưu tham số", "error");
    }
  };

  return (
    <Dialog
      open={open}
      onOpenChange={onOpenChange}
      title="Tạo trường tính toán & Tham số (UC16)"
      className="max-w-3xl"
    >
      <Tabs
        defaultValue="field"
        value={activeTab}
        onValueChange={(v) => setActiveTab(v as "field" | "parameter")}
      >
        <TabsList>
          <TabsTrigger value="field">Trường tính toán (Calculated Field)</TabsTrigger>
          <TabsTrigger value="parameter">Tham số (Parameter)</TabsTrigger>
        </TabsList>

        {/* Tab 1: Calculated Field */}
        <TabsContent value="field" className="pt-4">
          <div className="grid grid-cols-[1fr_180px] gap-3">
            <div>
              <Label required>Tên trường tính toán</Label>
              <Input
                value={fieldName}
                onChange={(e) => setFieldName(e.target.value)}
                placeholder="Ví dụ: Tỷ suất lợi nhuận"
              />
            </div>
            <div>
              <Label>Kiểu dữ liệu trả về</Label>
              <select
                className="h-10 w-full rounded-lg border bg-white px-3 text-sm"
                value={returnType}
                onChange={(e) => setReturnType(e.target.value)}
              >
                <option value="FLOAT">Số thực (Float)</option>
                <option value="INTEGER">Số nguyên (Integer)</option>
                <option value="STRING">Chuỗi (String)</option>
                <option value="BOOLEAN">Logic (Boolean)</option>
                <option value="DATE">Ngày tháng (Date)</option>
              </select>
            </div>
          </div>

          <div className="mt-4 grid gap-4 md:grid-cols-[1fr_190px]">
            <div>
              <div className="flex items-center justify-between">
                <Label required>Công thức tính toán</Label>
                <span className="text-xs text-muted-foreground">{formula.length} / 8.000</span>
              </div>
              <Textarea
                className="mt-1 min-h-44 font-mono text-xs"
                value={formula}
                onChange={(e) => setFormula(e.target.value)}
                maxLength={8000}
                placeholder="Ví dụ: SUM([Doanh số]) / SUM([Lợi nhuận])"
              />
              {errorPos != null && (
                <div className="mt-1 text-xs text-primary font-mono">
                  Lỗi tại ký tự thứ: {errorPos}
                </div>
              )}
            </div>

            <div className="rounded-lg border bg-muted p-3">
              <div className="mb-2 text-xs font-semibold">Chèn hàm mẫu</div>
              <div className="space-y-1">
                {[
                  "SUM([Doanh số])",
                  "AVG([Lợi nhuận])",
                  "COUNT([Mã đơn])",
                  "IF [Doanh số] > 100 THEN 'Cao' ELSE 'Thấp' END",
                  "YEAR([Ngày đặt hàng])",
                  "ZN([Lợi nhuận])",
                ].map((fn) => (
                  <button
                    key={fn}
                    type="button"
                    onClick={() => setFormula((f) => f + " " + fn)}
                    className="block w-full truncate rounded px-2 py-1 text-left text-xs hover:bg-white hover:text-primary transition-colors"
                    title={fn}
                  >
                    {fn}
                  </button>
                ))}
              </div>
            </div>
          </div>

          {/* Validation Status Banner */}
          <div
            className={`mt-3 flex items-center gap-2 rounded-lg border p-3 text-xs transition-colors ${
              validating
                ? "border-muted bg-muted text-muted-foreground"
                : fieldValid
                  ? "border-green-200 bg-green-50 text-green-700"
                  : "border-primary-border bg-primary-tint text-primary"
            }`}
          >
            {validating ? (
              <span>Đang kiểm tra cú pháp công thức...</span>
            ) : fieldValid ? (
              <>
                <CheckCircle2 className="size-4 shrink-0 text-green-600" />
                <span>Công thức hợp lệ và sẵn sàng tính toán.</span>
              </>
            ) : (
              <>
                <AlertCircle className="size-4 shrink-0 text-primary" />
                <span className="font-medium">{fieldErrorMessage}</span>
              </>
            )}
          </div>

          <div className="mt-5 flex justify-end gap-2">
            <Button onClick={() => onOpenChange(false)}>Hủy</Button>
            <Button
              variant="primary"
              disabled={!fieldValid || !fieldName.trim() || validating}
              onClick={handleSaveField}
            >
              Lưu trường tính toán
            </Button>
          </div>
        </TabsContent>

        {/* Tab 2: Parameter */}
        <TabsContent value="parameter" className="space-y-4 pt-4">
          <div className="grid grid-cols-2 gap-3">
            <div>
              <Label required>Tên tham số</Label>
              <Input
                value={paramName}
                onChange={(e) => setParamName(e.target.value)}
                placeholder="Ví dụ: Top N hoặc Ngưỡng doanh thu"
              />
            </div>
            <div>
              <Label>Kiểu dữ liệu</Label>
              <select
                className="h-10 w-full rounded-lg border bg-white px-3 text-sm"
                value={dataType}
                onChange={(e) => setDataType(e.target.value)}
              >
                <option value="INTEGER">Số nguyên (Integer)</option>
                <option value="FLOAT">Số thực (Float)</option>
                <option value="STRING">Chuỗi (String)</option>
                <option value="BOOLEAN">Logic (Boolean)</option>
                <option value="DATE">Ngày tháng (Date)</option>
              </select>
            </div>
          </div>

          <div>
            <Label>Giá trị cho phép</Label>
            <div className="mt-1 flex gap-4">
              <label className="flex items-center gap-2 text-xs">
                <input
                  type="radio"
                  name="allowableType"
                  checked={allowableType === "ALL"}
                  onChange={() => setAllowableType("ALL")}
                  className="accent-primary"
                />
                Tất cả (All)
              </label>
              <label className="flex items-center gap-2 text-xs">
                <input
                  type="radio"
                  name="allowableType"
                  checked={allowableType === "RANGE"}
                  onChange={() => setAllowableType("RANGE")}
                  className="accent-primary"
                />
                Phạm vi (Range)
              </label>
              <label className="flex items-center gap-2 text-xs">
                <input
                  type="radio"
                  name="allowableType"
                  checked={allowableType === "LIST"}
                  onChange={() => setAllowableType("LIST")}
                  className="accent-primary"
                />
                Danh sách (List)
              </label>
            </div>
          </div>

          {allowableType === "RANGE" && (
            <div className="grid grid-cols-3 gap-3 rounded-lg border bg-muted/40 p-3">
              <div>
                <Label>Giá trị nhỏ nhất (Min)</Label>
                <Input value={rangeMin} onChange={(e) => setRangeMin(e.target.value)} />
              </div>
              <div>
                <Label>Giá trị lớn nhất (Max)</Label>
                <Input value={rangeMax} onChange={(e) => setRangeMax(e.target.value)} />
              </div>
              <div>
                <Label>Bước nhảy (Step)</Label>
                <Input value={rangeStep} onChange={(e) => setRangeStep(e.target.value)} />
              </div>
            </div>
          )}

          {allowableType === "LIST" && (
            <div className="rounded-lg border bg-muted/40 p-3">
              <Label>Danh sách giá trị cho phép (phân cách bởi dấu phẩy)</Label>
              <Input
                value={listValues}
                onChange={(e) => setListValues(e.target.value)}
                placeholder="5, 10, 20, 50"
              />
            </div>
          )}

          <div>
            <Label required>Giá trị hiện tại</Label>
            <Input
              value={paramCurrentValue}
              onChange={(e) => setParamCurrentValue(e.target.value)}
              placeholder="Nhập giá trị mặc định..."
            />
          </div>

          {/* Parameter Error/Success Banner */}
          <div
            className={`flex items-center gap-2 rounded-lg border p-3 text-xs transition-colors ${
              paramValid
                ? "border-green-200 bg-green-50 text-green-700"
                : "border-primary-border bg-primary-tint text-primary"
            }`}
          >
            {paramValid ? (
              <>
                <CheckCircle2 className="size-4 shrink-0 text-green-600" />
                <span>Giá trị tham số nằm trong phạm vi cho phép.</span>
              </>
            ) : (
              <>
                <AlertCircle className="size-4 shrink-0 text-primary" />
                <span className="font-medium">{paramError}</span>
              </>
            )}
          </div>

          <div className="mt-5 flex justify-end gap-2">
            <Button onClick={() => onOpenChange(false)}>Hủy</Button>
            <Button
              variant="primary"
              disabled={!paramValid || !paramName.trim()}
              onClick={handleSaveParameter}
            >
              Lưu tham số
            </Button>
          </div>
        </TabsContent>
      </Tabs>
    </Dialog>
  );
}
