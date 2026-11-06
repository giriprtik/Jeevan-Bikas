import { useNavigate } from 'react-router-dom';
import { ChevronRight, HeartPulse, RefreshCw } from 'lucide-react';
import { OpdApi } from '../../lib/services';
import { usePolling } from '../../hooks/usePolling';
import { useFlash } from '../../hooks/useFlash';
import { ageSex } from '../../lib/format';
import { Alert, Button, Card, CardHeader, EmptyState, PageHeader, PriorityBadge, Spinner, Token, cx } from '../../components/ui';

export default function TriageQueue() {
  const navigate = useNavigate();
  const q = usePolling(() => OpdApi.triageQueue(), [], 15000);
  const [flash, clearFlash] = useFlash();

  return (
    <>
      <PageHeader title="Triage" subtitle="Patients waiting for vitals. Emergencies are listed first."
        actions={<Button variant="secondary" size="sm" onClick={q.reload}
          icon={<RefreshCw className={cx('size-4', q.refreshing && 'animate-spin')} />}>Refresh</Button>} />
      {flash && <div className="max-w-4xl mb-4"><Alert kind="success" onClose={clearFlash}>{flash}</Alert></div>}
      <Card className="max-w-4xl">
        <CardHeader icon={<HeartPulse className="size-[18px]" />} title="Waiting for vitals"
          subtitle={q.data ? `${q.data.length} patient${q.data.length === 1 ? '' : 's'}` : ' '} />
        {q.loading ? <Spinner /> : q.error ? (
          <div className="p-4"><Alert action={<Button size="sm" variant="secondary" onClick={q.reload}>Retry</Button>}>{q.error}</Alert></div>
        ) : !q.data?.length ? (
          <EmptyState icon={<HeartPulse className="size-5" />} title="Nobody is waiting">Newly registered patients appear here.</EmptyState>
        ) : (
          <ul className="divide-y divide-line">
            {q.data.map((i) => (
              <li key={i.visitId}>
                <button type="button" onClick={() => navigate(`/triage/${i.visitId}`, { state: { patient: i, from: '/triage' } })}
                  className={cx('w-full text-left flex items-center gap-4 px-5 py-3.5 hover:bg-subtle group',
                    i.priority === 'EMERGENCY' && 'bg-red-50/50')}>
                  <Token n={i.tokenNumber} />
                  <div className="min-w-0 flex-1">
                    <div className="flex items-center gap-2">
                      <p className="text-sm font-medium text-ink truncate">{i.patientFullName}</p>
                      <PriorityBadge priority={i.priority} quiet />
                    </div>
                    <p className="text-[13px] text-ink-3 truncate mt-0.5">{i.chiefComplaint}</p>
                  </div>
                  <div className="text-right text-xs text-ink-3 hidden sm:block">
                    <p className="font-mono">{i.patientMrn}</p>
                    <p>{ageSex(i.patientAge, i.patientGender)} · {i.registeredTime}</p>
                  </div>
                  <ChevronRight className="size-4 text-ink-4 group-hover:text-brand-600" />
                </button>
              </li>
            ))}
          </ul>
        )}
      </Card>
    </>
  );
}
