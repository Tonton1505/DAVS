import { useEffect, useMemo, useState } from "react";
import { AlertCircle, AlertTriangle, Plus, ShieldCheck, X } from "lucide-react";
import { Button, Card, Select } from "../ui";
import { extractApiError } from "../../lib/axios/client";
import { useUIStore } from "../../stores/ui.store";
import {
  permissionService,
  type Capability,
  type PermissionRuleDto,
  type PermissionSubjectDto,
  type PermissionTemplateDto,
  type ResourcePermissionsResponse,
  type ResourceType,
  type RuleInput,
  type UiEffect,
} from "../../services/permission.service";
import { TemplatePicker } from "./TemplatePicker";
import { PermissionRuleRow, type SubjectRuleDraft } from "./PermissionRuleRow";
import { EffectivePermissionsPanel } from "./EffectivePermissionsPanel";

interface PermissionsPanelBodyProps {
  resourceType: ResourceType;
  resourceId: string;
  onClose?: () => void;
}

function groupRulesToDrafts(rules: PermissionRuleDto[]): SubjectRuleDraft[] {
  const map = new Map<string, SubjectRuleDraft>();
  for (const r of rules) {
    const key = `${r.subjectType}:${r.subjectId}`;
    let draft = map.get(key);
    if (!draft) {
      draft = {
        subjectType: r.subjectType,
        subjectId: r.subjectId,
        subjectName: r.subjectName,
        subjectDetail: r.subjectDetail,
        effects: {},
      };
      map.set(key, draft);
    }
    draft.effects[r.capability] = r.effect;
  }
  return Array.from(map.values());
}

