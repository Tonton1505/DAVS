import { render, screen } from "@testing-library/react";
import { MemoryRouter } from "react-router";
import { beforeEach, describe, expect, it } from "vitest";
import { useAuthStore } from "../../../stores/auth.store";
import Sidebar from "../Sidebar";

describe("Sidebar role-based navigation (DIS-AUTH-01)", () => {
  beforeEach(() => {
    useAuthStore.getState().logout();
  });

  it("shows all admin and workspace items for admin", () => {
    useAuthStore.getState().login("admin");
    render(
      <MemoryRouter>
        <Sidebar />
      </MemoryRouter>,
    );

    expect(screen.getByText("Trang chủ")).toBeInTheDocument();
    expect(screen.getByText("Dự án")).toBeInTheDocument();
    expect(screen.getByText("Nguồn dữ liệu")).toBeInTheDocument();
    expect(screen.getByText("Người dùng")).toBeInTheDocument();
    expect(screen.getByText("Nhóm")).toBeInTheDocument();
    expect(screen.getByText("Cài đặt")).toBeInTheDocument();
  });

  it("hides admin items for creator", () => {
    useAuthStore.getState().login("creator");
    render(
      <MemoryRouter>
        <Sidebar />
      </MemoryRouter>,
    );

    expect(screen.getByText("Trang chủ")).toBeInTheDocument();
    expect(screen.getByText("Dự án")).toBeInTheDocument();
    expect(screen.getByText("Nguồn dữ liệu")).toBeInTheDocument();
    expect(screen.getByText("Nhãn dữ liệu")).toBeInTheDocument();
    expect(screen.queryByText("Người dùng")).not.toBeInTheDocument();
    expect(screen.queryByText("Nhóm")).not.toBeInTheDocument();
    expect(screen.queryByText("Cài đặt")).not.toBeInTheDocument();
  });

  it("hides creator and admin items for explorer", () => {
    useAuthStore.getState().login("explorer");
    render(
      <MemoryRouter>
        <Sidebar />
      </MemoryRouter>,
    );

    expect(screen.getByText("Trang chủ")).toBeInTheDocument();
    expect(screen.getByText("Khám phá")).toBeInTheDocument();
    expect(screen.getByText("Dự án")).toBeInTheDocument();
    expect(screen.queryByText("Nguồn dữ liệu")).not.toBeInTheDocument();
    expect(screen.queryByText("Người dùng")).not.toBeInTheDocument();
  });

  it("shows only viewer items for viewer", () => {
    useAuthStore.getState().login("viewer");
    render(
      <MemoryRouter>
        <Sidebar />
      </MemoryRouter>,
    );

    expect(screen.getByText("Trang chủ")).toBeInTheDocument();
    expect(screen.getByText("Dự án")).toBeInTheDocument();
    expect(screen.queryByText("Khám phá")).not.toBeInTheDocument();
    expect(screen.queryByText("Nguồn dữ liệu")).not.toBeInTheDocument();
    expect(screen.queryByText("Người dùng")).not.toBeInTheDocument();
  });
});
