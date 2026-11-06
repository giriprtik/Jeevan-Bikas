import { useEffect, useState } from 'react';
import { NavLink, Outlet, useNavigate } from 'react-router-dom';
import { ClipboardList, FlaskConical, HeartPulse, LogOut, Stethoscope, UserCog } from 'lucide-react';
import { useAuth } from '../context/AuthContext';
import { ROLE_LABEL, initials } from '../lib/format';
import type { Role } from '../lib/types';
import { cx } from './ui';

const NAV: { to: string; label: string; icon: typeof ClipboardList; roles: Role[] }[] = [
  { to: '/registration', label: 'Registration', icon: ClipboardList, roles: ['NURSE', 'ADMIN'] },
  { to: '/triage', label: 'Triage', icon: HeartPulse, roles: ['NURSE', 'ADMIN'] },
  { to: '/doctor', label: 'My patients', icon: Stethoscope, roles: ['DOCTOR'] },
  { to: '/lab', label: 'Laboratory', icon: FlaskConical, roles: ['LABTECH', 'ADMIN'] },
  { to: '/staff', label: 'Staff', icon: UserCog, roles: ['ADMIN'] },
];

function Clock() {
  const [now, setNow] = useState(() => new Date());
  useEffect(() => {
    const id = setInterval(() => setNow(new Date()), 30_000);
    return () => clearInterval(id);
  }, []);
  const fmt = new Intl.DateTimeFormat('en-GB', {
    weekday: 'short', day: 'numeric', month: 'short', hour: '2-digit', minute: '2-digit', timeZone: 'Asia/Kathmandu',
  });
  return <span className="tabular-nums">{fmt.format(now)} NPT</span>;
}

export function AppShell() {
  const { user, logout, hasRole } = useAuth();
  const navigate = useNavigate();
  if (!user) return null;

  return (
    <div className="min-h-screen flex">
      <aside className="w-60 shrink-0 bg-surface border-r border-line flex flex-col sticky top-0 h-screen max-lg:w-16">
        <div className="h-16 flex items-center gap-2.5 px-5 border-b border-line max-lg:px-3 max-lg:justify-center">
          <img src="/favicon.svg" alt="" className="size-8" />
          <div className="leading-tight max-lg:hidden">
            <p className="text-[14px] font-semibold text-ink">Jeevan Bikas</p>
            <p className="text-[11px] text-ink-3">Hospital · OPD</p>
          </div>
        </div>

        <nav className="flex-1 p-3 space-y-0.5" aria-label="Main">
          {NAV.filter((n) => hasRole(...n.roles)).map(({ to, label, icon: Icon }) => (
            <NavLink
              key={to}
              to={to}
              title={label}
              className={({ isActive }) =>
                cx(
                  'flex items-center gap-3 h-9 px-3 rounded-lg text-[13.5px] font-medium transition-colors max-lg:justify-center max-lg:px-0',
                  isActive ? 'bg-brand-50 text-brand-700' : 'text-ink-3 hover:bg-subtle hover:text-ink',
                )
              }
            >
              <Icon className="size-[18px] shrink-0" strokeWidth={1.75} />
              <span className="max-lg:hidden">{label}</span>
            </NavLink>
          ))}
        </nav>

        <div className="p-3 border-t border-line">
          <div className="flex items-center gap-2.5 px-2 py-2 max-lg:justify-center max-lg:px-0">
            <span className="size-8 rounded-full bg-brand-600 text-white grid place-items-center text-xs font-semibold shrink-0">
              {initials(user.fullName || user.username)}
            </span>
            <div className="min-w-0 max-lg:hidden">
              <p className="text-[13px] font-medium text-ink truncate">{user.fullName || user.username}</p>
              <p className="text-[11px] text-ink-3">{ROLE_LABEL[user.role]}</p>
            </div>
          </div>
          <button
            onClick={() => { logout(); navigate('/login'); }}
            className="mt-1 w-full flex items-center gap-3 h-9 px-3 rounded-lg text-[13px] text-ink-3 hover:bg-subtle hover:text-ink max-lg:justify-center max-lg:px-0"
            title="Sign out"
          >
            <LogOut className="size-[18px]" strokeWidth={1.75} />
            <span className="max-lg:hidden">Sign out</span>
          </button>
        </div>
      </aside>

      <div className="flex-1 min-w-0 flex flex-col">
        <header className="h-16 shrink-0 bg-surface/80 backdrop-blur border-b border-line flex items-center justify-end px-6 lg:px-8 text-[13px] text-ink-3 sticky top-0 z-10">
          <Clock />
        </header>
        <main className="flex-1 px-6 lg:px-8 py-7">
          <div className="max-w-[1400px] mx-auto">
            <Outlet />
          </div>
        </main>
      </div>
    </div>
  );
}
