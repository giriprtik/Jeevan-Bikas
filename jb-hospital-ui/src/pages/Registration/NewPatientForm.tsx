import { useState, type FormEvent } from 'react';
import { ChevronDown, UserPlus } from 'lucide-react';
import { OpdApi } from '../../lib/services';
import { apiError } from '../../lib/api';
import type { Gender, PatientRegistered } from '../../lib/types';
import { Alert, Button, Field, Input, Segmented, Select, cx } from '../../components/ui';
import { VisitFields, emptyVisit, type VisitValues } from './VisitFields';

const BLOOD_GROUPS = ['A+', 'A-', 'B+', 'B-', 'O+', 'O-', 'AB+', 'AB-'];
const today = () => new Date().toISOString().slice(0, 10);

export function NewPatientForm({ onRegistered }: { onRegistered: (r: PatientRegistered) => void }) {
  const [fullName, setFullName] = useState('');
  const [ageMode, setAgeMode] = useState<'dob' | 'age'>('dob');
  const [dob, setDob] = useState('');
  const [age, setAge] = useState('');
  const [gender, setGender] = useState<Gender>('FEMALE');
  const [phone, setPhone] = useState('');
  const [bloodGroup, setBloodGroup] = useState('');
  const [allergies, setAllergies] = useState('');
  const [address, setAddress] = useState('');
  const [ecName, setEcName] = useState('');
  const [ecPhone, setEcPhone] = useState('');
  const [more, setMore] = useState(false);
  const [visit, setVisit] = useState<VisitValues>(emptyVisit);

  const [busy, setBusy] = useState(false);
  const [error, setError] = useState<string | null>(null);

  const submit = async (e: FormEvent) => {
    e.preventDefault();
    setError(null);
    const estimated = ageMode === 'age';
    const dateOfBirth = estimated ? `${new Date().getFullYear() - Number(age)}-01-01` : dob;
    setBusy(true);
    try {
      const res = await OpdApi.registerPatient({
        fullName: fullName.trim(), dateOfBirth, dobEstimated: estimated, gender, phone: phone.trim(),
        bloodGroup: bloodGroup || undefined, allergies: allergies.trim() || undefined, address: address.trim() || undefined,
        emergencyContactName: ecName.trim() || undefined, emergencyContactPhone: ecPhone.trim() || undefined,
        chiefComplaint: visit.chiefComplaint.trim(), priority: visit.priority, assignedDoctorUsername: visit.assignedDoctorUsername,
      });
      onRegistered(res);
    } catch (err) {
      setError(apiError(err, 'Registration failed'));
      setBusy(false);
    }
  };

  return (
    <form onSubmit={submit} className="p-5 space-y-6">
      {error && <Alert>{error}</Alert>}

      <fieldset className="space-y-4" disabled={busy}>
        <h3 className="text-xs font-semibold uppercase tracking-wide text-ink-4">Patient</h3>
        <Field label="Full name" required htmlFor="fullName">
          <Input id="fullName" required maxLength={150} autoComplete="off" placeholder="e.g. Sunita Gurung"
            value={fullName} onChange={(e) => setFullName(e.target.value)} />
        </Field>

        <div className="grid sm:grid-cols-2 gap-4">
          <Field label={ageMode === 'dob' ? 'Date of birth' : 'Age in years'} required htmlFor="dob"
            hint={
              <button type="button" className="text-brand-700 hover:underline"
                onClick={() => setAgeMode(ageMode === 'dob' ? 'age' : 'dob')}>
                {ageMode === 'dob' ? 'Only know age?' : 'Enter exact date'}
              </button>
            }>
            {ageMode === 'dob'
              ? <Input id="dob" type="date" required max={today()} value={dob} onChange={(e) => setDob(e.target.value)} />
              : <Input id="dob" type="number" required min={0} max={120} placeholder="e.g. 45" value={age} onChange={(e) => setAge(e.target.value)} />}
          </Field>
          <Field label="Sex" required>
            <Segmented name="Sex" value={gender} onChange={setGender}
              options={[{ value: 'FEMALE', label: 'Female' }, { value: 'MALE', label: 'Male' }, { value: 'OTHER', label: 'Other' }]} />
          </Field>
        </div>

        <div className="grid sm:grid-cols-2 gap-4">
          <Field label="Phone" required htmlFor="phone">
            <Input id="phone" required inputMode="tel" maxLength={20} placeholder="98XXXXXXXX" value={phone}
              onChange={(e) => setPhone(e.target.value)} />
          </Field>
          <Field label="Blood group" htmlFor="blood">
            <Select id="blood" value={bloodGroup} onChange={(e) => setBloodGroup(e.target.value)}>
              <option value="">Unknown</option>
              {BLOOD_GROUPS.map((b) => <option key={b}>{b}</option>)}
            </Select>
          </Field>
        </div>

        <Field label="Known allergies" htmlFor="allergies" hint="Shown to the doctor as an alert">
          <Input id="allergies" placeholder="e.g. Penicillin. Leave blank if none known." value={allergies}
            onChange={(e) => setAllergies(e.target.value)}
            className={cx(allergies.trim() && 'border-red-300 bg-red-50/40')} />
        </Field>

        <button type="button" onClick={() => setMore(!more)}
          className="flex items-center gap-1.5 text-[13px] font-medium text-ink-3 hover:text-ink">
          <ChevronDown className={cx('size-4 transition-transform', more && 'rotate-180')} />
          Address and emergency contact
        </button>
        {more && (
          <div className="space-y-4 animate-fade-up">
            <Field label="Address" htmlFor="address">
              <Input id="address" maxLength={255} placeholder="Tole, municipality, district" value={address}
                onChange={(e) => setAddress(e.target.value)} />
            </Field>
            <div className="grid sm:grid-cols-2 gap-4">
              <Field label="Emergency contact" htmlFor="ecName">
                <Input id="ecName" maxLength={150} placeholder="Name and relation" value={ecName}
                  onChange={(e) => setEcName(e.target.value)} />
              </Field>
              <Field label="Emergency phone" htmlFor="ecPhone">
                <Input id="ecPhone" inputMode="tel" maxLength={20} value={ecPhone} onChange={(e) => setEcPhone(e.target.value)} />
              </Field>
            </div>
          </div>
        )}
      </fieldset>

      <fieldset className="pt-5 border-t border-line space-y-4" disabled={busy}>
        <h3 className="text-xs font-semibold uppercase tracking-wide text-ink-4">Today's visit</h3>
        <VisitFields value={visit} onChange={setVisit} disabled={busy} />
      </fieldset>

      <div className="flex justify-end">
        <Button type="submit" loading={busy} icon={<UserPlus className="size-4" />}>Register and issue token</Button>
      </div>
    </form>
  );
}
