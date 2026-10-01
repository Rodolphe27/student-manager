import { useEffect, useState } from 'react';
import { useLocation } from 'react-router-dom';
import studentService from '../services/studentService';
import enrollmentService from '../services/enrollmentService';

/**
 * How many grades the logged-in student has not acknowledged yet. Re-checked on every
 * navigation, so the sidebar badge clears once the student has read them on My Courses.
 * Always 0 when `enabled` is false (staff) or the student has no linked record.
 */
export function useNewGradeCount(enabled: boolean): number {
  const { pathname } = useLocation();
  const [count, setCount] = useState(0);

  useEffect(() => {
    if (!enabled) return;
    let cancelled = false;
    studentService.getMe()
      .then((me) => enrollmentService.getByStudent(me.data.id))
      .then((r) => { if (!cancelled) setCount(r.data.filter((e) => !e.gradeSeen).length); })
      .catch(() => { if (!cancelled) setCount(0); });
    return () => { cancelled = true; };
  }, [enabled, pathname]);

  return enabled ? count : 0;
}
