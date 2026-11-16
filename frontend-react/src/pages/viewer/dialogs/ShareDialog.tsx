import { useEffect, useState } from "react";
import { Copy } from "lucide-react";
import {
  Alert,
  Button,
  Dialog,
  Input,
  Label,
  Tabs,
  TabsContent,
  TabsList,
  TabsTrigger,
  Textarea,
} from "../../../components/ui";
import {
  shareService,
  type DirectoryItem,
} from "../../../services/share.service";
import { useUIStore } from "../../../stores/ui.store";

export interface ShareDialogProps {
  open: boolean;
  onOpenChange: (v: boolean) => void;
  resourceType?: "WORKBOOK" | "DASHBOARD" | "VIEW";
  resourceId?: string;
}

export default function ShareDialog({
  open,
  onOpenChange,
  resourceType = "DASHBOARD",
  resourceId = "00000000-0000-0000-0000-000000000701",
}: ShareDialogProps) {
  const toast = useUIStore((s) => s.toast);

  const [recipient, setRecipient] = useState("");
  const [permissionLevel, setPermissionLevel] = useState<"VIEW" | "INTERACT" | "EDIT">("VIEW");
  const [message, setMessage] = useState("");
  const [sendEmail, setSendEmail] = useState(true);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);

  const [shareLink, setShareLink] = useState("https://davs.congty.vn/d/tong-quan");
  const [embedCode, setEmbedCode] = useState('<iframe src="https://davs.congty.vn/embed/tong-quan"></iframe>');
  const [suggestions, setSuggestions] = useState<DirectoryItem[]>([]);
  const [showSuggestions, setShowSuggestions] = useState(false);

  useEffect(() => {
    if (!open) return;
    shareService
      .getShareLink(resourceType, resourceId)
      .then((res) => {
        if (res.shareLink) setShareLink(res.shareLink);
        if (res.embedCode) setEmbedCode(res.embedCode);
      })
      .catch(() => {
        // Use defaults
      });
  }, [open, resourceType, resourceId]);

  const handleRecipientChange = (val: string) => {
    setRecipient(val);
    setError(null);
    if (val.trim().length >= 2 && shareService.searchDirectory) {
      Promise.resolve(shareService.searchDirectory(val.trim()))
        .then((items) => {
          setSuggestions(items || []);
          setShowSuggestions(true);
        })
        .catch(() => {
          setSuggestions([]);
        });
    } else {
      setShowSuggestions(false);
    }
  };

  const handleSelectSuggestion = (item: DirectoryItem) => {
    setRecipient(item.email || item.name);
    setShowSuggestions(false);
  };

  const handleShare = async () => {
    if (!recipient.trim()) {
      setError("Vui lòng nhập người nhận.");
      return;
    }

    const recipientList = recipient
      .split(/[,;\n]+/)
      .map((r) => r.trim())
      .filter(Boolean);

    if (recipientList.length > 100) {
      setError("Tối đa 100 người nhận trong một lần chia sẻ (BUS-SHR-07).");
      return;
    }

    setLoading(true);
    setError(null);

    try {
      await shareService.shareContent({
        resourceType,
        resourceId,
        recipients: recipientList,
        permissionLevel,
        message,
        sendEmailNotification: sendEmail,
      });
      toast("Đã chia sẻ thành công");
      onOpenChange(false);
    } catch (err: unknown) {
      const axiosErr = err as {
        response?: { data?: { code?: string; message?: string } };
        message?: string;
      };
      const code = axiosErr.response?.data?.code;
      const msg = axiosErr.response?.data?.message || axiosErr.message || "Chia sẻ thất bại";

      if (code === "ERR-SHR-01") {
        setError(`Không tìm thấy người nhận "${recipientList.join(", ")}". Vui lòng kiểm tra lại.`);
      } else if (code === "ERR-SHR-02") {
        setError("Chính sách không cho phép chia sẻ cho người ngoài tổ chức.");
      } else {
        setError(msg);
      }
    } finally {
      setLoading(false);
    }
  };

  return (
    <Dialog
      open={open}
      onOpenChange={onOpenChange}
      title="Chia sẻ nội dung"
      description="Cấp quyền cho người dùng hoặc nhóm trong Công ty."
      className="max-w-xl"
    >
      {error && (
        <div className="mb-4" data-testid="share-error-alert">
          <Alert className="border-red-200 bg-red-50 text-red-900">
            {error}
          </Alert>
        </div>
      )}

      <Tabs defaultValue="people">
        <TabsList>
          <TabsTrigger value="people">Người nhận</TabsTrigger>
          <TabsTrigger value="link">Liên kết & Nhúng</TabsTrigger>
        </TabsList>

        <TabsContent value="people" className="space-y-4 pt-5">
          <div className="relative">
            <Label required>Người nhận</Label>
            <Input
              value={recipient}
              onChange={(e) => handleRecipientChange(e.target.value)}
              placeholder="Nhập tên, email hoặc nhóm (ví dụ: colleague@congty.vn)"
              data-testid="share-recipient-input"
            />
            {showSuggestions && suggestions.length > 0 && (
              <div className="absolute z-10 mt-1 max-h-48 w-full overflow-y-auto rounded-lg border bg-card p-1 shadow-lg">
                {suggestions.map((item) => (
                  <button
                    key={item.id}
                    type="button"
                    onClick={() => handleSelectSuggestion(item)}
                    className="flex w-full items-center justify-between rounded p-2 text-left text-sm hover:bg-muted"
                  >
                    <div>
                      <div className="font-medium">{item.name}</div>
                      {item.email && <div className="text-xs text-muted-foreground">{item.email}</div>}
                    </div>
                    <span className="rounded bg-muted px-1.5 py-0.5 text-xs text-muted-foreground">
                      {item.type === "GROUP" ? "Nhóm" : item.isExternal ? "Ngoài tổ chức" : "Thành viên"}
                    </span>
                  </button>
                ))}
              </div>
            )}
          </div>

          <div>
            <Label>Mức quyền</Label>
            <select
              value={permissionLevel}
              onChange={(e) => setPermissionLevel(e.target.value as "VIEW" | "INTERACT" | "EDIT")}
              className="h-10 w-full rounded-lg border px-3"
              data-testid="share-permission-select"
            >
              <option value="VIEW">Xem</option>
              <option value="INTERACT">Tương tác</option>
              <option value="EDIT">Chỉnh sửa</option>
            </select>
          </div>

          <div>
            <Label>Lời nhắn (tối đa 500 ký tự)</Label>
            <Textarea
              maxLength={500}
              value={message}
              onChange={(e) => setMessage(e.target.value)}
              placeholder="Nhập lời nhắn gửi kèm..."
            />
            <div className="mt-1 text-right text-xs text-muted-foreground">
              {message.length} / 500
            </div>
          </div>

          <label className="flex items-center gap-2 text-sm font-medium">
            <input
              type="checkbox"
              checked={sendEmail}
              onChange={(e) => setSendEmail(e.target.checked)}
              className="accent-primary"
            />
            Gửi thông báo email
          </label>
        </TabsContent>

        <TabsContent value="link" className="space-y-4 pt-5">
          <div>
            <Label>Liên kết trực tiếp</Label>
            <div className="flex gap-2">
              <Input readOnly value={shareLink} data-testid="share-link-input" />
              <Button
                type="button"
                onClick={() => {
                  navigator.clipboard?.writeText(shareLink);
                  toast("Đã sao chép liên kết");
                }}
              >
                <Copy className="size-4" />
                Sao chép
              </Button>
            </div>
            <div className="mt-1 text-xs text-muted-foreground">
              Người mở liên kết cần có quyền truy cập được cấp trên hệ thống (BUS-SHR-05).
            </div>
          </div>

          <div>
            <Label>Mã nhúng iframe</Label>
            <Textarea readOnly value={embedCode} rows={3} data-testid="share-embed-input" />
            <div className="mt-2 flex justify-end">
              <Button
                type="button"
                onClick={() => {
                  navigator.clipboard?.writeText(embedCode);
                  toast("Đã sao chép mã nhúng");
                }}
              >
                <Copy className="size-4" />
                Sao chép mã nhúng
              </Button>
            </div>
          </div>
        </TabsContent>
      </Tabs>

      <div className="mt-6 flex justify-end gap-2">
        <Button onClick={() => onOpenChange(false)}>Hủy</Button>
        <Button
          variant="primary"
          disabled={loading}
          onClick={handleShare}
          data-testid="share-submit-button"
        >
          {loading ? "Đang gửi…" : "Chia sẻ"}
        </Button>
      </div>
    </Dialog>
  );
}
