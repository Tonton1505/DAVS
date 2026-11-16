import { fireEvent, render, screen } from "@testing-library/react";
import ActionsDrawer from "../ActionsDrawer";
import type { DashboardActionDto } from "../../../services/viz.service";

describe("ActionsDrawer (UC18)", () => {
  const initialActions: DashboardActionDto[] = [
    {
      id: "act-1",
      name: "Lọc theo vùng",
      actionType: "FILTER",
      triggerEvent: "SELECT",
      sourceSheetId: "Doanh số theo tháng",
      targetSheetId: "Top sản phẩm",
      config: { highlight: true },
    },
  ];

  const sheets = ["Doanh số theo tháng", "Top sản phẩm", "Cơ cấu kênh"];

  it("renders list of current actions and allows deleting an action", () => {
    const handleActionsChange = vi.fn();
    render(
      <ActionsDrawer
        open={true}
        onClose={vi.fn()}
        sheets={sheets}
        actions={initialActions}
        onActionsChange={handleActionsChange}
      />,
    );

    expect(screen.getByText("Tương tác & Hành động (UC18)")).toBeInTheDocument();
    expect(screen.getByText("Lọc theo vùng")).toBeInTheDocument();

    const deleteBtn = screen.getByTitle("Xóa hành động");
    fireEvent.click(deleteBtn);

    expect(handleActionsChange).toHaveBeenCalledWith([]);
  });

  it("enforces ERR-VIZ-07 when source sheet equals target sheet", () => {
    const handleActionsChange = vi.fn();
    render(
      <ActionsDrawer
        open={true}
        onClose={vi.fn()}
        sheets={sheets}
        actions={initialActions}
        onActionsChange={handleActionsChange}
      />,
    );

    const addActionBtn = screen.getByText("Thêm hành động");
    fireEvent.click(addActionBtn);

    expect(screen.getByText("Tạo hành động tương tác mới")).toBeInTheDocument();

    // Set target sheet to same as source sheet ("Doanh số theo tháng")
    const targetSelect = screen.getByDisplayValue("Top sản phẩm");
    fireEvent.change(targetSelect, { target: { value: "Doanh số theo tháng" } });

    const saveBtn = screen.getByText("Lưu hành động");
    fireEvent.click(saveBtn);

    expect(
      screen.getByText(/ERR-VIZ-07: Sheet nguồn và sheet đích không được trùng nhau/i),
    ).toBeInTheDocument();
    expect(handleActionsChange).not.toHaveBeenCalled();
  });

  it("enforces BUS-VIZ-12 when URL action does not start with https://", () => {
    render(
      <ActionsDrawer
        open={true}
        onClose={vi.fn()}
        sheets={sheets}
        actions={initialActions}
        onActionsChange={vi.fn()}
      />,
    );

    const addActionBtn = screen.getByText("Thêm hành động");
    fireEvent.click(addActionBtn);

    // Switch action type to URL
    const actionTypeSelect = screen.getByDisplayValue("Lọc dữ liệu (Filter)");
    fireEvent.change(actionTypeSelect, { target: { value: "URL" } });

    const urlInput = screen.getByPlaceholderText("https://example.com/analytics");
    fireEvent.change(urlInput, { target: { value: "http://insecure-site.com" } });

    const saveBtn = screen.getByText("Lưu hành động");
    fireEvent.click(saveBtn);

    expect(
      screen.getByText(/BUS-VIZ-12: URL hành động bắt buộc phải dùng giao thức https:\/\/ bảo mật/i),
    ).toBeInTheDocument();
  });
});