export function PermissionsPanelBody({
  resourceType,
  resourceId,
  onClose,
}: PermissionsPanelBodyProps) {
  const toast = useUIStore((s) => s.toast);

  const [loading, setLoading] = useState(true);
  const [saving, setSaving] = useState(false);
  const [loadError, setLoadError] = useState<string | null>(null);
  const [saveError, setSaveError] = useState<string | null>(null);
  const [warnings, setWarnings] = useState<string[]>([]);

  const [permState, setPermState] = useState<ResourcePermissionsResponse | null>(null);
  const [templates, setTemplates] = useState<PermissionTemplateDto[]>([]);
  const [subjects, setSubjects] = useState<PermissionSubjectDto[]>([]);

  const [drafts, setDrafts] = useState<SubjectRuleDraft[]>([]);
  const [showAddForm, setShowAddForm] = useState(false);
  const [selectedSubjectKey, setSelectedSubjectKey] = useState("");
  const [selectedTemplateId, setSelectedTemplateId] = useState("");
  const [subjectError, setSubjectError] = useState("");
  const [tab, setTab] = useState<"rules" | "effective">("rules");

  useEffect(() => {
    let cancelled = false;
    Promise.all([
      permissionService.getPermissions(resourceType, resourceId),
      permissionService.listTemplates(),
      permissionService.listSubjects(resourceType, resourceId),
    ])
      .then(([perms, tpls, subs]) => {
        if (cancelled) return;
        setLoadError(null);
        setPermState(perms);
        setTemplates(tpls);
        setSubjects(subs);
        setDrafts(groupRulesToDrafts(perms.rules));
      })
      .catch((err) => {
        if (cancelled) return;
        const parsed = extractApiError(err);
        setLoadError(parsed?.message || "Không thể tải danh sách quyền.");
      })
      .finally(() => {
        if (!cancelled) setLoading(false);
      });
    return () => {
      cancelled = true;
    };
  }, [resourceType, resourceId]);

  const readOnly = Boolean(permState && (!permState.canEdit || permState.lockedByParent));

  const availableSubjects = useMemo(() => {
    const used = new Set(drafts.map((d) => `${d.subjectType}:${d.subjectId}`));
    return subjects.filter((s) => !used.has(`${s.subjectType}:${s.id}`));
  }, [subjects, drafts]);

  const addDraftRule = () => {
    setSubjectError("");
    if (!selectedSubjectKey) {
      setSubjectError("Vui lòng chọn người dùng hoặc nhóm.");
      return;
    }
    const found = subjects.find((s) => `${s.subjectType}:${s.id}` === selectedSubjectKey);
    if (!found) {
      setSubjectError("Đối tượng nhận không hợp lệ.");
      return;
    }

    const effects: Partial<Record<Capability, UiEffect>> = {};
    if (selectedTemplateId) {
      const tpl = templates.find((t) => t.id === selectedTemplateId);
      if (tpl) {
        for (const cap of tpl.capabilities) {
          effects[cap] = "ALLOW";
        }
      }
    }

    setDrafts((prev) => [
      ...prev,
      {
        subjectType: found.subjectType,
        subjectId: found.id,
        subjectName: found.name,
        subjectDetail: found.detail,
        effects,
      },
    ]);
    setSelectedSubjectKey("");
    setSelectedTemplateId("");
    setShowAddForm(false);
  };

  const handleChangeEffect = (
    subjectType: string,
    subjectId: string,
    capability: Capability,
    effect: UiEffect,
  ) => {
    setDrafts((prev) =>
      prev.map((d) =>
        d.subjectType === subjectType && d.subjectId === subjectId
          ? { ...d, effects: { ...d.effects, [capability]: effect } }
          : d,
      ),
    );
  };

  const handleRemoveRow = (subjectType: string, subjectId: string) => {
    setDrafts((prev) =>
      prev.filter((d) => !(d.subjectType === subjectType && d.subjectId === subjectId)),
    );
  };

  const handleSave = async () => {
    setSaveError(null);
    setWarnings([]);

    // VAL-PRJ-12: Every added subject row must specify at least 1 capability
    for (const d of drafts) {
      const hasAny = Object.values(d.effects).some((e) => e === "ALLOW" || e === "DENY");
      if (!hasAny) {
        setSaveError(`Vui lòng chọn ít nhất 1 khả năng cho "${d.subjectName}".`);
        return;
      }
    }

    const rules: RuleInput[] = [];
    for (const d of drafts) {
      for (const [cap, eff] of Object.entries(d.effects)) {
        if (eff === "ALLOW" || eff === "DENY") {
          rules.push({
            subjectType: d.subjectType,
            subjectId: d.subjectId,
            capability: cap as Capability,
            effect: eff,
          });
        }
      }
    }

    setSaving(true);
    try {
      const res = await permissionService.updatePermissions(resourceType, resourceId, rules);
      setPermState(res.permissions);
      setDrafts(groupRulesToDrafts(res.permissions.rules));
      setWarnings(res.warnings || []);
      toast("Đã lưu quy tắc quyền");
    } catch (err) {
      const parsed = extractApiError(err);
      setSaveError(parsed?.message || "Không thể lưu quy tắc quyền.");
    } finally {
      setSaving(false);
    }
  };

  const handleCancel = () => {
    if (permState) {
      setDrafts(groupRulesToDrafts(permState.rules));
    }
    setSaveError(null);
    setWarnings([]);
    setShowAddForm(false);
    onClose?.();
  };

  if (loading) {
    return <div className="p-6 text-sm text-muted-foreground">Đang tải cấu hình quyền…</div>;
  }

  if (loadError) {
    return (
      <div role="alert" className="flex items-center gap-2 p-6 text-sm text-primary">
        <AlertCircle className="size-4 shrink-0" />
        <span>{loadError}</span>
      </div>
    );
  }

  return (
    <div className="space-y-4">
      <div className="flex flex-wrap items-center justify-between gap-2 border-b pb-3">
        <div>
          <div className="text-base font-semibold text-ink">
            Thiết lập quyền: {permState?.resourceName}
          </div>
          <div className="text-xs text-muted-foreground">
            Chế độ: {permState?.permissionMode}
          </div>
        </div>
        <div className="flex items-center gap-2">
          <Button
            variant={tab === "rules" ? "primary" : "secondary"}
            onClick={() => setTab("rules")}
          >
            Quy tắc quyền
          </Button>
          <Button
            variant={tab === "effective" ? "primary" : "secondary"}
            onClick={() => setTab("effective")}
          >
            <ShieldCheck className="size-4" />
            Quyền hiệu lực
          </Button>
        </div>
      </div>

      {permState?.lockedByParent && (
        <div
          role="status"
          className="flex items-start gap-2 rounded-lg border border-amber-300 bg-amber-50 p-3 text-xs text-amber-900"
        >
          <AlertTriangle className="mt-0.5 size-4 shrink-0" />
          <span>
            Dự án cha đang ở chế độ Khóa quyền (LOCKED). Trang quyền này đang ở chế độ chỉ xem
            (DIS-PRJ-03).
          </span>
        </div>
      )}

      {!permState?.lockedByParent && !permState?.canEdit && (
        <div
          role="status"
          className="flex items-start gap-2 rounded-lg border bg-muted p-3 text-xs text-muted-foreground"
        >
          <AlertCircle className="mt-0.5 size-4 shrink-0" />
          <span>Bạn chỉ có quyền xem, không có quyền Đổi quyền trên tài nguyên này.</span>
        </div>
      )}

      {saveError && (
        <div
          role="alert"
          className="flex items-start gap-2 rounded-lg border border-primary-border bg-primary-tint p-3 text-xs text-primary"
        >
          <AlertCircle className="mt-0.5 size-4 shrink-0" />
          <span>{saveError}</span>
        </div>
      )}

      {warnings.length > 0 && (
        <div
          role="status"
          className="space-y-1 rounded-lg border border-amber-300 bg-amber-50 p-3 text-xs text-amber-900"
        >
          {warnings.map((w) => (
            <div key={w} className="flex items-start gap-2">
              <AlertTriangle className="mt-0.5 size-4 shrink-0" />
              <span>{w}</span>
            </div>
          ))}
        </div>
      )}

      {tab === "rules" ? (
        <div className="space-y-4">
          {!readOnly && (
            <div className="flex justify-end">
              <Button onClick={() => setShowAddForm((v) => !v)}>
                <Plus className="size-4" />
                Thêm quy tắc
              </Button>
            </div>
          )}

          {showAddForm && !readOnly && (
            <Card className="space-y-3 bg-muted/30">
              <div className="text-sm font-medium text-ink">Thêm quy tắc quyền mới</div>
              <div>
                <label
                  htmlFor="permission-subject-select"
                  className="mb-1.5 block text-xs font-medium text-foreground"
                >
                  Đối tượng nhận *
                </label>
                <Select
                  id="permission-subject-select"
                  className="w-full"
                  value={selectedSubjectKey}
                  onChange={(e) => {
                    setSelectedSubjectKey(e.target.value);
                    if (subjectError) setSubjectError("");
                  }}
                >
                  <option value="">-- Chọn người dùng hoặc nhóm --</option>
                  {availableSubjects.map((s) => (
                    <option key={`${s.subjectType}:${s.id}`} value={`${s.subjectType}:${s.id}`}>
                      {s.subjectType === "GROUP" ? "[Nhóm]" : "[Người dùng]"} {s.name}
                      {s.detail && s.subjectType === "USER" ? ` (${s.detail})` : ""}
                    </option>
                  ))}
                </Select>
                {subjectError && <div className="mt-1 text-xs text-primary">{subjectError}</div>}
              </div>

              <TemplatePicker
                templates={templates}
                value={selectedTemplateId}
                onSelect={setSelectedTemplateId}
              />

              <div className="flex justify-end gap-2">
                <Button onClick={() => setShowAddForm(false)}>Đóng</Button>
                <Button variant="primary" onClick={addDraftRule}>
                  Thêm vào danh sách
                </Button>
              </div>
            </Card>
          )}

          {drafts.length === 0 ? (
            <div className="rounded-lg border border-dashed p-6 text-center text-sm text-muted-foreground">
              Chưa có quy tắc quyền riêng nào trên tài nguyên này.
            </div>
          ) : (
            <div className="space-y-3">
              {drafts.map((row) => (
                <PermissionRuleRow
                  key={`${row.subjectType}:${row.subjectId}`}
                  row={row}
                  readOnly={readOnly}
                  onChangeEffect={(cap, eff) =>
                    handleChangeEffect(row.subjectType, row.subjectId, cap, eff)
                  }
                  onRemove={() => handleRemoveRow(row.subjectType, row.subjectId)}
                />
              ))}
            </div>
          )}

          <div className="flex justify-end gap-2 border-t pt-3">
            <Button onClick={handleCancel}>Hủy</Button>
            <Button variant="primary" disabled={readOnly || saving} onClick={handleSave}>
              {saving ? "Đang lưu…" : "Lưu"}
            </Button>
          </div>
        </div>
      ) : (
        <EffectivePermissionsPanel
          resourceType={resourceType}
          resourceId={resourceId}
          subjects={subjects}
        />
      )}
    </div>
  );
}

