import { createContext, useContext, useState, type ReactNode } from 'react';
import { TOKEN_KEY, USER_KEY } from '../lib/api';
import { AuthApi } from '../lib/services';
import type { Role, SessionUser } from '../lib/types';

interface AuthState {
  user: SessionUser | null;
  login: (username: string, password: string) => Promise<SessionUser>;
  logout: () => void;
  hasRole: (...roles: Role[]) => boolean;
}

const AuthContext = createContext<AuthState | null>(null);

/** Restores a session only if the stored token has not expired. */
function restore(): SessionUser | null {
  try {
    const token = localStorage.getItem(TOKEN_KEY);
    const raw = localStorage.getItem(USER_KEY);
    if (!token || !raw) return null;
    const { exp } = JSON.parse(atob(token.split('.')[1].replace(/-/g, '+').replace(/_/g, '/')));
    if (typeof exp === 'number' && exp * 1000 < Date.now()) throw new Error('expired');
    return JSON.parse(raw) as SessionUser;
  } catch {
    localStorage.removeItem(TOKEN_KEY);
    localStorage.removeItem(USER_KEY);
    return null;
  }
}

export function AuthProvider({ children }: { children: ReactNode }) {
  const [user, setUser] = useState<SessionUser | null>(restore);

  const login = async (username: string, password: string) => {
    const res = await AuthApi.login(username, password);
    const u: SessionUser = { username: res.username, fullName: res.fullName, role: res.role };
    localStorage.setItem(TOKEN_KEY, res.accessToken);
    localStorage.setItem(USER_KEY, JSON.stringify(u));
    setUser(u);
    return u;
  };

  const logout = () => {
    localStorage.removeItem(TOKEN_KEY);
    localStorage.removeItem(USER_KEY);
    setUser(null);
  };

  const hasRole = (...roles: Role[]) => !!user && roles.includes(user.role);

  return <AuthContext.Provider value={{ user, login, logout, hasRole }}>{children}</AuthContext.Provider>;
}

// eslint-disable-next-line react-refresh/only-export-components
export function useAuth() {
  const ctx = useContext(AuthContext);
  if (!ctx) throw new Error('useAuth must be used inside <AuthProvider>');
  return ctx;
}

/** Where each role lands after sign-in. */
// eslint-disable-next-line react-refresh/only-export-components
export const HOME: Record<Role, string> = {
  ADMIN: '/registration',
  NURSE: '/registration',
  DOCTOR: '/doctor',
  LABTECH: '/lab',
};
