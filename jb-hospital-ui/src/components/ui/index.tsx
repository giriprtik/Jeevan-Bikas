import { forwardRef, type ButtonHTMLAttributes, type InputHTMLAttributes, type ReactNode, type SelectHTMLAttributes, type TextareaHTMLAttributes } from 'react';
import { AlertTriangle, CheckCircle2, Info, Loader2, X, XCircle } from 'lucide-react';
import type { Flag, Level } from '../../lib/format';
import { LAB_STATUS_LABEL, PRIORITY_LABEL, STATUS_LABEL } from '../../lib/format';
import type { LabOrderStatus, Priority, VisitStatus } from '../../lib/types';

// eslint-disable-next-line react/only-export-components -- tiny class helper shared with the primitives
export const cx = (...c: (string | false | null | undefined)[]) => c.filter(Boolean).join(' ');

/* ------------------------------------------------------------------ Button */
type Variant = 'primary' | 'secondary' | 'ghost' | 'danger';
const variants: Record<Variant, string> = {
  primary: 'bg-brand-600 text-white hover:bg-brand-700 shadow-card disabled:bg-brand-600/50',
  secondary: 'bg-surface text-ink-2 border border-line-strong hover:bg-subtle hover:text-ink disabled:text-ink-4',
  ghost: 'text-ink-3 hover:bg-subtle hover:text-ink',
  danger: 'bg-red-600 text-white hover:bg-red-700',
};
interface ButtonProps extends ButtonHTMLAttributes<HTMLButtonElement> {
  variant?: Variant;
  size?: 'sm' | 'md';
  loading?: boolean;
  icon?: ReactNode;
}
export function Button({ variant = 'primary', size = 'md', loading, icon, className, children, disabled, ...rest }: ButtonProps) {
  return (
    <button
      {...rest}
      disabled={disabled || loading}
      className={cx(
        'inline-flex items-center justify-center gap-2 rounded-lg font-medium transition-colors disabled:cursor-not-allowed whitespace-nowrap',
        size === 'sm' ? 'h-8 px-3 text-[13px]' : 'h-10 px-4 text-sm',
        variants[variant],
        className,
      )}
    >
      {loading ? <Loader2 className="size-4 animate-spin" /> : icon}
      {children}
    </button>
  );
}

/* ------------------------------------------------------------------ Card */
export function Card({ className, children }: { className?: string; children: ReactNode }) {
  return <section className={cx('bg-surface border border-line rounded-xl shadow-card', className)}>{children}</section>;
}
export function CardHeader({ title, subtitle, actions, icon }: { title: ReactNode; subtitle?: ReactNode; actions?: ReactNode; icon?: ReactNode }) {
  return (
    <header className="flex items-start justify-between gap-3 px-5 py-4 border-b border-line">
      <div className="flex items-start gap-3 min-w-0">
        {icon && <span className="mt-0.5 text-brand-600">{icon}</span>}
        <div className="min-w-0">
          <h2 className="text-[15px] font-semibold text-ink leading-tight">{title}</h2>
          {subtitle && <p className="text-[13px] text-ink-3 mt-0.5">{subtitle}</p>}
        </div>
      </div>
      {actions && <div className="flex items-center gap-2 shrink-0">{actions}</div>}
    </header>
  );
}

/* ------------------------------------------------------------------ Page header */
export function PageHeader({ title, subtitle, actions }: { title: string; subtitle?: ReactNode; actions?: ReactNode }) {
  return (
    <div className="flex flex-wrap items-end justify-between gap-4 mb-6">
      <div>
        <h1 className="text-[22px] font-semibold tracking-tight text-ink">{title}</h1>
        {subtitle && <p className="text-sm text-ink-3 mt-1">{subtitle}</p>}
      </div>
      {actions && <div className="flex items-center gap-2">{actions}</div>}
    </div>
  );
}

/* ------------------------------------------------------------------ Form fields */
const fieldBase =
  'w-full rounded-lg border border-line-strong bg-surface px-3 text-sm text-ink placeholder:text-ink-4 ' +
  'transition-colors focus:outline-none focus:border-brand-500 focus:ring-3 focus:ring-brand-100 ' +
  'disabled:bg-subtle disabled:text-ink-3';

