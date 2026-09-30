import type { CourseStatus, EnrollmentStatus } from '../types';

type Status = CourseStatus | EnrollmentStatus;

// One colour per meaning, shared by course and enrollment statuses.
const styles: Record<Status, string> = {
  ACTIVE:    'bg-green-100 text-green-700',
  CONFIRMED: 'bg-green-100 text-green-700',
  PENDING:   'bg-yellow-100 text-yellow-700',
  INACTIVE:  'bg-gray-100 text-gray-600',
  CANCELLED: 'bg-red-100 text-red-600',
  ARCHIVED:  'bg-red-100 text-red-600',
};

interface StatusBadgeProps {
  status: Status;
}

export default function StatusBadge({ status }: StatusBadgeProps) {
  return (
    <span className={`px-2 py-1 rounded-full text-xs font-medium ${styles[status]}`}>
      {status}
    </span>
  );
}
