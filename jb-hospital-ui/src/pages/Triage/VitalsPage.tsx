import { useEffect, useState, type FormEvent, type ReactNode } from 'react';
import { useLocation, useNavigate, useParams } from 'react-router-dom';
import { Activity, ArrowLeft, Droplets, Gauge, HeartPulse, Save, Thermometer } from 'lucide-react';
import { OpdApi } from '../../lib/services';
import { apiError } from '../../lib/api';
import type { QueueItem, VitalsRecord } from '../../lib/types';
import { ageSex, bpFlag, dateTime, pulseFlag, spo2Flag, tempFlag, type Flag } from '../../lib/format';
import { Alert, Badge, Button, Card, CardHeader, Field, FlagBadge, Input, Meta, PriorityBadge, Spinner, StatusBadge, Textarea, Token, cx } from '../../components/ui';

const ring = (f: Flag | null) =>
  !f || f.level === 'ok' ? '' : f.level === 'high' ? 'border-red-400 bg-red-50/40' : f.level === 'warn' ? 'border-amber-400 bg-amber-50/40' : 'border-blue-400 bg-blue-50/40';

function VitalField({ label, unit, icon, flag, children }: { label: string; unit: string; icon: ReactNode; flag: Flag | null; children: ReactNode }) {
  return (
    <div className="rounded-xl border border-line p-4">
      <div className="flex items-center justify-between mb-2.5">
        <span className="flex items-center gap-2 text-[13px] font-medium text-ink-2">{icon}{label}</span>
        <FlagBadge flag={flag} />
      </div>
      <div className="flex items-end gap-2">
        {children}
        <span className="text-xs text-ink-3 pb-2.5 shrink-0">{unit}</span>
      </div>
    </div>
  );
}