export function Field({ label, required, hint, error, children, className, htmlFor }: {
  label: string; required?: boolean; hint?: ReactNode; error?: string | null; children: ReactNode; className?: string; htmlFor?: string;
}) {
  return (
    <div className={className}>
      <label htmlFor={htmlFor} className="flex items-baseline justify-between gap-2 mb-1.5">
        <span className="text-[13px] font-medium text-ink-2">
          {label}
          {required && <span className="text-red-600 ml-0.5" aria-hidden>*</span>}
        </span>
        {hint && <span className="text-xs text-ink-4">{hint}</span>}
      </label>
      {children}
      {error && <p className="text-xs text-red-700 mt-1">{error}</p>}
    </div>
  );
}
export const Input = forwardRef<HTMLInputElement, InputHTMLAttributes<HTMLInputElement>>(({ className, ...p }, ref) => (
  <input ref={ref} {...p} className={cx(fieldBase, 'h-10', className)} />
));
export const Select = forwardRef<HTMLSelectElement, SelectHTMLAttributes<HTMLSelectElement>>(({ className, children, ...p }, ref) => (
  <select ref={ref} {...p} className={cx(fieldBase, 'h-10 pr-8', className)}>{children}</select>
));
export const Textarea = forwardRef<HTMLTextAreaElement, TextareaHTMLAttributes<HTMLTextAreaElement>>(({ className, ...p }, ref) => (
  <textarea ref={ref} {...p} className={cx(fieldBase, 'py-2 resize-y min-h-20', className)} />
));

/** Segmented control (used for priority, gender). */
export function Segmented<T extends string>({ value, onChange, options, disabled, name }: {
  value: T; onChange: (v: T) => void; options: { value: T; label: string; tone?: string }[]; disabled?: boolean; name: string;
}) {
  return (
    <div role="radiogroup" aria-label={name} className="inline-flex w-full rounded-lg border border-line-strong bg-subtle p-0.5">
      {options.map((o) => {
        const active = o.value === value;
        return (
          <button
            key={o.value}
            type="button"
            role="radio"
            aria-checked={active}
            disabled={disabled}
            onClick={() => onChange(o.value)}
            className={cx(
              'flex-1 h-9 rounded-md text-[13px] font-medium transition-colors',
              active ? cx('bg-surface shadow-card text-ink ring-1 ring-line-strong', o.tone) : 'text-ink-3 hover:text-ink',
            )}
          >
            {o.label}
          </button>
        );
      })}
    </div>
  );
}

/* ------------------------------------------------------------------ Badges */
const tones = {
  neutral: 'bg-slate-100 text-slate-700 ring-slate-200',
  brand: 'bg-brand-50 text-brand-700 ring-brand-200',
  blue: 'bg-blue-50 text-blue-700 ring-blue-200',
  violet: 'bg-violet-50 text-violet-700 ring-violet-200',
  amber: 'bg-amber-50 text-amber-800 ring-amber-200',
  red: 'bg-red-50 text-red-700 ring-red-200',
  green: 'bg-emerald-50 text-emerald-700 ring-emerald-200',
} as const;
export type Tone = keyof typeof tones;

export function Badge({ tone = 'neutral', children, dot, className }: { tone?: Tone; children: ReactNode; dot?: boolean; className?: string }) {
  return (
    <span className={cx('inline-flex items-center gap-1.5 rounded-md px-2 py-0.5 text-xs font-medium ring-1 ring-inset whitespace-nowrap', tones[tone], className)}>
      {dot && <span className="size-1.5 rounded-full bg-current" />}
      {children}
    </span>
  );
}

const statusTone: Record<VisitStatus, Tone> = {
  REGISTERED: 'amber', TRIAGED: 'blue', IN_CONSULTATION: 'brand', AWAITING_LAB: 'violet',
  LAB_RESULTS_READY: 'green', COMPLETED: 'neutral', CANCELLED: 'neutral',
};
export const StatusBadge = ({ status }: { status: VisitStatus }) => <Badge tone={statusTone[status]} dot>{STATUS_LABEL[status]}</Badge>;

const labTone: Record<LabOrderStatus, Tone> = { PLACED: 'amber', SAMPLE_COLLECTED: 'blue', COMPLETED: 'green', CANCELLED: 'neutral' };
export const LabStatusBadge = ({ status }: { status: LabOrderStatus }) => <Badge tone={labTone[status]} dot>{LAB_STATUS_LABEL[status]}</Badge>;

