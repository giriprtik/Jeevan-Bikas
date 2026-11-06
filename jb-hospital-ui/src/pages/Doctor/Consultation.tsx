import { useEffect, useMemo, useState, type FormEvent, type ReactNode } from 'react';
import { AlertTriangle, CheckCircle2, Droplets, FileText, FlaskConical, Gauge, HeartPulse, History, Send, Thermometer, X } from 'lucide-react';
import { LabApi, OpdApi } from '../../lib/services';
import { apiError } from '../../lib/api';
import type { LabTest, QueueItem, VisitEvent, VisitLabResults } from '../../lib/types';
import { EVENT_LABEL, ageSex, bpFlag, dateTime, pulseFlag, spo2Flag, tempFlag, time, type Flag } from '../../lib/format';
import { Alert, Badge, Button, Card, CardHeader, Field, FlagBadge, Input, LabStatusBadge, Meta, PriorityBadge, Spinner, StatusBadge, Textarea, Token, cx, flagText } from '../../components/ui';

function Vital({ icon, label, value, unit, flag }: { icon: ReactNode; label: string; value: ReactNode; unit: string; flag: Flag | null }) {
  return (
    <div className="rounded-lg border border-line px-3.5 py-3">
      <p className="flex items-center gap-1.5 text-xs text-ink-3">{icon}{label}</p>
      <p className={cx('mt-1 text-xl font-semibold tabular-nums', flag ? flagText[flag.level] : 'text-ink-4')}>
        {value ?? '–'}<span className="text-xs font-normal text-ink-3 ml-1">{value != null && unit}</span>
      </p>
      <div className="mt-1 h-5"><FlagBadge flag={flag} /></div>
    </div>
  );
}