interface PermissionsDialogProps {
  open: boolean;
  resourceType: ResourceType;
  resourceId: string | null;
  onOpenChange: (open: boolean) => void;
}

export function PermissionsDialog({
  open,
  resourceType,
  resourceId,
  onOpenChange,
}: PermissionsDialogProps) {
  useEffect(() => {
    if (!open) return;
    const onKeyDown = (e: KeyboardEvent) => {
      if (e.key === "Escape") onOpenChange(false);
    };
    window.addEventListener("keydown", onKeyDown);
    return () => window.removeEventListener("keydown", onKeyDown);
  }, [open, onOpenChange]);

  if (!open || !resourceId) return null;

  return (
    <div
      role="dialog"
      aria-modal="true"
      aria-label="Thiết lập quyền truy cập"
      className="fixed inset-0 z-50 flex items-center justify-center bg-black/40 p-4"
    >
      <div className="max-h-[90vh] w-full max-w-4xl overflow-y-auto rounded-xl bg-white p-6 shadow-xl">
        <div className="mb-2 flex justify-end">
          <button
            type="button"
            aria-label="Đóng hộp thoại quyền"
            onClick={() => onOpenChange(false)}
            className="rounded p-1 text-muted-foreground hover:bg-muted"
          >
            <X className="size-4" />
          </button>
        </div>
        <PermissionsPanelBody
          resourceType={resourceType}
          resourceId={resourceId}
          onClose={() => onOpenChange(false)}
        />
      </div>
    </div>
  );
}
