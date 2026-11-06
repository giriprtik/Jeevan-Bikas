import { useEffect, useState } from 'react';
import { AuthApi } from '../../lib/services';
import { apiError } from '../../lib/api';
import type { Priority, StaffSummary } from '../../lib/types';
import { Field, Segmented, Select, Textarea } from '../../components/ui';

export interface VisitValues {
  chiefComplaint: string;
  priority: Priority;
  assignedDoctorUsername: string;
}
// eslint-disable-next-line react/only-export-components
export const emptyVisit: VisitValues = { chiefComplaint: '', priority: 'NORMAL', assignedDoctorUsername: '' };

/** Chief complaint, priority and doctor: shared by the new- and returning-patient forms. */
export function VisitFields({ value, onChange, disabled }: {
  value: VisitValues; onChange: (v: VisitValues) => void; disabled?: boolean;
}) {
  const [doctors, setDoctors] = useState<StaffSummary[]>([]);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    AuthApi.doctors()
      .then((list) => {
        setDoctors(list);
        if (!value.assignedDoctorUsername && list.length === 1) onChange({ ...value, assignedDoctorUsername: list[0].username });
      })
      .catch((e) => setError(apiError(e, "Couldn't load doctors")));
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, []);

  const set = <K extends keyof VisitValues>(k: K, v: VisitValues[K]) => onChange({ ...value, [k]: v });

  return (
    <div className="space-y-4">
      <Field label="Chief complaint" required htmlFor="complaint">
        <Textarea id="complaint" required rows={2} maxLength={500} disabled={disabled}
          placeholder="e.g. Fever and cough for 3 days" value={value.chiefComplaint}
          onChange={(e) => set('chiefComplaint', e.target.value)} />
      </Field>
      <div className="grid sm:grid-cols-[1fr_1.3fr] gap-4">
        <Field label="Priority" required>
          <Segmented name="Priority" value={value.priority} onChange={(v) => set('priority', v)} disabled={disabled}
            options={[
              { value: 'NORMAL', label: 'Normal' },
              { value: 'URGENT', label: 'Urgent', tone: '!bg-amber-100 !text-amber-900' },
              { value: 'EMERGENCY', label: 'Emergency', tone: '!bg-red-600 !text-white' },
            ]} />
        </Field>
        <Field label="Doctor" required htmlFor="doctor" error={error}>
          <Select id="doctor" required disabled={disabled} value={value.assignedDoctorUsername}
            onChange={(e) => set('assignedDoctorUsername', e.target.value)}>
            <option value="" disabled>{doctors.length ? 'Select a doctor' : 'Loading…'}</option>
            {doctors.map((d) => (
              <option key={d.username} value={d.username}>
                {d.fullName}{d.specialization ? ` · ${d.specialization}` : ''}
              </option>
            ))}
          </Select>
        </Field>
      </div>
    </div>
  );
}