export function Consultation({ visitId, onChanged, onClose }: { visitId: number; onChanged: () => void; onClose: () => void }) {
  const [v, setV] = useState<QueueItem | null>(null);
  const [lab, setLab] = useState<VisitLabResults | null>(null);
  const [events, setEvents] = useState<VisitEvent[]>([]);
  const [catalogue, setCatalogue] = useState<LabTest[]>([]);
  const [loadError, setLoadError] = useState<string | null>(null);
  const [done, setDone] = useState<string | null>(null);

  const load = () => {
    OpdApi.visit(visitId).then(setV).catch((e) => setLoadError(apiError(e, "Couldn't load the visit")));
    OpdApi.labResults(visitId).then(setLab).catch(() => setLab(null));
    OpdApi.events(visitId).then(setEvents).catch(() => setEvents([]));
  };
  useEffect(() => {
    load();
    LabApi.catalogue().then(setCatalogue).catch(() => setCatalogue([]));
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [visitId]);

  if (loadError) return <Alert>{loadError}</Alert>;
  if (!v) return <Card><Spinner label="Loading patient…" /></Card>;

  const active = v.status === 'IN_CONSULTATION' || v.status === 'TRIAGED' || v.status === 'LAB_RESULTS_READY';

  return (
    <div className="space-y-5 animate-fade-up">
      {done && <Alert kind="success" title={done} onClose={onClose}>The patient has left your queue.</Alert>}

      {/* Patient banner */}
      <Card>
        <div className="flex flex-wrap items-center gap-4 px-5 py-4">
          <Token n={v.tokenNumber} size="lg" />
          <div className="flex-1 min-w-48">
            <div className="flex items-center gap-2 flex-wrap">
              <h2 className="text-lg font-semibold">{v.patientFullName}</h2>
              <PriorityBadge priority={v.priority} quiet />
              <StatusBadge status={v.status} />
            </div>
            <p className="text-[13px] text-ink-3 mt-0.5"><span className="font-medium text-ink-2">Complaint:</span> {v.chiefComplaint}</p>
          </div>
          <Button variant="ghost" size="sm" onClick={onClose} aria-label="Close" icon={<X className="size-4" />} />
        </div>
        {v.patientAllergies && (
          <div className="flex items-center gap-2 px-5 py-2 bg-red-50 border-y border-red-100 text-[13px] text-red-800">
            <AlertTriangle className="size-4" /><span className="font-semibold">Allergies:</span> {v.patientAllergies}
          </div>
        )}
        <dl className="grid grid-cols-2 sm:grid-cols-4 gap-4 px-5 py-3 bg-subtle rounded-b-xl border-t border-line">
          <Meta label="MRN" mono>{v.patientMrn}</Meta>
          <Meta label="Age / Sex">{ageSex(v.patientAge, v.patientGender)}</Meta>
          <Meta label="Blood group">{v.patientBloodGroup ?? '–'}</Meta>
          <Meta label="Registered">{v.registeredTime ?? '–'}</Meta>
        </dl>
      </Card>

      {/* Vitals */}
      <Card>
        <CardHeader icon={<HeartPulse className="size-[18px]" />} title="Triage vitals" />
        <div className="p-5">
          <div className="grid grid-cols-2 md:grid-cols-4 gap-3">
            <Vital icon={<Gauge className="size-3.5" />} label="Blood pressure" value={v.bloodPressure} unit="mmHg" flag={bpFlag(v.bloodPressure)} />
            <Vital icon={<HeartPulse className="size-3.5" />} label="Pulse" value={v.pulseRate} unit="bpm" flag={pulseFlag(v.pulseRate)} />
            <Vital icon={<Thermometer className="size-3.5" />} label="Temperature" value={v.temperatureCelsius != null ? Number(v.temperatureCelsius).toFixed(1) : null} unit="°C" flag={tempFlag(v.temperatureCelsius)} />
            <Vital icon={<Droplets className="size-3.5" />} label="SpO₂" value={v.oxygenSaturation} unit="%" flag={spo2Flag(v.oxygenSaturation)} />
          </div>
          {v.symptomsBrief && <p className="mt-3 text-[13px] text-ink-2"><span className="text-ink-3">Triage notes:</span> {v.symptomsBrief}</p>}
        </div>
      </Card>

      {lab && <LabResults visitId={visitId} lab={lab} />}

      {active && !done && (
        <div className="grid xl:grid-cols-2 gap-5 items-start">
          <OrderTests visitId={visitId} catalogue={catalogue} alreadyAtLab={lab != null && lab.orderStatus !== 'COMPLETED'}
            onSent={() => { setDone('Sent to the lab'); onChanged(); }} />
          <SignOff visitId={visitId} onDone={() => { setDone('Visit completed'); onChanged(); }} />
        </div>
      )}
      {v.status === 'AWAITING_LAB' && (
        <Alert kind="info" title="Waiting for the lab">You can sign off once results are released. This page checks for results every 20 seconds.</Alert>
      )}
      {v.status === 'COMPLETED' && v.diagnosis && (
        <Alert kind="success" title="Completed">Diagnosis: {v.diagnosis}</Alert>
      )}

      <Timeline events={events} />
    </div>
  );
}

function LabResults({ visitId, lab }: { visitId: number; lab: VisitLabResults }) {
  const [pdfBusy, setPdfBusy] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const groups = useMemo(() => {
    const m = new Map<string, typeof lab.results>();
    lab.results.forEach((r) => m.set(r.testCode, [...(m.get(r.testCode) ?? []), r]));
    return [...m.entries()];
  }, [lab]);
  const abnormal = lab.results.filter((r) => r.abnormal).length;
  const completed = lab.orderStatus === 'COMPLETED';

  return (
    <Card>
      <CardHeader icon={<FlaskConical className="size-[18px]" />} title="Lab results"
        subtitle={<>Order #{lab.orderId} · {lab.testCodes.join(', ')} · {dateTime(lab.orderedAt)}</>}
        actions={<>
          <LabStatusBadge status={lab.orderStatus} />
          {completed && abnormal > 0 && <Badge tone="red">{abnormal} abnormal</Badge>}
          {completed && (
            <Button variant="secondary" size="sm" loading={pdfBusy} icon={<FileText className="size-4" />}
              onClick={async () => {
                setPdfBusy(true); setError(null);
                try { await OpdApi.openLabReport(visitId); } catch (e) { setError(apiError(e, "Couldn't open the report")); }
                finally { setPdfBusy(false); }
              }}>PDF report</Button>
          )}
        </>} />
      {error && <div className="px-5 pt-4"><Alert>{error}</Alert></div>}
      {!completed ? (
        <p className="px-5 py-4 text-[13px] text-ink-3">Results will appear here when the lab releases them.</p>
      ) : (
        <div className="divide-y divide-line">
          {groups.map(([code, rows]) => (
            <div key={code} className="px-5 py-4">
              <p className="text-[13px] font-semibold text-ink mb-2">{rows[0].testName} <span className="font-mono font-normal text-ink-4">{code}</span></p>
              <table className="w-full text-[13px]">
                <thead>
                  <tr className="text-left text-xs text-ink-4">
                    <th className="font-medium pb-1.5">Parameter</th><th className="font-medium pb-1.5">Result</th><th className="font-medium pb-1.5">Reference</th>
                  </tr>
                </thead>
                <tbody className="divide-y divide-line">
                  {rows.map((r) => (
                    <tr key={r.id} className={r.abnormal ? 'bg-red-50/60' : ''}>
                      <td className="py-1.5 pr-3 text-ink-2">{r.parameterName}</td>
                      <td className={cx('py-1.5 pr-3 font-semibold tabular-nums', r.abnormal ? 'text-red-700' : 'text-ink')}>
                        {r.observedValue}{r.abnormal && <span className="ml-1.5 text-[11px] font-medium">Abnormal</span>}
                      </td>
                      <td className="py-1.5 font-mono text-xs text-ink-3">{r.referenceRange ?? '–'}</td>
                    </tr>
                  ))}
                </tbody>
              </table>
              {rows.some((r) => r.technicianNotes) && (
                <p className="mt-2 text-xs text-ink-3 italic">{[...new Set(rows.map((r) => r.technicianNotes).filter(Boolean))].join(' ')}</p>
              )}
            </div>
          ))}
        </div>
      )}
    </Card>
  );
}

function OrderTests({ visitId, catalogue, alreadyAtLab, onSent }: { visitId: number; catalogue: LabTest[]; alreadyAtLab: boolean; onSent: () => void }) {
  const [picked, setPicked] = useState<string[]>([]);
  const [notes, setNotes] = useState('');
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const total = catalogue.filter((t) => picked.includes(t.testCode)).reduce((s, t) => s + Number(t.cost), 0);

  const send = async () => {
    setBusy(true); setError(null);
    try { await OpdApi.sendToLab(visitId, picked, notes.trim() || undefined); onSent(); }
    catch (e) { setError(apiError(e, "Couldn't send to the lab")); setBusy(false); }
  };

  return (
    <Card>
      <CardHeader icon={<FlaskConical className="size-[18px]" />} title="Order lab tests" subtitle="Patient moves to the lab queue" />
      <div className="p-5 space-y-4">
        {error && <Alert>{error}</Alert>}
        {alreadyAtLab && <Alert kind="warning">This visit already has an open lab order.</Alert>}
        <div className="grid sm:grid-cols-2 gap-2">
          {catalogue.length === 0 && <p className="text-[13px] text-ink-3">Loading test catalogue…</p>}
          {catalogue.map((t) => {
            const on = picked.includes(t.testCode);
            return (
              <label key={t.testCode} className={cx('flex items-start gap-2.5 rounded-lg border px-3 py-2.5 cursor-pointer transition-colors',
                on ? 'border-brand-500 bg-brand-50' : 'border-line hover:border-line-strong')}>
                <input type="checkbox" className="mt-0.5 accent-brand-600 size-4" checked={on} disabled={busy}
                  onChange={() => setPicked(on ? picked.filter((c) => c !== t.testCode) : [...picked, t.testCode])} />
                <span className="min-w-0">
                  <span className="block text-[13px] font-medium text-ink leading-tight">{t.testName}</span>
                  <span className="block text-[11.5px] text-ink-3 font-mono mt-0.5">{t.testCode} · Rs {Number(t.cost).toFixed(0)}</span>
                </span>
              </label>
            );
          })}
        </div>
        <Field label="Clinical notes for the lab" htmlFor="labNotes" hint="Optional">
          <Input id="labNotes" disabled={busy} placeholder="e.g. Suspected typhoid; fasting since 8am" value={notes} onChange={(e) => setNotes(e.target.value)} />
        </Field>
        <div className="flex items-center justify-between gap-3">
          <span className="text-[13px] text-ink-3">{picked.length ? <>Total <span className="font-medium text-ink tabular-nums">Rs {total.toFixed(0)}</span></> : 'No tests selected'}</span>
          <Button variant="secondary" disabled={!picked.length || alreadyAtLab} loading={busy} onClick={send} icon={<Send className="size-4" />}>
            Send {picked.length || ''} to lab
          </Button>
        </div>
      </div>
    </Card>
  );
}

function SignOff({ visitId, onDone }: { visitId: number; onDone: () => void }) {
  const [diagnosis, setDiagnosis] = useState('');
  const [notes, setNotes] = useState('');
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState<string | null>(null);

  const submit = async (e: FormEvent) => {
    e.preventDefault();
    setBusy(true); setError(null);
    try { await OpdApi.conclude(visitId, diagnosis.trim(), notes.trim()); onDone(); }
    catch (err) { setError(apiError(err, "Couldn't complete the visit")); setBusy(false); }
  };

  return (
    <Card>
      <CardHeader icon={<CheckCircle2 className="size-[18px]" />} title="Diagnosis and sign-off" subtitle="Completes the visit" />
      <form onSubmit={submit} className="p-5 space-y-4">
        {error && <Alert>{error}</Alert>}
        <Field label="Diagnosis" required htmlFor="dx">
          <Input id="dx" required maxLength={255} disabled={busy} placeholder="e.g. Acute pharyngitis (J02.9)" value={diagnosis} onChange={(e) => setDiagnosis(e.target.value)} />
        </Field>
        <Field label="Consultation notes" required htmlFor="cn" hint="Findings, treatment, prescription">
          <Textarea id="cn" required rows={5} disabled={busy} value={notes} onChange={(e) => setNotes(e.target.value)}
            placeholder={'Findings: …\nRx: Paracetamol 500 mg TDS × 3 days\nAdvice: …'} />
        </Field>
        <div className="flex justify-end">
          <Button type="submit" loading={busy} icon={<CheckCircle2 className="size-4" />}>Complete visit</Button>
        </div>
      </form>
    </Card>
  );
}

function Timeline({ events }: { events: VisitEvent[] }) {
  if (!events.length) return null;
  return (
    <Card>
      <CardHeader icon={<History className="size-[18px]" />} title="Visit timeline" subtitle="Audit trail of every step" />
      <ol className="px-5 py-4 space-y-3">
        {events.map((e, idx) => (
          <li key={e.id} className="flex gap-3">
            <div className="flex flex-col items-center">
              <span className={cx('size-2.5 rounded-full mt-1.5', idx === events.length - 1 ? 'bg-brand-600' : 'bg-line-strong')} />
              {idx < events.length - 1 && <span className="w-px flex-1 bg-line mt-1" />}
            </div>
            <div className="pb-1 min-w-0">
              <p className="text-[13px] text-ink"><span className="font-medium">{EVENT_LABEL[e.toStatus]}</span>
                <span className="text-ink-3"> · {e.actor} · <span className="font-mono">{time(e.occurredAt)}</span></span></p>
              {e.note && <p className="text-xs text-ink-3 mt-0.5">{e.note}</p>}
            </div>
          </li>
        ))}
      </ol>
    </Card>
  );
}
