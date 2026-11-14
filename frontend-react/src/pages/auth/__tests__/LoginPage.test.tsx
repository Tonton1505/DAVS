import { beforeEach, describe, expect, it, vi } from "vitest";
import { fireEvent, render, screen, waitFor } from "@testing-library/react";
import { MemoryRouter } from "react-router";
import LoginPage from "../LoginPage";
import { useAuthStore } from "../../../stores/auth.store";
import { authApi } from "../../../services/auth.service";

const mockNavigate = vi.fn();

vi.mock("react-router", async () => {
  const actual = await vi.importActual<typeof import("react-router")>("react-router");
  return {
    ...actual,
    useNavigate: () => mockNavigate,
  };
});

vi.mock("../../../services/auth.service", async () => {
  const actual = await vi.importActual<typeof import("../../../services/auth.service")>(
    "../../../services/auth.service",
  );
  return {
    ...actual,
    authApi: {
      login: vi.fn(),
      acceptTerms: vi.fn(),
      refresh: vi.fn(),
      logout: vi.fn(),
      me: vi.fn(),
    },
  };
});

describe("LoginPage (UC01)", () => {
  beforeEach(() => {
    vi.clearAllMocks();
    useAuthStore.getState().logout();
  });

  it("validates email and password length before calling API", () => {
    render(
      <MemoryRouter>
        <LoginPage />
      </MemoryRouter>,
    );

    fireEvent.change(screen.getByLabelText(/Email/i), { target: { value: "invalid-email" } });
    fireEvent.change(screen.getByLabelText(/^Mật khẩu/i), { target: { value: "123" } });
    fireEvent.click(screen.getByRole("button", { name: "Đăng nhập" }));

    expect(screen.getByRole("alert")).toHaveTextContent(/Email không đúng định dạng/i);
    expect(authApi.login).not.toHaveBeenCalled();

    fireEvent.change(screen.getByLabelText(/Email/i), { target: { value: "user@davs.local" } });
    fireEvent.click(screen.getByRole("button", { name: "Đăng nhập" }));

    expect(screen.getByRole("alert")).toHaveTextContent(/8 đến 64 ký tự/i);
    expect(authApi.login).not.toHaveBeenCalled();
  });

  it("logs in active user, stores session, and navigates home", async () => {
    vi.mocked(authApi.login).mockResolvedValue({
      accessToken: "jwt-token-123",
      expiresInSeconds: 900,
      requiresTermsAcceptance: false,
      mustChangePassword: false,
      user: {
        id: "u1",
        email: "creator@davs.local",
        fullName: "Creator User",
        siteRole: "CREATOR",
        status: "ACTIVE",
        termsAccepted: true,
        mustChangePassword: false,
      },
    });

    render(
      <MemoryRouter>
        <LoginPage />
      </MemoryRouter>,
    );

    fireEvent.change(screen.getByLabelText(/Email/i), { target: { value: "creator@davs.local" } });
    fireEvent.change(screen.getByLabelText(/^Mật khẩu/i), { target: { value: "StrongPass!123" } });
    fireEvent.click(screen.getByLabelText("Ghi nhớ đăng nhập"));
    fireEvent.click(screen.getByRole("button", { name: "Đăng nhập" }));

    await waitFor(() => {
      expect(authApi.login).toHaveBeenCalledWith({
        email: "creator@davs.local",
        password: "StrongPass!123",
        rememberMe: true,
      });
    });

    await waitFor(() => {
      expect(mockNavigate).toHaveBeenCalledWith("/");
    });
    expect(useAuthStore.getState().role).toBe("creator");
    expect(useAuthStore.getState().accessToken).toBe("jwt-token-123");
  });

  it("shows terms dialog on first login and activates account on accept", async () => {
    vi.mocked(authApi.login).mockResolvedValue({
      accessToken: "pending-jwt",
      expiresInSeconds: 900,
      requiresTermsAcceptance: true,
      mustChangePassword: false,
      user: {
        id: "u2",
        email: "pending@davs.local",
        fullName: "Pending User",
        siteRole: "EXPLORER",
        status: "PENDING",
        termsAccepted: false,
        mustChangePassword: false,
      },
    });
    vi.mocked(authApi.acceptTerms).mockResolvedValue({
      id: "u2",
      email: "pending@davs.local",
      fullName: "Pending User",
      siteRole: "EXPLORER",
      status: "ACTIVE",
      termsAccepted: true,
      mustChangePassword: false,
    });

    render(
      <MemoryRouter>
        <LoginPage />
      </MemoryRouter>,
    );

    fireEvent.change(screen.getByLabelText(/Email/i), { target: { value: "pending@davs.local" } });
    fireEvent.change(screen.getByLabelText(/^Mật khẩu/i), { target: { value: "StrongPass!123" } });
    fireEvent.click(screen.getByRole("button", { name: "Đăng nhập" }));

    await screen.findByRole("dialog");
    const activateBtn = screen.getByRole("button", { name: "Kích hoạt tài khoản" });
    expect(activateBtn).toBeDisabled();

    fireEvent.click(screen.getByLabelText(/Tôi đã đọc và đồng ý điều khoản/i));
    expect(activateBtn).toBeEnabled();
    fireEvent.click(activateBtn);

    await waitFor(() => {
      expect(authApi.acceptTerms).toHaveBeenCalledWith(true);
      expect(mockNavigate).toHaveBeenCalledWith("/");
    });
    expect(useAuthStore.getState().role).toBe("explorer");
  });

  it("shows ERR-AUTH-01 message when login fails", async () => {
    vi.mocked(authApi.login).mockRejectedValue({
      isAxiosError: true,
      response: {
        status: 401,
        data: {
          code: "ERR-AUTH-01",
          message: "Email hoặc mật khẩu không đúng. Vui lòng kiểm tra lại.",
        },
      },
    });

    render(
      <MemoryRouter>
        <LoginPage />
      </MemoryRouter>,
    );

    fireEvent.change(screen.getByLabelText(/Email/i), { target: { value: "user@davs.local" } });
    fireEvent.change(screen.getByLabelText(/^Mật khẩu/i), { target: { value: "WrongPass123" } });
    fireEvent.click(screen.getByRole("button", { name: "Đăng nhập" }));

    await waitFor(() => {
      expect(screen.getByRole("alert")).toHaveTextContent(
        "Email hoặc mật khẩu không đúng. Vui lòng kiểm tra lại.",
      );
    });
  });
});
