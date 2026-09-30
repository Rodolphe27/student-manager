import { useCallback, useEffect, useState } from 'react';
import type { Course, Enrollment, Student } from '../types';
import studentService from '../services/studentService';
import enrollmentService from '../services/enrollmentService';
import courseService from '../services/courseService';
import { getErrorMessage } from '../services/errorMessage';
import { useAuth } from '../context/useAuth';
import ErrorAlert from '../components/ErrorAlert';
import LoadingSpinner from '../components/LoadingSpinner';
import StatusBadge from '../components/StatusBadge';

export default function MyCoursesPage() {
  const { user } = useAuth();
  const [student, setStudent]         = useState<Student | null>(null);
  const [enrollments, setEnrollments] = useState<Enrollment[]>([]);
  const [courses, setCourses]         = useState<Course[]>([]);
  const [loading, setLoading]         = useState<boolean>(true);
  const [loadError, setLoadError]     = useState<string>('');
  const [linked, setLinked]           = useState<boolean>(true);
  const [enrollingId, setEnrollingId] = useState<number | null>(null);
  const [enrollError, setEnrollError] = useState<string>('');
  const [withdrawingId, setWithdrawingId] = useState<number | null>(null);
  const [markingRead, setMarkingRead] = useState<boolean>(false);

  const fetchMine = useCallback(async (): Promise<void> => {
    try {
      let mine: Student;
      try {
        mine = (await studentService.getMe()).data;
      } catch (err) {
        const status = (err as { response?: { status?: number } }).response?.status;
        if (status !== 404) throw err;
        setLinked(false);
        setEnrollments([]);
        return;
      }

      setLinked(true);
      setStudent(mine);
      const [enrollmentsRes, coursesRes] = await Promise.all([
        enrollmentService.getByStudent(mine.id),
        // Only ACTIVE courses can be enrolled in — ask the API for just those.
        courseService.getByStatus('ACTIVE'),
      ]);
      setEnrollments(enrollmentsRes.data);
      setCourses(coursesRes.data);
      setLoadError('');
    } catch (err) {
      console.error(err);
      setLoadError('Could not load your courses. Check your connection and try again.');
    } finally {
      setLoading(false);
    }
  }, []);

  // Fetch-on-mount; result lands via setState. See CoursesPage for the rationale.
  // eslint-disable-next-line react-hooks/set-state-in-effect
  useEffect(() => { void fetchMine(); }, [fetchMine]);

  const handleEnroll = async (courseId: number): Promise<void> => {
    if (!student) return;
    setEnrollError('');
    setEnrollingId(courseId);
    try {
      await enrollmentService.create({ studentId: student.id, courseId });
      const r = await enrollmentService.getByStudent(student.id);
      setEnrollments(r.data);
    } catch (err: unknown) {
      setEnrollError(getErrorMessage(err, 'Could not enroll in this course'));
    } finally {
      setEnrollingId(null);
    }
  };

  // Acknowledging clears the "new grade" notice (and the sidebar badge on the next navigation).
  const handleMarkGradesRead = async (): Promise<void> => {
    setMarkingRead(true);
    try {
      await Promise.all(enrollments.filter((e) => !e.gradeSeen).map((e) => enrollmentService.markGradeSeen(e.id)));
      if (student) setEnrollments((await enrollmentService.getByStudent(student.id)).data);
    } catch (err: unknown) {
      setEnrollError(getErrorMessage(err, 'Could not mark the grades as read'));
    } finally {
      setMarkingRead(false);
    }
  };

  // A student may withdraw while the enrollment is still PENDING; once a teacher has
  // confirmed it, only staff can cancel. (They can enroll again later.)
  const handleWithdraw = async (e: Enrollment): Promise<void> => {
    if (!confirm(`Withdraw from ${e.courseCode} — ${e.courseTitle}?`)) return;
    setEnrollError('');
    setWithdrawingId(e.id);
    try {
      await enrollmentService.cancel(e.id);
      if (student) setEnrollments((await enrollmentService.getByStudent(student.id)).data);
    } catch (err: unknown) {
      setEnrollError(getErrorMessage(err, 'Could not withdraw from this course'));
    } finally {
      setWithdrawingId(null);
    }
  };

  if (loading) return <LoadingSpinner />;

  if (loadError) {
    return (
      <div className="p-4 sm:p-6">
        <ErrorAlert
          message={loadError}
          actionLabel="Retry"
          onAction={() => { setLoading(true); void fetchMine(); }}
        />
      </div>
    );
  }

  // A course is offered if it's ACTIVE and the student has no live enrollment for it. A
  // CANCELLED one doesn't count: enrolling again reopens it as a new PENDING request.
  const newGrades = enrollments.filter((e) => !e.gradeSeen);
  const enrolledCourseIds = new Set(enrollments.filter((e) => e.status !== 'CANCELLED').map((e) => e.courseId));
  const availableCourses = courses.filter((c) => c.active && !enrolledCourseIds.has(c.id));

  return (
    <div className="p-4 sm:p-6">
      <div className="mb-6">
        <h1 className="text-xl font-bold text-gray-800">My Courses</h1>
        <p className="text-sm text-gray-400">{enrollments.length} enrollment{enrollments.length === 1 ? '' : 's'}</p>
      </div>

      {!linked ? (
        <div className="bg-white rounded-xl shadow-sm border border-gray-100 p-8 text-center text-gray-400 text-sm">
          No student record is linked to your account yet ({user?.email}). Ask an admin to add you as a student
          using this email address.
        </div>
      ) : (
        <>
          {newGrades.length > 0 && (
            <div
              role="status"
              className="bg-amber-50 border border-amber-200 text-amber-800 text-sm px-4 py-3 rounded-lg mb-6 flex items-center justify-between gap-4"
            >
              <span>
                🎓 {newGrades.length === 1
                  ? `A new grade is available for ${newGrades[0].courseCode}.`
                  : `${newGrades.length} new grades are available.`}
              </span>
              <button
                onClick={() => void handleMarkGradesRead()}
                disabled={markingRead}
                className="text-amber-900 font-medium hover:underline whitespace-nowrap disabled:opacity-50"
              >
                Mark as read
              </button>
            </div>
          )}
          {enrollError && (
            <ErrorAlert className="mb-6" message={enrollError} actionLabel="Dismiss" onAction={() => setEnrollError('')} />
          )}

          <div className="bg-white rounded-xl shadow-sm border border-gray-100 overflow-x-auto mb-6">
            <table className="w-full text-sm min-w-[560px]">
              <thead className="bg-gray-50 text-gray-400 text-xs uppercase">
                <tr>
                  <th className="px-5 py-3 text-left">Course</th>
                  <th className="px-5 py-3 text-left">Enrolled At</th>
                  <th className="px-5 py-3 text-left">Grade</th>
                  <th className="px-5 py-3 text-left">Status</th>
                  <th className="px-5 py-3 text-left">Actions</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-gray-50">
                {enrollments.map((e: Enrollment) => (
                  <tr key={e.id} className="hover:bg-gray-50">
                    <td className="px-5 py-3 text-gray-800">
                      <span className="font-mono text-blue-600 text-xs">{e.courseCode}</span>
                      {' '}— {e.courseTitle}
                    </td>
                    <td className="px-5 py-3 text-gray-500">{e.enrolledAt}</td>
                    <td className="px-5 py-3 text-gray-600 font-medium">
                      {e.grade === 'NOT_GRADED' ? '—' : e.grade}
                      {!e.gradeSeen && (
                        <span className="ml-2 bg-amber-100 text-amber-800 text-xs font-medium rounded-full px-2 py-0.5">New</span>
                      )}
                    </td>
                    <td className="px-5 py-3">
                      <StatusBadge status={e.status} />
                    </td>
                    <td className="px-5 py-3">
                      {e.status === 'PENDING' && (
                        <button
                          onClick={() => void handleWithdraw(e)}
                          disabled={withdrawingId === e.id}
                          className="text-amber-600 hover:text-amber-800 text-xs font-medium disabled:opacity-50"
                        >
                          {withdrawingId === e.id ? 'Withdrawing…' : 'Withdraw'}
                        </button>
                      )}
                    </td>
                  </tr>
                ))}
                {enrollments.length === 0 && (
                  <tr>
                    <td colSpan={5} className="px-5 py-8 text-center text-gray-400 text-sm">
                      You're not enrolled in any courses yet.
                    </td>
                  </tr>
                )}
              </tbody>
            </table>
          </div>

          <div className="mb-3">
            <h2 className="text-lg font-bold text-gray-800">Available Courses</h2>
            <p className="text-sm text-gray-400">Enroll yourself — new enrollments start as PENDING until a teacher confirms them.</p>
          </div>
          <div className="bg-white rounded-xl shadow-sm border border-gray-100 overflow-x-auto">
            <table className="w-full text-sm min-w-[720px]">
              <thead className="bg-gray-50 text-gray-400 text-xs uppercase">
                <tr>
                  <th className="px-5 py-3 text-left">Code</th>
                  <th className="px-5 py-3 text-left">Title</th>
                  <th className="px-5 py-3 text-left">Teacher</th>
                  <th className="px-5 py-3 text-left">Term</th>
                  <th className="px-5 py-3 text-left">ECTS</th>
                  <th className="px-5 py-3 text-left">Actions</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-gray-50">
                {availableCourses.map((c: Course) => (
                  <tr key={c.id} className="hover:bg-gray-50">
                    <td className="px-5 py-3 font-mono text-blue-600 font-medium whitespace-nowrap">{c.code}</td>
                    <td className="px-5 py-3 text-gray-800">{c.title}</td>
                    <td className="px-5 py-3 text-gray-600">{c.teacherName ?? '—'}</td>
                    <td className="px-5 py-3 text-gray-600">{c.termName ?? '—'}</td>
                    <td className="px-5 py-3 text-gray-600">{c.creditHours}</td>
                    <td className="px-5 py-3">
                      <button
                        onClick={() => handleEnroll(c.id)}
                        disabled={enrollingId === c.id}
                        className="bg-blue-600 text-white px-3 py-1.5 rounded-lg text-xs font-medium hover:bg-blue-700 disabled:opacity-50 disabled:cursor-not-allowed"
                      >
                        {enrollingId === c.id ? 'Enrolling…' : 'Enroll'}
                      </button>
                    </td>
                  </tr>
                ))}
                {availableCourses.length === 0 && (
                  <tr>
                    <td colSpan={6} className="px-5 py-8 text-center text-gray-400 text-sm">
                      No new courses available to enroll in right now.
                    </td>
                  </tr>
                )}
              </tbody>
            </table>
          </div>
        </>
      )}
    </div>
  );
}
