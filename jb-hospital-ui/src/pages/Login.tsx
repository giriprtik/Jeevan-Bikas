import { useState, type FormEvent } from 'react';
import { Navigate, useLocation, useNavigate } from 'react-router-dom';
import { ArrowRight, ClipboardList, FlaskConical, HeartPulse, Lock, Stethoscope, User } from 'lucide-react';
import { HOME, useAuth } from '../context/AuthContext';
import { apiError } from '../lib/api';
import { Alert, Button, Field, Input } from '../components/ui';

const DEMO = [
  { username: 'nurse', label: 'Nurse', note: 'Registration & triage' },
  { username: 'doctor', label: 'Doctor', note: 'Dr. Ramesh Sharma' },
  { username: 'drkarki', label: 'Doctor 2', note: 'Dr. Anjali Karki' },
  { username: 'labtech', label: 'Lab tech', note: 'Results entry' },
  { username: 'admin', label: 'Admin', note: 'Staff accounts' },
];
const DEMO_PASSWORD = 'Demo@1234';

const FLOW = [
  { icon: ClipboardList, label: 'Register', text: 'Token issued, doctor assigned' },
  { icon: HeartPulse, label: 'Triage', text: 'Vitals flagged automatically' },
  { icon: Stethoscope, label: 'Consult', text: 'Diagnosis or lab order' },
  { icon: FlaskConical, label: 'Lab', text: 'Results back to the doctor' },
];

export default function Login() {
  const { user, login } = useAuth();
  const navigate = useNavigate();
  const location = useLocation();
  const expired = new URLSearchParams(location.search).has('expired');

  const [username, setUsername] = useState('');
  const [password, setPassword] = useState('');
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState<string | null>(null);

  if (user) return <Navigate to={HOME[user.role]} replace />;

  const submit = async (e: FormEvent) => {
    e.preventDefault();
    setBusy(true);
    setError(null);
    try {
      const u = await login(username.trim(), password);
      const from = (location.state as { from?: string } | null)?.from;
      navigate(from && from !== '/login' ? from : HOME[u.role], { replace: true });
    } catch (err) {
      setError(apiError(err, 'Sign-in failed'));
      setBusy(false);
    }
  };

  return (
    <div className="min-h-screen grid lg:grid-cols-[1.05fr_1fr] bg-surface">
      {/* Brand panel */}
      <div className="relative hidden lg:flex flex-col justify-between overflow-hidden bg-brand-900 text-white p-12">
        <div
          aria-hidden
          className="absolute inset-0 opacity-[0.07]"
          style={{ backgroundImage: 'linear-gradient(#fff 1px, transparent 1px), linear-gradient(90deg, #fff 1px, transparent 1px)', backgroundSize: '32px 32px' }}
        />
        <div className="relative flex items-center gap-3">
          <img src="/favicon.svg" alt="" className="size-10 rounded-lg ring-1 ring-white/20" />
          <div>
            <p className="font-semibold text-[17px]">Jeevan Bikas Hospital</p>
            <p className="text-sm text-brand-200">Outpatient &amp; Laboratory System</p>
          </div>
        </div>

        <div className="relative max-w-md">
          <h1 className="text-[34px] leading-[1.15] font-semibold tracking-tight">
            One patient record, from the front desk to the lab report.
          </h1>
          <ol className="mt-10 space-y-5">
            {FLOW.map(({ icon: Icon, label, text }, i) => (
              <li key={label} className="flex items-center gap-4">
                <span className="size-10 rounded-lg bg-white/10 ring-1 ring-white/15 grid place-items-center">
                  <Icon className="size-5 text-brand-200" strokeWidth={1.75} />
                </span>
                <div>
                  <p className="text-[15px] font-medium"><span className="font-mono text-brand-200 mr-2">0{i + 1}</span>{label}</p>
                  <p className="text-sm text-white/60">{text}</p>
                </div>
              </li>
            ))}
          </ol>
        </div>

        <p className="relative text-xs text-white/40">Spring Boot microservices · API gateway · React</p>
      </div>

      {/* Form */}
      <div className="flex items-center justify-center p-6 sm:p-12">
        <div className="w-full max-w-sm">
          <div className="lg:hidden flex items-center gap-2.5 mb-8">
            <img src="/favicon.svg" alt="" className="size-9" />
            <p className="font-semibold">Jeevan Bikas Hospital</p>
          </div>
          <h2 className="text-2xl font-semibold tracking-tight">Sign in</h2>
          <p className="text-sm text-ink-3 mt-1">Use your hospital staff account.</p>

          <div className="mt-6 space-y-3">
            {expired && !error && <Alert kind="info">Your session expired. Please sign in again.</Alert>}
            {error && <Alert>{error}</Alert>}
          </div>

          <form onSubmit={submit} className="mt-6 space-y-4">
            <Field label="Username" htmlFor="username">
              <div className="relative">
                <User className="size-4 text-ink-4 absolute left-3 top-1/2 -translate-y-1/2" />
                <Input id="username" autoComplete="username" autoFocus required value={username}
                  onChange={(e) => setUsername(e.target.value)} disabled={busy} className="pl-9" />
              </div>
            </Field>
            <Field label="Password" htmlFor="password">
              <div className="relative">
                <Lock className="size-4 text-ink-4 absolute left-3 top-1/2 -translate-y-1/2" />
                <Input id="password" type="password" autoComplete="current-password" required value={password}
                  onChange={(e) => setPassword(e.target.value)} disabled={busy} className="pl-9" />
              </div>
            </Field>
            <Button type="submit" loading={busy} className="w-full" icon={!busy && <ArrowRight className="size-4" />}>
              Sign in
            </Button>
          </form>

          <div className="mt-10">
            <p className="text-xs font-medium uppercase tracking-wide text-ink-4">Demo accounts</p>
            <p className="text-xs text-ink-3 mt-1">Password <span className="font-mono text-ink-2">{DEMO_PASSWORD}</span>. Click to fill.</p>
            <div className="mt-3 grid grid-cols-2 gap-2">
              {DEMO.map((d) => (
                <button key={d.username} type="button" disabled={busy}
                  onClick={() => { setUsername(d.username); setPassword(DEMO_PASSWORD); setError(null); }}
                  className="text-left rounded-lg border border-line px-3 py-2 hover:border-brand-200 hover:bg-brand-50 transition-colors">
                  <p className="text-[13px] font-medium text-ink">{d.label} <span className="font-mono text-ink-4 font-normal">· {d.username}</span></p>
                  <p className="text-[11.5px] text-ink-3">{d.note}</p>
                </button>
              ))}
            </div>
          </div>
        </div>
      </div>
    </div>
  );
}
