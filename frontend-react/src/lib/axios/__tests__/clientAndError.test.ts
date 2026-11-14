import { AxiosError, AxiosHeaders, type AxiosResponse } from "axios";
import { beforeEach, describe, expect, it, vi } from "vitest";
import { useAuthStore } from "../../../stores/auth.store";
import client, { applyApiError, authRedirect } from "../client";

describe("HTTP client and applyApiError", () => {
  beforeEach(() => {
    useAuthStore.getState().logout();
    vi.restoreAllMocks();
  });

  it("applies fieldErrors and toast message via applyApiError", () => {
    const setFieldError = vi.fn();
    const onToast = vi.fn();
    const err = {
      response: {
        data: {
          code: "ERR-VAL-01",
          message: "Dữ liệu không hợp lệ",
          fieldErrors: [{ field: "email", message: "Email không đúng định dạng" }],
        },
      },
    };

    const msg = applyApiError(err, { setFieldError, onToast });
    expect(msg).toBe("Dữ liệu không hợp lệ");
    expect(setFieldError).toHaveBeenCalledWith("email", "Email không đúng định dạng");
    expect(onToast).toHaveBeenCalledWith("Dữ liệu không hợp lệ");
  });

  it("attaches Bearer token and redirects ERR-AUTH-05 to /login", async () => {
    useAuthStore.getState().setSession({
      accessToken: "jwt-123",
      refreshToken: "ref-456",
      role: "creator",
    });
    const toLoginSpy = vi.spyOn(authRedirect, "toLogin").mockImplementation(() => {});

    let seenAuthHeader: string | undefined;
    client.defaults.adapter = async (config) => {
      seenAuthHeader = String(config.headers?.Authorization ?? "");
      const headers = new AxiosHeaders();
      const response = {
        status: 401,
        statusText: "Unauthorized",
        headers,
        config,
        data: { code: "ERR-AUTH-05", message: "Phiên làm việc đã hết hạn" },
      } as AxiosResponse;
      throw new AxiosError("Unauthorized", "ERR_BAD_REQUEST", config, null, response);
    };

    await expect(client.get("/v1/projects")).rejects.toThrow();
    expect(seenAuthHeader).toBe("Bearer jwt-123");
    expect(useAuthStore.getState().accessToken).toBeNull();
    expect(toLoginSpy).toHaveBeenCalledTimes(1);
  });
});
