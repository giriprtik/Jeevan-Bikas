import { useState } from 'react';
import { Check, FlaskConical, RefreshCw, TestTube } from 'lucide-react';
import { LabApi } from '../../lib/services';
import { apiError } from '../../lib/api';
import { usePolling } from '../../hooks/usePolling';
import { time } from '../../lib/format';
import type { LabOrder, LabOrderStatus } from '../../lib/types';
import { Alert, Badge, Button, Card, EmptyState, LabStatusBadge, PageHeader, Spinner, cx } from '../../components/ui';
import { ResultEntry } from './ResultEntry';

type Tab = 'pending' | 'reported';
const STATUSES: Record<Tab, LabOrderStatus[]> = { pending: ['PLACED', 'SAMPLE_COLLECTED'], reported: ['COMPLETED'] };

const pendingSamples = (o: LabOrder) => o.samples.filter((s) => !s.collected);

export default function LabWorkspace() {
  const [tab, setTab] = useState<Tab>('pending');
  const [selected, setSelected] = useState<LabOrder | null>(null);
  /** The test the technician has clicked on, per order. */
  const [pickedTest, setPickedTest] = useState<Record<number, string>>({});
  const [actionError, setActionError] = useState<string | null>(null);
  const [busyId, setBusyId] = useState<number | null>(null);
  const [flash, setFlash] = useState<string | null>(null);
  const q = usePolling(() => LabApi.queue(STATUSES[tab]), [tab], 15000);

  // A single-test order needs no choice; with several tests the technician must click one.
  const testToCollect = (o: LabOrder) => {
    if (o.samples.length === 1 && !o.samples[0].collected) return o.samples[0].testCode;
    const picked = pickedTest[o.id];
    return picked && pendingSamples(o).some((s) => s.testCode === picked) ? picked : null;
  };

  const collect = async (o: LabOrder, testCode: string) => {
    setBusyId(o.id); setActionError(null);
    try {
      const updated = await LabApi.collectSample(o.id, testCode);
      q.setData((q.data ?? []).map((x) => (x.id === o.id ? updated : x)));
      setPickedTest((p) => { const n = { ...p }; delete n[o.id]; return n; });
      if (selected?.id === o.id) setSelected(updated);
    } catch (e) { setActionError(apiError(e, "Couldn't record the sample")); }
    finally { setBusyId(null); }
  };

  const orders = q.data ?? [];
  return (
    <>
      <PageHeader title="Laboratory" subtitle="Collect each test's sample, enter results and release them to the doctor."
        actions={<Button variant="secondary" size="sm" onClick={q.reload}
          icon={<RefreshCw className={cx('size-4', q.refreshing && 'animate-spin')} />}>Refresh</Button>} />
      {flash && <div className="mb-4"><Alert kind="success" onClose={() => setFlash(null)}>{flash}</Alert></div>}

      <div className="grid xl:grid-cols-[minmax(0,1fr)_minmax(0,1fr)] gap-6 items-start">
        <Card className="overflow-hidden">
          <div className="flex border-b border-line px-2" role="tablist">
            {(['pending', 'reported'] as Tab[]).map((t) => (
              <button key={t} role="tab" aria-selected={tab === t} onClick={() => { setTab(t); setSelected(null); }}
                className={cx('h-12 px-4 -mb-px border-b-2 text-[13.5px] font-medium',
                  tab === t ? 'border-brand-600 text-brand-700' : 'border-transparent text-ink-3 hover:text-ink')}>
                {t === 'pending' ? 'Worklist' : 'Reported'}
              </button>
            ))}
          </div>
          {actionError && <div className="p-3"><Alert onClose={() => setActionError(null)}>{actionError}</Alert></div>}
          {q.loading ? <Spinner /> : q.error ? (
            <div className="p-3"><Alert action={<Button size="sm" variant="secondary" onClick={q.reload}>Retry</Button>}>{q.error}</Alert></div>
          ) : orders.length === 0 ? (
            <EmptyState icon={<TestTube className="size-5" />} title={tab === 'pending' ? 'No pending orders' : 'Nothing reported yet'}>
              {tab === 'pending' && 'Orders placed by doctors appear here automatically.'}
            </EmptyState>
          ) : (
            <ul className="divide-y divide-line">
              {orders.map((o) => {
                const pending = pendingSamples(o);
                const done = o.samples.length - pending.length;
                const target = testToCollect(o);
                const canPick = tab === 'pending' && o.samples.length > 1;
                return (
                  <li key={o.id} className={cx('px-5 py-3.5 flex items-start gap-4', selected?.id === o.id && 'bg-brand-50')}>
                    <div className="min-w-0 flex-1">
                      <div className="flex items-center gap-2 flex-wrap">
                        <p className="text-[13.5px] font-medium text-ink truncate">{o.patientName ?? o.patientMrn}</p>
                        {o.orderStatus === 'PLACED' && done > 0
                          ? <Badge tone="blue" dot>{done} of {o.samples.length} collected</Badge>
                          : <LabStatusBadge status={o.orderStatus} />}
                      </div>
                      <p className="text-xs text-ink-3 mt-0.5 font-mono">#{o.id} · {o.patientMrn} · ordered by {o.orderedByDoctor} at {time(o.orderedAt)}</p>

                      <div className="flex flex-wrap gap-1.5 mt-2" role={canPick ? 'radiogroup' : undefined} aria-label={canPick ? `Tests on order ${o.id}` : undefined}>
                        {o.samples.map((s) => {
                          const isPicked = target === s.testCode && canPick;
                          if (s.collected || !canPick) {
                            return (
                              <span key={s.testCode}
                                title={s.collected ? `Collected by ${s.collectedBy} at ${time(s.collectedAt)}` : undefined}
                                className={cx('inline-flex items-center gap-1 h-7 px-2.5 rounded-md text-xs font-medium font-mono ring-1 ring-inset',
                                  s.collected ? 'bg-emerald-50 text-emerald-700 ring-emerald-200' : 'bg-violet-50 text-violet-700 ring-violet-200')}>
                                {s.collected && <Check className="size-3.5" />}{s.testCode}
                              </span>
                            );
                          }
                          return (
                            <button key={s.testCode} type="button" role="radio" aria-checked={isPicked}
                              onClick={() => setPickedTest((p) => ({ ...p, [o.id]: s.testCode }))}
                              className={cx('inline-flex items-center gap-1 h-7 px-2.5 rounded-md text-xs font-medium font-mono ring-1 ring-inset transition-colors',
                                isPicked ? 'bg-brand-600 text-white ring-brand-600' : 'bg-surface text-violet-700 ring-violet-300 hover:bg-violet-50')}>
                              {s.testCode}
                            </button>
                          );
                        })}
                      </div>
                      {canPick && pending.length > 0 && !target && (
                        <p className="text-[11.5px] text-ink-3 mt-1.5">Click a test, then collect its sample.</p>
                      )}
                      {o.clinicalHistory && <p className="text-xs text-ink-3 mt-1.5 truncate">{o.clinicalHistory}</p>}
                    </div>

                    {tab === 'pending' && (
                      <div className="flex flex-col gap-1.5 shrink-0 w-40">
                        {pending.length > 0 && (
                          <Button size="sm" variant="secondary" disabled={!target} loading={busyId === o.id}
                            onClick={() => target && collect(o, target)}>
                            {target ? `Collect ${target} sample` : 'Collect sample'}
                          </Button>
                        )}
                        <Button size="sm" variant={selected?.id === o.id ? 'primary' : 'secondary'}
                          disabled={pending.length > 0} title={pending.length ? 'Collect every sample first' : undefined}
                          onClick={() => setSelected(o)}>
                          Enter results
                        </Button>
                      </div>
                    )}
                  </li>
                );
              })}
            </ul>
          )}
        </Card>

        {selected ? (
          <ResultEntry key={selected.id} order={selected} onCancel={() => setSelected(null)}
            onSubmitted={() => {
              setFlash(`Results released for ${selected.patientName ?? selected.patientMrn} (order #${selected.id}). The doctor will see them on their next refresh.`);
              setSelected(null); q.reload();
            }} />
        ) : (
          <Card>
            <EmptyState icon={<FlaskConical className="size-5" />} title="No order selected">
              Collect every sample on an order, then click “Enter results”. Values outside the reference range are flagged automatically.
            </EmptyState>
          </Card>
        )}
      </div>
    </>
  );
}
