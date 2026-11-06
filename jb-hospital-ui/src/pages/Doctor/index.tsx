import { useState } from 'react';
import { FlaskConical, RefreshCw, Stethoscope, UserRound } from 'lucide-react';
import { OpdApi } from '../../lib/services';
import { apiError } from '../../lib/api';
import { usePolling } from '../../hooks/usePolling';
import { useAuth } from '../../context/AuthContext';
import { ageSex } from '../../lib/format';
import type { QueueItem, VisitStatus } from '../../lib/types';
import { Alert, Badge, Button, Card, EmptyState, PageHeader, PriorityBadge, Spinner, StatusBadge, Token, cx } from '../../components/ui';
import { Consultation } from './Consultation';

type Tab = 'queue' | 'lab' | 'done';
const TABS: { id: Tab; label: string; statuses: VisitStatus[] }[] = [
  { id: 'queue', label: 'To see', statuses: ['LAB_RESULTS_READY', 'IN_CONSULTATION', 'TRIAGED'] },
  { id: 'lab', label: 'At lab', statuses: ['AWAITING_LAB'] },
  { id: 'done', label: 'Completed', statuses: ['COMPLETED'] },
];

export default function DoctorWorkspace() {
  const { user } = useAuth();
  const [tab, setTab] = useState<Tab>('queue');
  const [selectedId, setSelectedId] = useState<number | null>(null);
  const [openError, setOpenError] = useState<string | null>(null);

  // Pull finished lab orders first so those visits show as "Results ready", then load every tab in one go.
  const q = usePolling(async () => {
    await OpdApi.refreshLabStatuses().catch(() => undefined);
    const all = await OpdApi.myQueue(TABS.flatMap((t) => t.statuses));
    return all;
  }, [], 20000);

  const byTab = (t: Tab) => (q.data ?? []).filter((i) => TABS.find((x) => x.id === t)!.statuses.includes(i.status));
  const list = byTab(tab);
  const readyCount = (q.data ?? []).filter((i) => i.status === 'LAB_RESULTS_READY').length;

  const open = async (item: QueueItem) => {
    setOpenError(null);
    if (item.status === 'TRIAGED' || item.status === 'LAB_RESULTS_READY') {
      try {
        await OpdApi.startConsultation(item.visitId);
      } catch (e) {
        setOpenError(apiError(e, "Couldn't open the consultation"));
        return;
      }
      q.reload();
    }
    setSelectedId(item.visitId);
  };

  return (
    <>
      <PageHeader title="My patients" subtitle={`${user?.fullName} · emergencies first, then by token`}
        actions={<Button variant="secondary" size="sm" onClick={q.reload}
          icon={<RefreshCw className={cx('size-4', q.refreshing && 'animate-spin')} />}>Refresh</Button>} />

      <div className="grid lg:grid-cols-[360px_minmax(0,1fr)] gap-6 items-start">
        <Card className="lg:sticky lg:top-24 overflow-hidden">
          <div className="flex border-b border-line" role="tablist">
            {TABS.map((t) => {
              const n = byTab(t.id).length;
              return (
                <button key={t.id} role="tab" aria-selected={tab === t.id} onClick={() => setTab(t.id)}
                  className={cx('flex-1 h-11 -mb-px border-b-2 text-[13px] font-medium flex items-center justify-center gap-1.5',
                    tab === t.id ? 'border-brand-600 text-brand-700' : 'border-transparent text-ink-3 hover:text-ink')}>
                  {t.label}
                  <span className={cx('text-[11px] rounded-full px-1.5 tabular-nums', tab === t.id ? 'bg-brand-50' : 'bg-subtle')}>{n}</span>
                </button>
              );
            })}
          </div>
          {tab === 'queue' && readyCount > 0 && (
            <p className="px-4 py-2 text-xs bg-emerald-50 text-emerald-800 border-b border-emerald-100">
              {readyCount} patient{readyCount === 1 ? ' has' : 's have'} lab results ready to review.
            </p>
          )}
          {openError && <div className="p-3"><Alert onClose={() => setOpenError(null)}>{openError}</Alert></div>}
          {q.loading ? <Spinner /> : q.error ? (
            <div className="p-3"><Alert action={<Button size="sm" variant="secondary" onClick={q.reload}>Retry</Button>}>{q.error}</Alert></div>
          ) : list.length === 0 ? (
            <EmptyState icon={tab === 'lab' ? <FlaskConical className="size-5" /> : <UserRound className="size-5" />}
              title={tab === 'queue' ? 'Your queue is clear' : tab === 'lab' ? 'No one is waiting on the lab' : 'Nothing completed yet'}>
              {tab === 'queue' && 'Patients appear here once the nurse records their vitals.'}
            </EmptyState>
          ) : (
            <ul className="divide-y divide-line max-h-[calc(100vh-260px)] overflow-y-auto">
              {list.map((i) => (
                <li key={i.visitId}>
                  <button type="button" onClick={() => open(i)}
                    className={cx('w-full text-left flex items-start gap-3 px-4 py-3 transition-colors',
                      selectedId === i.visitId ? 'bg-brand-50' : 'hover:bg-subtle')}>
                    <Token n={i.tokenNumber} />
                    <div className="min-w-0 flex-1">
                      <div className="flex items-center justify-between gap-2">
                        <p className="text-[13.5px] font-medium text-ink truncate">{i.patientFullName}</p>
                        <PriorityBadge priority={i.priority} quiet />
                      </div>
                      <p className="text-xs text-ink-3 truncate mt-0.5">{ageSex(i.patientAge, i.patientGender)} · {i.chiefComplaint}</p>
                      <div className="mt-1.5 flex items-center gap-1.5">
                        <StatusBadge status={i.status} />
                        {i.patientAllergies && <Badge tone="red">Allergy</Badge>}
                      </div>
                    </div>
                  </button>
                </li>
              ))}
            </ul>
          )}
        </Card>

        {selectedId ? (
          <Consultation key={selectedId} visitId={selectedId}
            onChanged={() => q.reload()}
            onClose={() => { setSelectedId(null); q.reload(); }} />
        ) : (
          <Card>
            <EmptyState icon={<Stethoscope className="size-5" />} title="Select a patient">
              Pick a patient from your queue to see their vitals, order tests or sign off the visit.
            </EmptyState>
          </Card>
        )}
      </div>
    </>
  );
}