export default function VitalsPage() {
  const { visitId: param } = useParams();
  const visitId = Number(param);
  const navigate = useNavigate();
  const state = useLocation().state as { patient?: QueueItem; from?: string } | null;
  const back = state?.from ?? '/triage';

  const [patient, setPatient] = useState<QueueItem | null>(state?.patient ?? null);
  const [history, setHistory] = useState<VitalsRecord[]>([]);
  const [loadError, setLoadError] = useState<string | null>(null);

  const [sys, setSys] = useState('');
  const [dia, setDia] = useState('');
  const [pulse, setPulse] = useState('');
  const [temp, setTemp] = useState('');
  const [spo2, setSpo2] = useState('');
  const [notes, setNotes] = useState('');
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    if (!Number.isInteger(visitId) || visitId <= 0) return;
    OpdApi.visit(visitId).then(setPatient).catch((e) => setLoadError(apiError(e, "Couldn't load the patient")));
    OpdApi.vitalsHistory(visitId).then(setHistory).catch(() => setHistory([]));
  }, [visitId]);

  const bp = sys && dia ? `${sys}/${dia}` : '';
  const flags = {
    bp: bpFlag(bp), pulse: pulseFlag(pulse ? Number(pulse) : null),
    temp: tempFlag(temp ? Number(temp) : null), spo2: spo2Flag(spo2 ? Number(spo2) : null),
  };
  const alerts = Object.values(flags).filter((f) => f && f.level !== 'ok').length;

  const submit = async (e: FormEvent) => {
    e.preventDefault();
    setBusy(true);
    setError(null);
    try {
      await OpdApi.saveVitals({
        visitId, bloodPressure: bp, pulseRate: Number(pulse), temperatureCelsius: Number(Number(temp).toFixed(1)),
        oxygenSaturation: Number(spo2), symptomsBrief: notes.trim() || undefined,
      });
      navigate(back, { replace: true, state: { saved: `Vitals saved for ${patient?.patientFullName}. Sent to ${patient?.assignedDoctorName ?? 'the doctor'}.` } });
    } catch (err) {
      setError(apiError(err, "Couldn't save vitals"));
      setBusy(false);
    }
  };

  const invalid = !Number.isInteger(visitId) || visitId <= 0;
  if (invalid || loadError) return <div className="max-w-xl"><Alert>{invalid ? 'Invalid visit.' : loadError}</Alert></div>;
  if (!patient) return <Spinner label="Loading patient…" />;

  const num = 'h-11 text-lg font-semibold tabular-nums';
  return (
    <div className="max-w-4xl">
      <button onClick={() => navigate(back)} className="flex items-center gap-1.5 text-[13px] text-ink-3 hover:text-ink mb-4">
        <ArrowLeft className="size-4" /> Back
      </button>

      <Card className="mb-5">
        <div className="flex flex-wrap items-center gap-4 px-5 py-4">
          <Token n={patient.tokenNumber} size="lg" />
          <div className="flex-1 min-w-48">
            <div className="flex items-center gap-2">
              <h1 className="text-lg font-semibold">{patient.patientFullName}</h1>
              <PriorityBadge priority={patient.priority} quiet />
            </div>
            <p className="text-[13px] text-ink-3 mt-0.5">{patient.chiefComplaint}</p>
          </div>
          <StatusBadge status={patient.status} />
        </div>
        <dl className="grid grid-cols-2 sm:grid-cols-5 gap-4 px-5 py-3 border-t border-line bg-subtle rounded-b-xl">
          <Meta label="MRN" mono>{patient.patientMrn}</Meta>
          <Meta label="Age / Sex">{ageSex(patient.patientAge, patient.patientGender)}</Meta>
          <Meta label="Blood group">{patient.patientBloodGroup ?? '–'}</Meta>
          <Meta label="Doctor">{patient.assignedDoctorName ?? '–'}</Meta>
          <Meta label="Allergies">
            {patient.patientAllergies ? <span className="text-red-700 font-medium">{patient.patientAllergies}</span> : 'None known'}
          </Meta>
        </dl>
      </Card>

      <Card>
        <CardHeader icon={<Activity className="size-[18px]" />} title={history.length ? 'Re-take vitals' : 'Record vitals'}
          subtitle="Abnormal readings are flagged as you type"
          actions={alerts > 0 && <Badge tone="red">{alerts} abnormal</Badge>} />
        <form onSubmit={submit} className="p-5 space-y-5">
          {error && <Alert>{error}</Alert>}
          <fieldset disabled={busy} className="grid sm:grid-cols-2 gap-4">
            <VitalField label="Blood pressure" unit="mmHg" icon={<Gauge className="size-4 text-ink-4" />} flag={flags.bp}>
              <Input aria-label="Systolic" required inputMode="numeric" pattern="\d{2,3}" maxLength={3} placeholder="120"
                value={sys} onChange={(e) => setSys(e.target.value.replace(/\D/g, ''))} className={cx(num, 'text-center', ring(flags.bp))} />
              <span className="text-xl text-ink-4 pb-2">/</span>
              <Input aria-label="Diastolic" required inputMode="numeric" pattern="\d{2,3}" maxLength={3} placeholder="80"
                value={dia} onChange={(e) => setDia(e.target.value.replace(/\D/g, ''))} className={cx(num, 'text-center', ring(flags.bp))} />
            </VitalField>
            <VitalField label="Pulse" unit="bpm" icon={<HeartPulse className="size-4 text-ink-4" />} flag={flags.pulse}>
              <Input aria-label="Pulse" required type="number" min={20} max={250} placeholder="72"
                value={pulse} onChange={(e) => setPulse(e.target.value)} className={cx(num, ring(flags.pulse))} />
            </VitalField>
            <VitalField label="Temperature" unit="°C" icon={<Thermometer className="size-4 text-ink-4" />} flag={flags.temp}>
              <Input aria-label="Temperature" required type="number" step="0.1" min={30} max={45} placeholder="36.6"
                value={temp} onChange={(e) => setTemp(e.target.value)} className={cx(num, ring(flags.temp))} />
            </VitalField>
            <VitalField label="SpO₂" unit="%" icon={<Droplets className="size-4 text-ink-4" />} flag={flags.spo2}>
              <Input aria-label="Oxygen saturation" required type="number" min={50} max={100} placeholder="98"
                value={spo2} onChange={(e) => setSpo2(e.target.value)} className={cx(num, ring(flags.spo2))} />
            </VitalField>
          </fieldset>
          <Field label="Triage notes" htmlFor="notes" hint="Optional">
            <Textarea id="notes" rows={2} disabled={busy} placeholder="Symptoms the patient reports, pain score…"
              value={notes} onChange={(e) => setNotes(e.target.value)} />
          </Field>
          <div className="flex justify-end gap-2">
            <Button type="button" variant="secondary" onClick={() => navigate(back)} disabled={busy}>Cancel</Button>
            <Button type="submit" loading={busy} icon={<Save className="size-4" />}>Save and send to doctor</Button>
          </div>
        </form>

        {history.length > 0 && (
          <div className="border-t border-line px-5 py-4">
            <p className="text-xs font-semibold uppercase tracking-wide text-ink-4 mb-2">Previous readings</p>
            <table className="w-full text-[13px]">
              <tbody className="divide-y divide-line">
                {history.map((h) => (
                  <tr key={h.id} className="text-ink-2">
                    <td className="py-1.5 font-mono text-ink-3">{dateTime(h.recordedAt)}</td>
                    <td>BP {h.bloodPressure}</td><td>HR {h.pulseRate}</td><td>T {h.temperatureCelsius}°</td><td>SpO₂ {h.oxygenSaturation}%</td>
                    <td className="text-ink-3">{h.recordedBy}</td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        )}
      </Card>
    </div>
  );
}
