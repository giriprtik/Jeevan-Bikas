import { render, screen, within } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { beforeEach, describe, expect, it, vi } from 'vitest';
import { LabApi } from '../../lib/services';
import type { LabOrder } from '../../lib/types';
import LabWorkspace from './index';

vi.mock('../../lib/services', () => ({ LabApi: { queue: vi.fn(), collectSample: vi.fn() } }));
const queue = vi.mocked(LabApi.queue);
const collectSample = vi.mocked(LabApi.collectSample);

function order(id: number, tests: string[], collected: string[] = []): LabOrder {
  return {
    id, visitId: id, patientMrn: `JB-00000${id}`, patientName: `Patient ${id}`, orderedByDoctor: 'doctor',
    orderStatus: collected.length === tests.length ? 'SAMPLE_COLLECTED' : 'PLACED',
    clinicalHistory: null, orderedAt: '2026-11-03T04:15:00Z', testCodes: tests,
    samples: tests.map((t) => ({ testCode: t, collected: collected.includes(t), collectedBy: collected.includes(t) ? 'labtech' : null })),
  } as LabOrder;
}

const row = async (patient: string) => (await screen.findByText(patient)).closest('li') as HTMLElement;

describe('Lab worklist: samples are collected one test at a time', () => {
  beforeEach(() => {
    queue.mockReset();
    collectSample.mockReset();
  });

  it('with several tests, the technician must pick one before collecting', async () => {
    queue.mockResolvedValue([order(1, ['CBC', 'LFT'])]);
    collectSample.mockResolvedValue(order(1, ['CBC', 'LFT'], ['LFT']));
    render(<LabWorkspace />);
    const li = await row('Patient 1');

    const collect = within(li).getByRole('button', { name: 'Collect sample' });
    expect(collect).toBeDisabled();
    expect(within(li).getByText('Click a test, then collect its sample.')).toBeInTheDocument();

    await userEvent.click(within(li).getByRole('radio', { name: 'LFT' }));
    expect(within(li).getByRole('radio', { name: 'LFT' })).toHaveAttribute('aria-checked', 'true');
    await userEvent.click(within(li).getByRole('button', { name: 'Collect LFT sample' }));

    expect(collectSample).toHaveBeenCalledWith(1, 'LFT');
    expect(await within(li).findByText('1 of 2 collected')).toBeInTheDocument();
    // LFT is done (no longer clickable); CBC is still pending and nothing is picked.
    expect(within(li).queryByRole('radio', { name: 'LFT' })).toBeNull();
    expect(within(li).getByRole('radio', { name: 'CBC' })).toHaveAttribute('aria-checked', 'false');
    expect(within(li).getByRole('button', { name: 'Collect sample' })).toBeDisabled();
  });

  it('a single-test order can be collected straight away', async () => {
    queue.mockResolvedValue([order(2, ['FBS'])]);
    render(<LabWorkspace />);
    const li = await row('Patient 2');

    expect(within(li).queryByRole('radio')).toBeNull();
    expect(within(li).getByRole('button', { name: 'Collect FBS sample' })).toBeEnabled();
  });

  it('results can only be entered once every sample is in', async () => {
    queue.mockResolvedValue([order(3, ['CBC', 'LFT'], ['CBC']), order(4, ['CBC', 'LFT'], ['CBC', 'LFT'])]);
    render(<LabWorkspace />);

    expect(within(await row('Patient 3')).getByRole('button', { name: 'Enter results' })).toBeDisabled();
    expect(within(await row('Patient 4')).getByRole('button', { name: 'Enter results' })).toBeEnabled();
  });

  it('shows the server message if a collection is rejected', async () => {
    queue.mockResolvedValue([order(5, ['CBC'])]);
    collectSample.mockRejectedValue(new Error('network down'));
    render(<LabWorkspace />);
    const li = await row('Patient 5');

    await userEvent.click(within(li).getByRole('button', { name: 'Collect CBC sample' }));

    expect(await screen.findByText("Couldn't record the sample")).toBeInTheDocument();
  });
});
