import { useEffect, useMemo, useState, type FormEvent } from 'react';
import { Send, X } from 'lucide-react';
import { LabApi } from '../../lib/services';
import { apiError } from '../../lib/api';
import type { LabOrder, ResultEntry as Entry, TestParameter } from '../../lib/types';
import { Alert, Badge, Button, Card, CardHeader, Input, Spinner, Textarea, cx } from '../../components/ui';

const keyOf = (p: TestParameter) => `${p.testCode}|${p.parameterName}`;
const hasRange = (p: TestParameter) => p.refLow != null || p.refHigh != null;

function flagOf(p: TestParameter, raw?: string): 'H' | 'L' | null {
  if (!raw?.trim() || !hasRange(p)) return null;
  const v = Number(raw.trim());
  if (Number.isNaN(v)) return null;
  if (p.refLow != null && v < p.refLow) return 'L';
  if (p.refHigh != null && v > p.refHigh) return 'H';
  return null;
}
function range(p: TestParameter) {
  if (p.refLow != null && p.refHigh != null) return `${p.refLow}–${p.refHigh}`;
  if (p.refLow != null) return `≥ ${p.refLow}`;
  if (p.refHigh != null) return `≤ ${p.refHigh}`;
  return 'Qualitative';
}

export function ResultEntry({ order, onSubmitted, onCancel }: { order: LabOrder; onSubmitted: () => void; onCancel: () => void }) {
  const [params, setParams] = useState<TestParameter[] | null>(null);
  const [loadError, setLoadError] = useState<string | null>(null);
  const [values, setValues] = useState<Record<string, string>>({});
  const [manual, setManual] = useState<Record<string, boolean>>({});
  const [notes, setNotes] = useState<Record<string, string>>({});
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    LabApi.parameters(order.testCodes).then(setParams).catch((e) => setLoadError(apiError(e, "Couldn't load test parameters")));
  }, [order.testCodes]);

  const groups = useMemo(() => order.testCodes.map((code) => {
    const ps = (params ?? []).filter((p) => p.testCode === code);
    return { code, name: ps[0]?.testName ?? code, ps };
  }), [order.testCodes, params]);

  const abnormal = (params ?? []).filter((p) => flagOf(p, values[keyOf(p)]) || (!hasRange(p) && manual[keyOf(p)])).length;
  const filled = (params ?? []).filter((p) => values[keyOf(p)]?.trim()).length;

  const submit = async (e: FormEvent) => {
    e.preventDefault();
    const missing = groups.filter((g) => g.ps.length && !g.ps.some((p) => values[keyOf(p)]?.trim()));
    if (missing.length) { setError(`Enter at least one value for ${missing.map((g) => g.code).join(', ')}.`); return; }

    const entries: Entry[] = [];
    for (const g of groups) {
      let noteUsed: boolean = false;
      for (const p of g.ps) {
        const value = values[keyOf(p)]?.trim();
        if (!value) continue;
        const note: string | undefined = !noteUsed && notes[g.code]?.trim() ? notes[g.code].trim() : undefined;
        noteUsed ||= !!note;
        entries.push({ testCode: p.testCode, parameterName: p.parameterName, observedValue: value,
          abnormal: hasRange(p) ? undefined : !!manual[keyOf(p)], technicianNotes: note });
      }
    }
    setBusy(true); setError(null);
    try { await LabApi.submitResults(order.id, entries); onSubmitted(); }
    catch (err) { setError(apiError(err, "Couldn't save results")); setBusy(false); }
  };

  return (
    <Card className="xl:sticky xl:top-24">
      <CardHeader title={`Results · ${order.patientName ?? order.patientMrn}`}
        subtitle={<span className="font-mono">#{order.id} · {order.patientMrn} · ordered by {order.orderedByDoctor}</span>}
        actions={<Button variant="ghost" size="sm" onClick={onCancel} disabled={busy} aria-label="Close" icon={<X className="size-4" />} />} />
      {loadError ? <div className="p-5"><Alert>{loadError}</Alert></div> : !params ? <Spinner label="Loading parameters…" /> : (
        <form onSubmit={submit}>
          <div className="max-h-[calc(100vh-330px)] overflow-y-auto divide-y divide-line">
            {groups.map((g) => (
              <section key={g.code} className="px-5 py-4">
                <p className="text-[13px] font-semibold text-ink mb-2">{g.name} <span className="font-mono font-normal text-ink-4">{g.code}</span></p>
                {g.ps.length === 0 ? <p className="text-xs text-amber-700">No parameters configured for {g.code}.</p> : (
                  <table className="w-full text-[13px]">
                    <thead>
                      <tr className="text-left text-xs text-ink-4">
                        <th className="font-medium pb-1.5 w-[38%]">Parameter</th><th className="font-medium pb-1.5">Value</th>
                        <th className="font-medium pb-1.5">Unit</th><th className="font-medium pb-1.5">Reference</th>
                      </tr>
                    </thead>
                    <tbody>
                      {g.ps.map((p) => {
                        const k = keyOf(p);
                        const f = flagOf(p, values[k]);
                        return (
                          <tr key={k} className={cx(f === 'H' && 'bg-red-50/60', f === 'L' && 'bg-blue-50/60')}>
                            <td className="py-1 pr-2 text-ink-2"><label htmlFor={k}>{p.parameterName}</label></td>
                            <td className="py-1 pr-2">
                              <div className="flex items-center gap-1.5">
                                <Input id={k} disabled={busy} maxLength={50} inputMode={hasRange(p) ? 'decimal' : 'text'}
                                  value={values[k] ?? ''} onChange={(e) => setValues({ ...values, [k]: e.target.value })}
                                  className={cx('h-8 w-24 font-semibold tabular-nums', f === 'H' && 'border-red-400 text-red-700', f === 'L' && 'border-blue-400 text-blue-700')} />
                                {f && <Badge tone={f === 'H' ? 'red' : 'blue'}>{f === 'H' ? 'High' : 'Low'}</Badge>}
                              </div>
                            </td>
                            <td className="py-1 pr-2 text-xs text-ink-3 font-mono">{p.unit ?? ''}</td>
                            <td className="py-1 text-xs text-ink-3 font-mono">
                              {hasRange(p) ? range(p) : (
                                <label className="inline-flex items-center gap-1.5 font-sans cursor-pointer">
                                  <input type="checkbox" className="accent-red-600" checked={!!manual[k]} disabled={busy}
                                    onChange={(e) => setManual({ ...manual, [k]: e.target.checked })} />
                                  <span className={manual[k] ? 'text-red-700 font-medium' : ''}>Abnormal</span>
                                </label>
                              )}
                            </td>
                          </tr>
                        );
                      })}
                    </tbody>
                  </table>
                )}
                <Textarea rows={1} disabled={busy} placeholder={`Notes for ${g.code} (optional)`} className="mt-2 min-h-9 text-[13px]"
                  value={notes[g.code] ?? ''} onChange={(e) => setNotes({ ...notes, [g.code]: e.target.value })} />
              </section>
            ))}
          </div>
          <div className="border-t border-line px-5 py-4 space-y-3">
            {error && <Alert>{error}</Alert>}
            <div className="flex items-center justify-between gap-3">
              <span className="text-[13px] text-ink-3">
                {filled} value{filled === 1 ? '' : 's'} entered
                {abnormal > 0 && <span className="text-red-700 font-medium"> · {abnormal} abnormal</span>}
              </span>
              <Button type="submit" loading={busy} icon={<Send className="size-4" />}>Release results</Button>
            </div>
          </div>
        </form>
      )}
    </Card>
  );
}
