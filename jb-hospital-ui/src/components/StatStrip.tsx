import type { QueueItem, VisitStatus } from '../lib/types';
import { cx } from './ui';

const GROUPS: { label: string; statuses: VisitStatus[]; dot: string }[] = [
  { label: 'Waiting for vitals', statuses: ['REGISTERED'], dot: 'bg-amber-500' },
  { label: 'Waiting for doctor', statuses: ['TRIAGED', 'IN_CONSULTATION', 'LAB_RESULTS_READY'], dot: 'bg-blue-500' },
  { label: 'At lab', statuses: ['AWAITING_LAB'], dot: 'bg-violet-500' },
  { label: 'Completed', statuses: ['COMPLETED'], dot: 'bg-slate-400' },
];

/** Today's patient flow at a glance. */
export function StatStrip({ items }: { items: QueueItem[] | null }) {
  return (
    <div className="grid grid-cols-2 lg:grid-cols-5 gap-3 mb-6">
      <div className="rounded-xl bg-brand-600 text-white px-4 py-3.5 shadow-card">
        <p className="text-[12px] text-brand-100">Registered today</p>
        <p className="text-2xl font-semibold tabular-nums mt-0.5">{items?.length ?? '–'}</p>
      </div>
      {GROUPS.map((g) => (
        <div key={g.label} className="rounded-xl bg-surface border border-line px-4 py-3.5 shadow-card">
          <p className="text-[12px] text-ink-3 flex items-center gap-1.5">
            <span className={cx('size-1.5 rounded-full', g.dot)} /> {g.label}
          </p>
          <p className="text-2xl font-semibold tabular-nums mt-0.5 text-ink">
            {items ? items.filter((i) => g.statuses.includes(i.status)).length : '–'}
          </p>
        </div>
      ))}
    </div>
  );
}
