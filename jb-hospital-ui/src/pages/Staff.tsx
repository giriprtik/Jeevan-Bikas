import { useState, type FormEvent } from 'react';
import { Stethoscope, UserPlus } from 'lucide-react';
import { AuthApi } from '../lib/services';
import { apiError } from '../lib/api';
import { usePolling } from '../hooks/usePolling';
import { ROLE_LABEL } from '../lib/format';
import type { Role } from '../lib/types';
import { Alert, Button, Card, CardHeader, EmptyState, Field, Input, PageHeader, Select, Spinner } from '../components/ui';

const empty = { username: '', fullName: '', email: '', password: '', role: 'NURSE' as Role, department: '', specialization: '', licenseNumber: '' };

export default function Staff() {
  const doctors = usePolling(() => AuthApi.doctors(), [], 0);
  const [f, setF] = useState(empty);
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [ok, setOk] = useState<string | null>(null);
  const set = (k: keyof typeof empty) => (e: { target: { value: string } }) => setF({ ...f, [k]: e.target.value });

  const submit = async (e: FormEvent) => {
    e.preventDefault();
    setBusy(true); setError(null); setOk(null);
    try {
      await AuthApi.register({ ...f, department: f.department || undefined, specialization: f.specialization || undefined, licenseNumber: f.licenseNumber || undefined });
      setOk(`${f.fullName} can now sign in as “${f.username}”.`);
      setF(empty);
      doctors.reload();
    } catch (err) { setError(apiError(err, "Couldn't create the account")); }
    finally { setBusy(false); }
  };

  return (
    <>
      <PageHeader title="Staff accounts" subtitle="Only administrators can create accounts; there is no public sign-up." />
      <div className="grid xl:grid-cols-[minmax(0,7fr)_minmax(0,5fr)] gap-6 items-start">
        <Card>
          <CardHeader icon={<UserPlus className="size-[18px]" />} title="New staff account" />
          <form onSubmit={submit} className="p-5 space-y-4">
            {error && <Alert>{error}</Alert>}
            {ok && <Alert kind="success" onClose={() => setOk(null)}>{ok}</Alert>}
            <fieldset disabled={busy} className="space-y-4">
              <div className="grid sm:grid-cols-2 gap-4">
                <Field label="Full name" required htmlFor="s-name"><Input id="s-name" required value={f.fullName} onChange={set('fullName')} placeholder="Dr. Priya Shrestha" /></Field>
                <Field label="Role" required htmlFor="s-role">
                  <Select id="s-role" value={f.role} onChange={set('role')}>
                    {(Object.keys(ROLE_LABEL) as Role[]).map((r) => <option key={r} value={r}>{ROLE_LABEL[r]}</option>)}
                  </Select>
                </Field>
                <Field label="Username" required htmlFor="s-user" hint="letters, digits . _ -"><Input id="s-user" required pattern="[A-Za-z0-9._\-]{3,50}" value={f.username} onChange={set('username')} /></Field>
                <Field label="Email" required htmlFor="s-mail"><Input id="s-mail" type="email" required value={f.email} onChange={set('email')} /></Field>
                <Field label="Temporary password" required htmlFor="s-pw" hint="min 8 characters"><Input id="s-pw" type="password" required minLength={8} value={f.password} onChange={set('password')} /></Field>
                <Field label="Department" htmlFor="s-dept"><Input id="s-dept" value={f.department} onChange={set('department')} placeholder="GENERAL_OPD" /></Field>
                {f.role === 'DOCTOR' && <>
                  <Field label="Specialisation" htmlFor="s-spec"><Input id="s-spec" value={f.specialization} onChange={set('specialization')} placeholder="Paediatrics" /></Field>
                  <Field label="NMC licence no." htmlFor="s-lic"><Input id="s-lic" value={f.licenseNumber} onChange={set('licenseNumber')} /></Field>
                </>}
              </div>
            </fieldset>
            <div className="flex justify-end"><Button type="submit" loading={busy} icon={<UserPlus className="size-4" />}>Create account</Button></div>
          </form>
        </Card>
        <Card>
          <CardHeader icon={<Stethoscope className="size-[18px]" />} title="Doctors on duty" subtitle="Available in the registration doctor picker" />
          {doctors.loading ? <Spinner /> : doctors.error ? <div className="p-4"><Alert>{doctors.error}</Alert></div> : !doctors.data?.length ? (
            <EmptyState icon={<Stethoscope className="size-5" />} title="No doctors yet" />
          ) : (
            <ul className="divide-y divide-line">
              {doctors.data.map((d) => (
                <li key={d.username} className="px-5 py-3">
                  <p className="text-[13.5px] font-medium">{d.fullName}</p>
                  <p className="text-xs text-ink-3"><span className="font-mono">{d.username}</span>{d.specialization && ` · ${d.specialization}`}{d.department && ` · ${d.department}`}</p>
                </li>
              ))}
            </ul>
          )}
        </Card>
      </div>
    </>
  );
}
