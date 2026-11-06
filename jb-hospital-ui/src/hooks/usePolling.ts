import { useCallback, useEffect, useRef, useState } from 'react';
import { apiError } from '../lib/api';

/**
 * Loads data now, then every `intervalMs` while the tab is visible.
 * Background refreshes keep the last good data on screen instead of flashing a spinner.
 */
export function usePolling<T>(load: () => Promise<T>, deps: unknown[], intervalMs = 20000) {
  const [data, setData] = useState<T | null>(null);
  const [error, setError] = useState<string | null>(null);
  const [loading, setLoading] = useState(true);
  const [refreshing, setRefreshing] = useState(false);
  const loadRef = useRef(load);
  loadRef.current = load;

  const run = useCallback(async (background: boolean) => {
    if (background) setRefreshing(true); else setLoading(true);
    try {
      setData(await loadRef.current());
      setError(null);
    } catch (e) {
      setError(apiError(e, "Couldn't load data"));
    } finally {
      setLoading(false);
      setRefreshing(false);
    }
  }, []);

  useEffect(() => {
    run(false);
    if (!intervalMs) return;
    const id = setInterval(() => document.visibilityState === 'visible' && run(true), intervalMs);
    return () => clearInterval(id);
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, deps);

  return { data, error, loading, refreshing, reload: () => run(true), setData };
}
