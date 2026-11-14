import { Select } from "../ui";
import type { PermissionTemplateDto } from "../../services/permission.service";

interface TemplatePickerProps {
  templates: PermissionTemplateDto[];
  value: string;
  disabled?: boolean;
  onSelect: (templateId: string) => void;
}

export function TemplatePicker({ templates, value, disabled, onSelect }: TemplatePickerProps) {
  return (
    <div>
      <label
        htmlFor="permission-template-select"
        className="mb-1.5 block text-xs font-medium text-foreground"
      >
        Mẫu quyền
      </label>
      <Select
        id="permission-template-select"
        className="w-full"
        value={value}
        disabled={disabled}
        onChange={(e) => onSelect(e.target.value)}
      >
        <option value="">Tùy chỉnh từng khả năng</option>
        {templates.map((tpl) => (
          <option key={tpl.id} value={tpl.id}>
            {tpl.name} — {tpl.description}
          </option>
        ))}
      </Select>
    </div>
  );
}
