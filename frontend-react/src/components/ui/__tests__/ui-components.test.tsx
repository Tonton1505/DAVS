import { render, screen } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { describe, expect, it, vi } from "vitest";
import {
  Badge,
  Button,
  Checkbox,
  ConfirmDialog,
  Dialog,
  EmptyState,
  FormField,
  Input,
  Radio,
  Select,
  Skeleton,
  Table,
  Tabs,
  TabsContent,
  TabsList,
  TabsTrigger,
  Td,
  Textarea,
  Toast,
} from "../index";

describe("Shared UI component library & basic a11y", () => {
  it("renders Button, Badge, Skeleton, EmptyState, Toast", async () => {
    const user = userEvent.setup();
    const onClick = vi.fn();
    const onDismiss = vi.fn();
    render(
      <div>
        <Button variant="primary" onClick={onClick}>
          Lưu thay đổi
        </Button>
        <Badge tone="error">Lỗi</Badge>
        <Skeleton className="h-4 w-20" />
        <EmptyState title="Trống" description="Chưa có bản ghi" />
        <Toast message="Đã lưu thành công" onDismiss={onDismiss} />
      </div>,
    );

    await user.click(screen.getByRole("button", { name: "Lưu thay đổi" }));
    expect(onClick).toHaveBeenCalledTimes(1);
    expect(screen.getByText("Lỗi")).toBeInTheDocument();
    expect(screen.getByText("Trống")).toBeInTheDocument();
    expect(screen.getByRole("status")).toHaveTextContent("Đã lưu thành công");
    await user.click(screen.getByRole("button", { name: "Đóng thông báo" }));
    expect(onDismiss).toHaveBeenCalledTimes(1);
  });

  it("renders accessible form controls: FormField, Input, Select, Textarea, Checkbox, Radio", async () => {
    const user = userEvent.setup();
    render(
      <form>
        <FormField id="f-name" label="Tên dự án" required error="Bắt buộc nhập">
          <Input id="f-name" invalid />
        </FormField>
        <FormField id="f-role" label="Vai trò">
          <Select id="f-role" defaultValue="viewer">
            <option value="viewer">Viewer</option>
            <option value="creator">Creator</option>
          </Select>
        </FormField>
        <FormField id="f-desc" label="Mô tả">
          <Textarea id="f-desc" />
        </FormField>
        <Checkbox id="f-check" label="Đồng ý điều khoản" />
        <Radio id="f-radio" name="mode" label="Extract" />
      </form>,
    );

    const nameInput = screen.getByLabelText(/Tên dự án/);
    expect(nameInput).toHaveAttribute("aria-invalid", "true");
    expect(screen.getByRole("alert")).toHaveTextContent("Bắt buộc nhập");

    const select = screen.getByLabelText("Vai trò");
    await user.selectOptions(select, "creator");
    expect(select).toHaveValue("creator");

    const textarea = screen.getByLabelText("Mô tả");
    await user.type(textarea, "Ghi chú");
    expect(textarea).toHaveValue("Ghi chú");

    const checkbox = screen.getByLabelText("Đồng ý điều khoản");
    await user.click(checkbox);
    expect(checkbox).toBeChecked();

    const radio = screen.getByLabelText("Extract");
    await user.click(radio);
    expect(radio).toBeChecked();
  });

  it("renders Table with pagination and Tabs", async () => {
    const user = userEvent.setup();
    const onPageChange = vi.fn();

    render(
      <div>
        <Table headers={["Tên"]} count={30} page={1} pageSize={10} onPageChange={onPageChange}>
          <tr>
            <Td>Dòng 1</Td>
          </tr>
        </Table>
        <Tabs defaultValue="a">
          <TabsList>
            <TabsTrigger value="a">Tab A</TabsTrigger>
            <TabsTrigger value="b">Tab B</TabsTrigger>
          </TabsList>
          <TabsContent value="a">Nội dung A</TabsContent>
          <TabsContent value="b">Nội dung B</TabsContent>
        </Tabs>
      </div>,
    );

    await user.click(screen.getByRole("button", { name: "Trang sau" }));
    expect(onPageChange).toHaveBeenCalledWith(2);

    await user.click(screen.getByRole("tab", { name: "Tab B" }));
    expect(screen.getByText("Nội dung B")).toBeVisible();
  });

  it("renders accessible Dialog and ConfirmDialog", async () => {
    const user = userEvent.setup();
    const onConfirm = vi.fn();

    const { rerender } = render(
      <Dialog open onOpenChange={() => {}} title="Hộp thoại">
        <p>Chi tiết</p>
      </Dialog>,
    );
    expect(screen.getByRole("dialog")).toHaveTextContent("Hộp thoại");
    expect(screen.getByRole("button", { name: "Đóng" })).toBeInTheDocument();

    rerender(<ConfirmDialog open onOpenChange={() => {}} title="Xác nhận xóa" onConfirm={onConfirm} />);
    await user.click(screen.getByRole("button", { name: "Xác nhận" }));
    expect(onConfirm).toHaveBeenCalledTimes(1);
  });
});