export function PriorityBadge({ priority, quiet }: { priority: Priority; quiet?: boolean }) {
  if (priority === 'NORMAL') return quiet ? null : <Badge>{PRIORITY_LABEL.NORMAL}</Badge>;
  return <Badge tone={priority === 'EMERGENCY' ? 'red' : 'amber'}>{PRIORITY_LABEL[priority]}</Badge>;
}

const flagTone: Record<Level, Tone> = { ok: 'green', warn: 'amber', high: 'red', low: 'blue' };
export const FlagBadge = ({ flag }: { flag: Flag | null }) => (flag ? <Badge tone={flagTone[flag.level]}>{flag.text}</Badge> : null);
// eslint-disable-next-line react/only-export-components
export const flagText: Record<Level, string> = { ok: 'text-ink', warn: 'text-amber-700', high: 'text-red-700', low: 'text-blue-700' };

/** Token number chip: the patient's queue identity for the day. */
export function Token({ n, size = 'md' }: { n: number; size?: 'md' | 'lg' }) {
  return (
    <span className={cx(
      'inline-flex items-center justify-center rounded-lg bg-brand-50 text-brand-700 font-mono font-medium ring-1 ring-inset ring-brand-200 tabular-nums',
      size === 'lg' ? 'h-12 min-w-12 px-2 text-lg' : 'h-9 min-w-9 px-1.5 text-sm',
    )}>
      {String(n).padStart(2, '0')}
    </span>
  );
}

/* ------------------------------------------------------------------ Alerts and states */
const alertStyle = {
  error: { cls: 'bg-red-50 border-red-200 text-red-800', Icon: XCircle },
  warning: { cls: 'bg-amber-50 border-amber-200 text-amber-900', Icon: AlertTriangle },
  success: { cls: 'bg-emerald-50 border-emerald-200 text-emerald-900', Icon: CheckCircle2 },
  info: { cls: 'bg-blue-50 border-blue-200 text-blue-900', Icon: Info },
};
export function Alert({ kind = 'error', title, children, onClose, action }: {
  kind?: keyof typeof alertStyle; title?: ReactNode; children?: ReactNode; onClose?: () => void; action?: ReactNode;
}) {
  const { cls, Icon } = alertStyle[kind];
  return (
    <div role={kind === 'error' ? 'alert' : 'status'} className={cx('flex gap-3 rounded-lg border px-3.5 py-3 text-[13px] animate-fade-up', cls)}>
      <Icon className="size-4 mt-0.5 shrink-0" />
      <div className="flex-1 min-w-0">
        {title && <p className="font-semibold">{title}</p>}
        {children && <div className={title ? 'mt-0.5' : ''}>{children}</div>}
        {action && <div className="mt-2">{action}</div>}
      </div>
      {onClose && (
        <button onClick={onClose} aria-label="Dismiss" className="shrink-0 opacity-60 hover:opacity-100">
          <X className="size-4" />
        </button>
      )}
    </div>
  );
}

export function EmptyState({ icon, title, children }: { icon: ReactNode; title: string; children?: ReactNode }) {
  return (
    <div className="flex flex-col items-center justify-center text-center px-6 py-14">
      <div className="size-11 rounded-full bg-subtle border border-line grid place-items-center text-ink-4 mb-3">{icon}</div>
      <p className="text-sm font-medium text-ink-2">{title}</p>
      {children && <p className="text-[13px] text-ink-3 mt-1 max-w-xs">{children}</p>}
    </div>
  );
}

export function Spinner({ label = 'Loading…' }: { label?: string }) {
  return (
    <div className="flex items-center justify-center gap-2 py-14 text-[13px] text-ink-3">
      <Loader2 className="size-4 animate-spin" /> {label}
    </div>
  );
}

/** Label/value pair used in patient banners. */
export function Meta({ label, children, mono }: { label: string; children: ReactNode; mono?: boolean }) {
  return (
    <div className="min-w-0">
      <dt className="text-[11px] font-medium uppercase tracking-wide text-ink-4">{label}</dt>
      <dd className={cx('text-[13px] text-ink mt-0.5 truncate', mono && 'font-mono')}>{children}</dd>
    </div>
  );
}
