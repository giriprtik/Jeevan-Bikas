import { describe, expect, it } from 'vitest';
import { ageSex, bpFlag, dateTime, initials, pulseFlag, spo2Flag, tempFlag, time } from './format';

describe('vital-sign flags (must agree with the OPD service thresholds)', () => {
  it.each([
    [37.8, 'ok'], [37.9, 'high'], [35.5, 'ok'], [35.4, 'low'],
  ])('temperature %s°C is %s', (t, level) => {
    expect(tempFlag(t)?.level).toBe(level);
  });

  it.each([
    [92, 'warn'], [91, 'high'], [95, 'ok'], [94, 'warn'],
  ])('SpO2 %s%% is %s', (s, level) => {
    expect(spo2Flag(s)?.level).toBe(level);
  });

  it.each([
    [100, 'ok'], [101, 'high'], [60, 'ok'], [59, 'warn'],
  ])('pulse %s is %s', (p, level) => {
    expect(pulseFlag(p)?.level).toBe(level);
  });

  it.each([
    ['120/79', 'ok', 'Normal'],
    ['130/70', 'warn', 'Elevated'],
    ['118/82', 'warn', 'Elevated'],
    ['140/85', 'high', 'Hypertensive'],
    ['85/55', 'low', 'Hypotensive'],
  ])('blood pressure %s is %s', (bp, level, text) => {
    expect(bpFlag(bp)).toEqual({ level, text });
  });

  it('ignores missing or malformed readings', () => {
    expect(tempFlag(undefined)).toBeNull();
    expect(tempFlag(Number.NaN)).toBeNull();
    expect(spo2Flag(null)).toBeNull();
    expect(bpFlag('120-80')).toBeNull();
    expect(bpFlag('')).toBeNull();
  });
});

describe('display helpers', () => {
  it('formats age and sex compactly', () => {
    expect(ageSex(34, 'FEMALE')).toBe('34y · F');
    expect(ageSex(null, null)).toBe('– · –');
  });

  it('builds initials without the Dr. prefix', () => {
    expect(initials('Dr. Anjali Karki')).toBe('AK');
    expect(initials('Sita Thapa')).toBe('ST');
    expect(initials('Madonna')).toBe('M');
  });

  it('shows times in hospital time (Asia/Kathmandu, UTC+05:45)', () => {
    expect(time('2026-11-03T04:15:00Z')).toBe('10:00');
    expect(dateTime('2026-11-03T18:20:00Z')).toBe('04 Nov, 00:05');
    expect(time(null)).toBe('–');
  });
});
