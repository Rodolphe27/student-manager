interface ErrorAlertProps {
  message: string;
  /** Label + handler of the optional action on the right (e.g. "Retry", "Dismiss"). */
  actionLabel?: string;
  onAction?: () => void;
  className?: string;
}

/** The red error box used for load failures, failed actions and form errors. */
export default function ErrorAlert({ message, actionLabel, onAction, className = '' }: ErrorAlertProps) {
  return (
    <div
      role="alert"
      className={`bg-red-50 border border-red-100 text-red-600 text-sm px-4 py-3 rounded-lg flex items-center justify-between gap-4 ${className}`}
    >
      <span>{message}</span>
      {actionLabel && onAction && (
        <button onClick={onAction} className="text-red-700 font-medium hover:underline whitespace-nowrap">
          {actionLabel}
        </button>
      )}
    </div>
  );
}
