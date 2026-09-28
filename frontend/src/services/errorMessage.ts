// The backend's error messages are already curated for display (see
// GlobalExceptionHandler — every handler returns a hand-written string, never a raw
// exception message or stack trace). This helper centralizes the extraction that was
// previously duplicated across every page's catch block, and adds one defensive check:
// if a message ever looks stack-trace-shaped (a future regression, not something that
// happens today), fall back to the page's generic message instead of displaying it.
const SUSPICIOUS_PATTERN = /\bat\s+[\w.$]+\(|Exception\b|\.java:\d+/;
const MAX_LENGTH = 300;

export function getErrorMessage(err: unknown, fallback: string): string {
  const message = (err as { response?: { data?: { message?: string } } }).response?.data?.message;

  if (!message || message.length > MAX_LENGTH || SUSPICIOUS_PATTERN.test(message)) {
    return fallback;
  }

  return message;
}
