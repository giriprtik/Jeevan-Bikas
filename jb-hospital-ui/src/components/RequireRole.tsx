import { Navigate, Outlet, useLocation } from 'react-router-dom';
import { useAuth } from '../context/AuthContext';
import type { Role } from '../lib/types';

export function RequireRole({ roles }: { roles?: Role[] }) {
  const { user, hasRole } = useAuth();
  const location = useLocation();
  if (!user) return <Navigate to="/login" replace state={{ from: location.pathname }} />;
  if (roles && !hasRole(...roles)) return <Navigate to="/forbidden" replace />;
  return <Outlet />;
}
