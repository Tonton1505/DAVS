import { render, screen } from "@testing-library/react";
import { MemoryRouter, Route, Routes } from "react-router";
import { beforeEach, describe, expect, it } from "vitest";
import { useAuthStore } from "../../../stores/auth.store";
import { RequireAuth, RequireRole } from "../RoleProtectedRoute";

describe("RequireAuth and RequireRole route guards", () => {
  beforeEach(() => {
    useAuthStore.getState().logout();
  });

  it("redirects unauthenticated users to /login", () => {
    render(
      <MemoryRouter initialEntries={["/protected"]}>
        <Routes>
          <Route path="/login" element={<div>Login Page</div>} />
          <Route
            path="/protected"
            element={
              <RequireAuth>
                <div>Secret Content</div>
              </RequireAuth>
            }
          />
        </Routes>
      </MemoryRouter>,
    );

    expect(screen.getByText("Login Page")).toBeInTheDocument();
    expect(screen.queryByText("Secret Content")).not.toBeInTheDocument();
  });

  it("redirects insufficient role to /403 and allows authorized role", () => {
    useAuthStore.getState().login("viewer");
    const { unmount } = render(
      <MemoryRouter initialEntries={["/admin"]}>
        <Routes>
          <Route path="/403" element={<div>Forbidden 403</div>} />
          <Route
            path="/admin"
            element={
              <RequireRole minRole="admin">
                <div>Admin Panel</div>
              </RequireRole>
            }
          />
        </Routes>
      </MemoryRouter>,
    );
    expect(screen.getByText("Forbidden 403")).toBeInTheDocument();
    unmount();

    useAuthStore.getState().login("admin");
    render(
      <MemoryRouter initialEntries={["/admin"]}>
        <Routes>
          <Route path="/403" element={<div>Forbidden 403</div>} />
          <Route
            path="/admin"
            element={
              <RequireRole minRole="admin">
                <div>Admin Panel</div>
              </RequireRole>
            }
          />
        </Routes>
      </MemoryRouter>,
    );
    expect(screen.getByText("Admin Panel")).toBeInTheDocument();
  });
});
