import type { Gender, LabOrderStatus, Priority, VisitStatus } from './types';

export const STATUS_LABEL: Record<VisitStatus, string> = {
  REGISTERED: 'Waiting for vitals',
  TRIAGED: 'Waiting for doctor',
  IN_CONSULTATION: 'In consultation',
  AWAITING_LAB: 'At lab',
  LAB_RESULTS_READY: 'Results ready',
  COMPLETED: 'Completed',
  CANCELLED: 'Cancelled',
};

/** Timeline wording: what happened, rather than the resulting state. */
export const EVENT_LABEL: Record<VisitStatus, string> = {
  REGISTERED: 'Registered',
  TRIAGED: 'Vitals recorded',
  IN_CONSULTATION: 'Seen by doctor',
  AWAITING_LAB: 'Sent to lab',
  LAB_RESULTS_READY: 'Lab results released',
  COMPLETED: 'Visit completed',
  CANCELLED: 'Visit cancelled',
};

export const LAB_STATUS_LABEL: Record<LabOrderStatus, string> = {
  PLACED: 'Awaiting sample',
  SAMPLE_COLLECTED: 'Sample collected',
  COMPLETED: 'Reported',
  CANCELLED: 'Cancelled',
};

export const PRIORITY_LABEL: Record<Priority, string> = {
  NORMAL: 'Normal',
  URGENT: 'Urgent',
  EMERGENCY: 'Emergency',
};

export const ROLE_LABEL = { ADMIN: 'Administrator', NURSE: 'Nurse', DOCTOR: 'Doctor', LABTECH: 'Lab technician' } as const;

export const sexShort = (g?: Gender | null) => (g ? g[0] : '–');

export const ageSex = (age?: number | null, g?: Gender | null) =>
  `${age ?? '–'}${age != null ? 'y' : ''} · ${sexShort(g)}`;

export const initials = (name: string) =>
  name.replace(/^Dr\.?\s+/i, '').split(/\s+/).slice(0, 2).map((p) => p[0]?.toUpperCase() ?? '').join('');

const tf = new Intl.DateTimeFormat('en-GB', { hour: '2-digit', minute: '2-digit', timeZone: 'Asia/Kathmandu' });
const dtf = new Intl.DateTimeFormat('en-GB', {
  day: '2-digit', month: 'short', hour: '2-digit', minute: '2-digit', timeZone: 'Asia/Kathmandu',
});
export const time = (iso?: string | null) => (iso ? tf.format(new Date(iso)) : '–');
export const dateTime = (iso?: string | null) => (iso ? dtf.format(new Date(iso)) : '–');

/** Vital-sign interpretation shared by triage entry and the doctor's view. */
export type Level = 'ok' | 'warn' | 'high' | 'low';
export interface Flag { level: Level; text: string }

export function tempFlag(t?: number | null): Flag | null {
  if (t == null || Number.isNaN(t)) return null;
  if (t > 37.8) return { level: 'high', text: 'Fever' };
  if (t < 35.5) return { level: 'low', text: 'Hypothermia' };
  return { level: 'ok', text: 'Normal' };
}
export function pulseFlag(p?: number | null): Flag | null {
  if (p == null || Number.isNaN(p)) return null;
  if (p > 100) return { level: 'high', text: 'Tachycardia' };
  if (p < 60) return { level: 'warn', text: 'Bradycardia' };
  return { level: 'ok', text: 'Normal' };
}
export function spo2Flag(s?: number | null): Flag | null {
  if (s == null || Number.isNaN(s)) return null;
  if (s < 92) return { level: 'high', text: 'Hypoxia' };
  if (s < 95) return { level: 'warn', text: 'Low' };
  return { level: 'ok', text: 'Normal' };
}
export function bpFlag(bp?: string | null): Flag | null {
  const m = bp?.match(/^(\d{2,3})\/(\d{2,3})$/);
  if (!m) return null;
  const [sys, dia] = [Number(m[1]), Number(m[2])];
  if (sys >= 140 || dia >= 90) return { level: 'high', text: 'Hypertensive' };
  if (sys >= 130 || dia >= 80) return { level: 'warn', text: 'Elevated' };
  if (sys < 90 || dia < 60) return { level: 'low', text: 'Hypotensive' };
  return { level: 'ok', text: 'Normal' };
}
