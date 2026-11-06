import { useEffect, useState } from 'react';
import { useLocation, useNavigate } from 'react-router-dom';

/** One-time success message passed through router state (e.g. "Vitals saved"), cleared so a refresh won't repeat it. */
export function useFlash() {
  const location = useLocation();
  const navigate = useNavigate();
  const [message, setMessage] = useState<string | null>((location.state as { saved?: string } | null)?.saved ?? null);
  useEffect(() => {
    if ((location.state as { saved?: string } | null)?.saved) navigate(location.pathname, { replace: true, state: null });
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, []);
  return [message, () => setMessage(null)] as const;
}
