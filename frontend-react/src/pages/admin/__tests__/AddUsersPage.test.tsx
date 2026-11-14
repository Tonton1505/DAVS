import { beforeEach, describe, expect, it, vi } from "vitest";
import { fireEvent, render, screen, waitFor } from "@testing-library/react";
import { MemoryRouter } from "react-router";
import AddUsersPage from "../AddUsersPage";
import { userService } from "../../../services/user.service";

vi.mock("../../../services/user.service", () => ({
  userService: {
    list: vi.fn(),
    listLicenses: vi.fn(),
    createUsers: vi.fn(),
    importUsers: vi.fn(),
    getImportStatus: vi.fn(),
    downloadImportErrors: vi.fn(),
    downloadTemplate: vi.fn(),
  },
}));

describe("AddUsersPage (UC02)", () => {
  beforeEach(() => {
    vi.clearAllMocks();
    vi.mocked(userService.listLicenses).mockResolvedValue([
      { role: "VIEWER", total: 200, used: 0, available: 200 },
      { role: "EXPLORER", total: 30, used: 2, available: 28 },
      { role: "CREATOR", total: 10, used: 0, available: 10 },
      { role: "SITE_ADMIN", total: 5, used: 1, available: 4 },
    ]);
  });

  it("validates invalid email format client-side before calling API", async () => {
    render(
      <MemoryRouter>
        <AddUsersPage />
      </MemoryRouter>,
    );

    await waitFor(() => expect(userService.listLicenses).toHaveBeenCalled());

    fireEvent.change(screen.getByRole("textbox", { name: /Tên tài khoản/i }), {
      target: { value: "sai.dinh.dang; valid@davs.local" },
    });
    fireEvent.click(screen.getByRole("button", { name: "Thêm người dùng" }));

    expect(await screen.findByRole("alert")).toHaveTextContent(
      'Tên tài khoản "sai.dinh.dang" không hợp lệ.',
    );
    expect(userService.createUsers).not.toHaveBeenCalled();
  });

  it("submits valid batch of users and renders created/skipped summary", async () => {
    vi.mocked(userService.createUsers).mockResolvedValue({
      createdCount: 1,
      skippedCount: 1,
      uninvitedCount: 0,
      created: [{ id: "u1", email: "new@davs.local", status: "PENDING" }],
      skipped: [
        {
          email: "dup@davs.local",
          code: "ERR-USR-02",
          message: 'Tài khoản "dup@davs.local" đã tồn tại.',
        },
      ],
    });

    render(
      <MemoryRouter>
        <AddUsersPage />
      </MemoryRouter>,
    );

    await waitFor(() => expect(userService.listLicenses).toHaveBeenCalled());

    fireEvent.change(screen.getByLabelText(/Phương thức xác thực/i), {
      target: { value: "SSO" },
    });
    fireEvent.change(screen.getByLabelText(/Vai trò trên site/i), {
      target: { value: "EXPLORER" },
    });
    fireEvent.change(screen.getByRole("textbox", { name: /Tên tài khoản/i }), {
      target: { value: "new@davs.local; dup@davs.local" },
    });
    fireEvent.click(screen.getByRole("button", { name: "Thêm người dùng" }));

    await waitFor(() => {
      expect(userService.createUsers).toHaveBeenCalledWith({
        authMethod: "SSO",
        usernames: ["new@davs.local", "dup@davs.local"],
        role: "EXPLORER",
      });
    });

    expect(await screen.findByText(/Đã tạo 1 tài khoản · Bỏ qua 1 trùng/i)).toBeInTheDocument();
  });

  it("rejects non-csv file client-side in import tab", async () => {
    render(
      <MemoryRouter>
        <AddUsersPage />
      </MemoryRouter>,
    );

    await waitFor(() => expect(userService.listLicenses).toHaveBeenCalled());

    const importTab = screen.getByRole("tab", { name: "Nhập từ tệp" });
    fireEvent.mouseDown(importTab, { button: 0, ctrlKey: false });
    fireEvent.click(importTab);

    const fileInput = await screen.findByLabelText("Chọn tệp CSV");
    const badFile = new File(["hello"], "users.txt", { type: "text/plain" });
    fireEvent.change(fileInput, { target: { files: [badFile] } });

    expect(await screen.findByRole("alert")).toHaveTextContent(/định dạng \.csv/i);
  });
});
