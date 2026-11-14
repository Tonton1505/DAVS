import type { ReactNode } from "react";
import { Navigate, Outlet, useLocation } from "react-router";
import type { Role } from "../../common/constants/types";
import { roleCan } from "../../common/permission-config/role-permissions";
import { useAuthStore } from "../../stores/auth.store";

const ROLE_ORDER: Record<Role, number> = {
  viewer: 0,
  explorer: 1,
  creator: 2,
  admin: 3,
};

export function RequireAuth({ children }: { children?: ReactNode }) {
  const role = useAuthStore((s) => s.role);
  const location = useLocation();
  if (!role) {
    return <Navigate to="/login" state={{ from: location }} replace />;
  }
  return children ? <>{children}</> : <Outlet />;
}

export function RequireRole({
  minRole,
  allowedRoles,
  requiredUc,
  children,
}: {
  minRole?: Role;
  allowedRoles?: readonly Role[];
  requiredUc?: string;
  children?: ReactNode;
}) {
  const role = useAuthStore((s) => s.role);
  if (!role) {
    return <Navigate to="/login" replace />;
  }
  const hasMinRole = minRole ? ROLE_ORDER[role] >= ROLE_ORDER[minRole] : true;
  const inAllowed = allowedRoles ? allowedRoles.includes(role) : true;
  const hasUc = requiredUc ? roleCan(role, requiredUc) : true;
  if (!hasMinRole || !inAllowed || !hasUc) {
    return <Navigate to="/403" replace />;
  }
  return children ? <>{children}</> : <Outlet />;
}

export const RoleProtectedRoute = RequireRole;
