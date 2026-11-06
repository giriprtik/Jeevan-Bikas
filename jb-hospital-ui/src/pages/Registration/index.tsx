import { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { CheckCircle2, ClipboardList, HeartPulse, RefreshCw, UserPlus, Users } from 'lucide-react';
import { OpdApi } from '../../lib/services';
import { usePolling } from '../../hooks/usePolling';
import { useFlash } from '../../hooks/useFlash';
import type { QueueItem } from '../../lib/types';
import { Alert, Button, Card, CardHeader, EmptyState, PageHeader, PriorityBadge, Spinner, StatusBadge, Token, cx } from '../../components/ui';
import { StatStrip } from '../../components/StatStrip';
import { NewPatientForm } from './NewPatientForm';
import { ReturningPatientForm } from './ReturningPatientForm';

type Mode = 'new' | 'returning';
interface Issued { visitId: number; token: number; name: string; mrn: string; doctor?: string | null }

export default function Registration() {
  const navigate = useNavigate();
  const [mode, setMode] = useState<Mode>('new');
  const [issued, setIssued] = useState<Issued | null>(null);
  const [formKey, setFormKey] = useState(0);
  const today = usePolling(() => OpdApi.today(), [], 20000);
  const [flash, clearFlash] = useFlash();

  const done = (i: Issued) => { setIssued(i); today.reload(); };
  const another = () => { setIssued(null); setFormKey((k) => k + 1); };

  return (
    <>
      <PageHeader title="Registration" subtitle="Register the patient, assign a doctor and issue today's token." />
      {flash && <div className="mb-4"><Alert kind="success" onClose={clearFlash}>{flash}</Alert></div>}
      <StatStrip items={today.data} />

      <div className="grid xl:grid-cols-[minmax(0,7fr)_minmax(0,5fr)] gap-6 items-start">
        <Card>
          {issued ? (
            <div className="p-8 text-center animate-fade-up">
              <CheckCircle2 className="size-10 text-emerald-600 mx-auto" strokeWidth={1.5} />
              <p className="text-lg font-semibold mt-3">{issued.name} is registered</p>
              <p className="text-sm text-ink-3 mt-1 font-mono">{issued.mrn}</p>
              <div className="mt-6 inline-flex items-center gap-4 rounded-xl border border-line bg-subtle px-6 py-4">
                <div className="text-left">
                  <p className="text-xs text-ink-3">Token</p>
                  <p className="text-4xl font-semibold font-mono text-brand-700 tabular-nums">{String(issued.token).padStart(2, '0')}</p>
                </div>
                {issued.doctor && (
                  <div className="text-left border-l border-line pl-4">
                    <p className="text-xs text-ink-3">Doctor</p>
                    <p className="text-sm font-medium">{issued.doctor}</p>
                  </div>
                )}
              </div>
              <div className="mt-7 flex flex-wrap justify-center gap-2">
                <Button icon={<HeartPulse className="size-4" />} onClick={() => navigate(`/triage/${issued.visitId}`, { state: { from: '/registration' } })}>
                  Record vitals now
                </Button>
                <Button variant="secondary" icon={<UserPlus className="size-4" />} onClick={another}>Register another</Button>
              </div>
            </div>
          ) : (
            <>
              <div className="flex border-b border-line px-2" role="tablist">
                {([['new', 'New patient', UserPlus], ['returning', 'Returning patient', Users]] as const).map(([m, label, Icon]) => (
                  <button key={m} role="tab" aria-selected={mode === m} onClick={() => setMode(m)}
                    className={cx('flex items-center gap-2 h-12 px-4 -mb-px border-b-2 text-[13.5px] font-medium transition-colors',
                      mode === m ? 'border-brand-600 text-brand-700' : 'border-transparent text-ink-3 hover:text-ink')}>
                    <Icon className="size-4" /> {label}
                  </button>
                ))}
              </div>
              {mode === 'new'
                ? <NewPatientForm key={`n${formKey}`} onRegistered={(r) => done({ visitId: r.visitId, token: r.tokenNumber, name: r.fullName, mrn: r.mrn })} />
                : <ReturningPatientForm key={`r${formKey}`} onOpened={(v) => done({ visitId: v.visitId, token: v.tokenNumber, name: v.patientFullName, mrn: v.patientMrn, doctor: v.assignedDoctorName })} />}
            </>
          )}
        </Card>

        <TodayRegister items={today.data} loading={today.loading} error={today.error} refreshing={today.refreshing}
          onRefresh={today.reload} onOpen={(i) => navigate(`/triage/${i.visitId}`, { state: { patient: i, from: '/registration' } })} />
      </div>
    </>
  );
}

function TodayRegister({ items, loading, error, refreshing, onRefresh, onOpen }: {
  items: QueueItem[] | null; loading: boolean; error: string | null; refreshing: boolean;
  onRefresh: () => void; onOpen: (i: QueueItem) => void;
}) {
  return (
    <Card className="xl:sticky xl:top-24">
      <CardHeader icon={<ClipboardList className="size-[18px]" />} title="Today's register" subtitle="Updates automatically"
        actions={<Button variant="ghost" size="sm" onClick={onRefresh} aria-label="Refresh"
          icon={<RefreshCw className={cx('size-4', refreshing && 'animate-spin')} />} />} />
      {loading ? <Spinner /> : error ? (
        <div className="p-4"><Alert action={<Button size="sm" variant="secondary" onClick={onRefresh}>Retry</Button>}>{error}</Alert></div>
      ) : !items?.length ? (
        <EmptyState icon={<ClipboardList className="size-5" />} title="No patients yet today">Registered patients appear here with their live status.</EmptyState>
      ) : (
        <ul className="divide-y divide-line max-h-[640px] overflow-y-auto">
          {items.map((i) => {
            const clickable = i.status === 'REGISTERED';
            const Row = clickable ? 'button' : 'div';
            return (
              <li key={i.visitId}>
                <Row {...(clickable ? { onClick: () => onOpen(i), type: 'button' as const } : {})}
                  className={cx('w-full text-left flex items-center gap-3 px-5 py-3', clickable && 'hover:bg-subtle')}>
                  <Token n={i.tokenNumber} />
                  <div className="min-w-0 flex-1">
                    <p className="text-[13.5px] font-medium text-ink truncate">{i.patientFullName}</p>
                    <p className="text-xs text-ink-3 truncate">
                      <span className="font-mono">{i.registeredTime}</span> · {i.assignedDoctorName ?? 'Unassigned'}
                    </p>
                  </div>
                  <div className="flex flex-col items-end gap-1">
                    <StatusBadge status={i.status} />
                    <PriorityBadge priority={i.priority} quiet />
                  </div>
                </Row>
              </li>
            );
          })}
        </ul>
      )}
    </Card>
  );
}
