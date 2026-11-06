import { useEffect, useState, type FormEvent } from 'react';
import { CalendarPlus, Search, UserRound } from 'lucide-react';
import { OpdApi } from '../../lib/services';
import { apiError } from '../../lib/api';
import type { PatientSummary, QueueItem } from '../../lib/types';
import { Alert, Badge, Button, EmptyState, Input, Spinner, cx } from '../../components/ui';
import { VisitFields, emptyVisit, type VisitValues } from './VisitFields';

const ageOf = (dob: string) => Math.floor((Date.now() - new Date(dob).getTime()) / (365.25 * 864e5));

export function ReturningPatientForm({ onOpened }: { onOpened: (v: QueueItem) => void }) {
  const [q, setQ] = useState('');
  const [results, setResults] = useState<PatientSummary[] | null>(null);
  const [searching, setSearching] = useState(false);
  const [selected, setSelected] = useState<PatientSummary | null>(null);
  const [visit, setVisit] = useState<VisitValues>(emptyVisit);
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState<string | null>(null);

  // Debounced search by MRN, phone or name
  useEffect(() => {
    const term = q.trim();
    if (term.length < 2) return;
    const t = setTimeout(() => {
      OpdApi.searchPatients(term)
        .then(setResults)
        .catch((e) => setError(apiError(e, 'Search failed')))
        .finally(() => setSearching(false));
    }, 250);
    return () => clearTimeout(t);
  }, [q]);

  const submit = async (e: FormEvent) => {
    e.preventDefault();
    if (!selected) return;
    setBusy(true);
    setError(null);
    try {
      onOpened(await OpdApi.openVisit({ patientMrn: selected.mrn, ...visit, chiefComplaint: visit.chiefComplaint.trim() }));
    } catch (err) {
      setError(apiError(err, "Couldn't open the visit"));
      setBusy(false);
    }
  };

  return (
    <div className="p-5 space-y-5">
      {error && <Alert onClose={() => setError(null)}>{error}</Alert>}

      <div className="relative">
        <Search className="size-4 text-ink-4 absolute left-3 top-1/2 -translate-y-1/2" />
        <Input autoFocus placeholder="Search by MRN, phone or name" value={q} onChange={(e) => { setQ(e.target.value); setSelected(null); setResults(null); setSearching(e.target.value.trim().length >= 2); }}
          className="pl-9" aria-label="Search patients" />
      </div>

      {!selected && (
        <div className="rounded-lg border border-line divide-y divide-line max-h-80 overflow-y-auto">
          {q.trim().length < 2 ? (
            <EmptyState icon={<Search className="size-5" />} title="Find a returning patient">
              Type at least two characters of their MRN (JB-000123), phone or name.
            </EmptyState>
          ) : searching && !results ? (
            <Spinner label="Searching…" />
          ) : results?.length === 0 ? (
            <EmptyState icon={<UserRound className="size-5" />} title="No matching patient">
              Check the spelling, or register them as a new patient.
            </EmptyState>
          ) : (
            results?.map((p) => (
              <button key={p.id} type="button" disabled={p.hasActiveVisit} onClick={() => setSelected(p)}
                className={cx('w-full text-left px-4 py-3 flex items-center justify-between gap-3',
                  p.hasActiveVisit ? 'opacity-60 cursor-not-allowed' : 'hover:bg-subtle')}>
                <div className="min-w-0">
                  <p className="text-sm font-medium text-ink truncate">{p.fullName}</p>
                  <p className="text-xs text-ink-3 font-mono mt-0.5">
                    {p.mrn} · {ageOf(p.dateOfBirth)}y {p.gender[0]} · {p.phone}
                  </p>
                </div>
                {p.hasActiveVisit ? <Badge tone="amber">Visit already open</Badge> : <span className="text-xs text-brand-700 font-medium">Select</span>}
              </button>
            ))
          )}
        </div>
      )}

      {selected && (
        <form onSubmit={submit} className="space-y-5 animate-fade-up">
          <div className="flex items-center justify-between gap-3 rounded-lg bg-brand-50 border border-brand-100 px-4 py-3">
            <div className="min-w-0">
              <p className="text-sm font-semibold text-ink">{selected.fullName}</p>
              <p className="text-xs text-ink-3 font-mono mt-0.5">
                {selected.mrn} · {ageOf(selected.dateOfBirth)}y {selected.gender[0]} · {selected.phone}
                {selected.bloodGroup && ` · ${selected.bloodGroup}`}
              </p>
              {selected.allergies && <p className="text-xs text-red-700 mt-1 font-medium">Allergies: {selected.allergies}</p>}
            </div>
            <Button type="button" variant="ghost" size="sm" onClick={() => setSelected(null)}>Change</Button>
          </div>
          <VisitFields value={visit} onChange={setVisit} disabled={busy} />
          <div className="flex justify-end">
            <Button type="submit" loading={busy} icon={<CalendarPlus className="size-4" />}>Open visit and issue token</Button>
          </div>
        </form>
      )}
    </div>
  );
}
