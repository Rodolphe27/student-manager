import { useEffect, useState } from 'react';

/**
 * Returns `value` once it has stopped changing for `delayMs` — used so a
 * search box queries the API after the user pauses typing, not on every key.
 */
export function useDebouncedValue<T>(value: T, delayMs = 300): T {
  const [debounced, setDebounced] = useState<T>(value);

  useEffect(() => {
    const timer = setTimeout(() => setDebounced(value), delayMs);
    return () => clearTimeout(timer);
  }, [value, delayMs]);

  return debounced;
}
