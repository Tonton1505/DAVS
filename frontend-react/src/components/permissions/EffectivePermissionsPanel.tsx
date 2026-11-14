import { useEffect, useState } from "react";
import { CheckCircle2, ShieldAlert, XCircle } from "lucide-react";
import { Select } from "../ui";
import {
  ALL_CAPABILITIES,
  permissionService,
  type EffectivePermissionsResponse,
  type PermissionSubjectDto,
  type ResourceType,
} from "../../services/permission.service";

interface EffectivePermissionsPanelProps {
  resourceType: ResourceType;
  resourceId: string;
  subjects: PermissionSubjectDto[];
}

export function EffectivePermissionsPanel({
  resourceType,
  resourceId,
  subjects,
}: EffectivePermissionsPanelProps) {
  const userSubjects = subjects.filter((s) => s.subjectType === "USER");
  const [selectedUserId, setSelectedUserId] = useState<string>("");
  const effectiveUserId = selectedUserId || userSubjects[0]?.id || "";
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [result, setResult] = useState<EffectivePermissionsResponse | null>(null);

  useEffect(() => {
    if (!effectiveUserId) {
      return;
    }
    let cancelled = false;
    permissionService
      .getEffective(resourceType, resourceId, effectiveUserId)
      .then((res) => {
        if (!cancelled) {
          setError(null);
          setResult(res);
        }
      })
      .catch(() => {
        if (!cancelled) setError("Không thể tải quyền hiệu lực.");
      })
      .finally(() => {
        if (!cancelled) setLoading(false);
      });
    return () => {
      cancelled = true;
    };
  }, [resourceType, resourceId, effectiveUserId]);

  return (
    <div className="space-y-4">
      <div>
        <label
          htmlFor="effective-user-select"
          className="mb-1.5 block text-xs font-medium text-foreground"
        >
          Chọn người dùng để kiểm tra quyền hiệu lực
        </label>
        <Select
          id="effective-user-select"
          className="w-full"
          value={effectiveUserId}
          onChange={(e) => {
            setLoading(true);
            setSelectedUserId(e.target.value);
          }}
        >
          {userSubjects.length === 0 && <option value="">Không có người dùng</option>}
          {userSubjects.map((u) => (
            <option key={u.id} value={u.id}>
              {u.name} {u.detail ? `(${u.detail})` : ""}
            </option>
          ))}
        </Select>
      </div>

      {loading && <div className="text-xs text-muted-foreground">Đang tính toán quyền hiệu lực…</div>}
      {error && (
        <div role="alert" className="text-xs text-primary">
          {error}
        </div>
      )}

      {result && !loading && (
        <div className="space-y-3">
          <div className="flex flex-wrap items-center justify-between gap-2 rounded-lg bg-muted/50 p-3 text-xs">
            <span>
              <strong>{result.displayName}</strong> · Vai trò site:{" "}
              <strong>{result.siteRole}</strong>
            </span>
            {result.lockedByParent && (
              <span className="inline-flex items-center gap-1 rounded bg-amber-100 px-2 py-0.5 text-amber-800">
                <ShieldAlert className="size-3.5" />
                Dự án cha đang khóa quyền
              </span>
            )}
          </div>

          <div className="grid grid-cols-1 gap-2 sm:grid-cols-2">
            {ALL_CAPABILITIES.map((cap) => {
              const allowed = result.effectiveCapabilities.includes(cap.code);
              return (
                <div
                  key={cap.code}
                  className="flex items-center justify-between rounded border px-3 py-2 text-xs"
                >
                  <span>{cap.label}</span>
                  {allowed ? (
                    <span className="inline-flex items-center gap-1 font-medium text-emerald-700">
                      <CheckCircle2 className="size-3.5" />
                      Cho phép
                    </span>
                  ) : (
                    <span className="inline-flex items-center gap-1 text-muted-foreground">
                      <XCircle className="size-3.5" />
                      Từ chối
                    </span>
                  )}
                </div>
              );
            })}
          </div>
        </div>
      )}
    </div>
  );
}
