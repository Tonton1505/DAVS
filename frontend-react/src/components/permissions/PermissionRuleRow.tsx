import { Trash2, Users, User } from "lucide-react";
import {
  ALL_CAPABILITIES,
  type Capability,
  type SubjectType,
  type UiEffect,
} from "../../services/permission.service";

export interface SubjectRuleDraft {
  subjectType: SubjectType;
  subjectId: string;
  subjectName: string;
  subjectDetail: string | null;
  effects: Partial<Record<Capability, UiEffect>>;
}

interface PermissionRuleRowProps {
  row: SubjectRuleDraft;
  readOnly?: boolean;
  onChangeEffect: (capability: Capability, effect: UiEffect) => void;
  onRemove: () => void;
}

export function PermissionRuleRow({
  row,
  readOnly,
  onChangeEffect,
  onRemove,
}: PermissionRuleRowProps) {
  return (
    <div className="rounded-lg border bg-white p-3">
      <div className="mb-3 flex items-center justify-between gap-2">
        <div className="flex items-center gap-2">
          {row.subjectType === "GROUP" ? (
            <Users className="size-4 text-muted-foreground" />
          ) : (
            <User className="size-4 text-muted-foreground" />
          )}
          <div>
            <div className="text-sm font-medium text-ink">{row.subjectName}</div>
            <div className="text-xs text-muted-foreground">
              {row.subjectType === "GROUP" ? "Nhóm" : "Người dùng"}
              {row.subjectDetail ? ` · ${row.subjectDetail}` : ""}
            </div>
          </div>
        </div>
        {!readOnly && (
          <button
            type="button"
            onClick={onRemove}
            aria-label={`Xóa quy tắc cho ${row.subjectName}`}
            className="rounded p-1.5 text-muted-foreground hover:bg-muted hover:text-primary"
          >
            <Trash2 className="size-4" />
          </button>
        )}
      </div>

      <div className="grid grid-cols-1 gap-2 sm:grid-cols-2">
        {ALL_CAPABILITIES.map((cap) => {
          const current: UiEffect = row.effects[cap.code] ?? "UNSPECIFIED";
          const radioName = `rule-${row.subjectType}-${row.subjectId}-${cap.code}`;
          return (
            <div
              key={cap.code}
              className="flex items-center justify-between rounded border px-2.5 py-1.5 text-xs"
            >
              <span className="font-medium text-foreground">{cap.label}</span>
              <div className="flex items-center gap-2">
                <label className="flex cursor-pointer items-center gap-1">
                  <input
                    type="radio"
                    name={radioName}
                    checked={current === "ALLOW"}
                    disabled={readOnly}
                    onChange={() => onChangeEffect(cap.code, "ALLOW")}
                    className="accent-primary"
                  />
                  <span>Cho phép</span>
                </label>
                <label className="flex cursor-pointer items-center gap-1">
                  <input
                    type="radio"
                    name={radioName}
                    checked={current === "DENY"}
                    disabled={readOnly}
                    onChange={() => onChangeEffect(cap.code, "DENY")}
                    className="accent-primary"
                  />
                  <span>Từ chối</span>
                </label>
                <label className="flex cursor-pointer items-center gap-1 text-muted-foreground">
                  <input
                    type="radio"
                    name={radioName}
                    checked={current === "UNSPECIFIED"}
                    disabled={readOnly}
                    onChange={() => onChangeEffect(cap.code, "UNSPECIFIED")}
                    className="accent-primary"
                  />
                  <span>Chưa chỉ định</span>
                </label>
              </div>
            </div>
          );
        })}
      </div>
    </div>
  );
}
