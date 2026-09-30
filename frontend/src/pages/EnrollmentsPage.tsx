import { useCallback, useEffect, useState, type FormEvent } from 'react';
import type { Enrollment, CreateEnrollmentRequest, EnrollmentStatus, Grade, Page, StudentOption, CourseOption } from '../types';
import enrollmentService from '../services/enrollmentService';
import studentService from '../services/studentService';
import courseService from '../services/courseService';
import { getErrorMessage } from '../services/errorMessage';
import { usePermissions } from '../context/usePermissions';
import ErrorAlert from '../components/ErrorAlert';
import LoadingSpinner from '../components/LoadingSpinner';
import Pagination from '../components/Pagination';
import StatusBadge from '../components/StatusBadge';

const PAGE_SIZE = 10;

const GRADES: Grade[] = ['A', 'B', 'C', 'D', 'F', 'NOT_GRADED'];

export default function EnrollmentsPage() {
  // An ADMIN sees every enrollment; a TEACHER only those in courses they run (the API scopes
  // the list). DELETE is ADMIN-only (see SecurityConfig), so the action is hidden for teachers.
  const { isAdmin } = usePermissions();
  const canDelete = isAdmin;
  const [data, setData]               = useState<Page<Enrollment> | null>(null);
  const [students, setStudents]       = useState<StudentOption[]>([]);
  const [courses, setCourses]         = useState<CourseOption[]>([]);
  const [showForm, setShowForm]       = useState<boolean>(false);
  const [loading, setLoading]         = useState<boolean>(true);
  const [loadError, setLoadError]     = useState<string>('');
  const [error, setError]             = useState<string>('');
  const [actionError, setActionError] = useState<string>('');
  const [statusFilter, setStatusFilter] = useState<EnrollmentStatus | 'ALL'>('ALL');
  const [studentFilter, setStudentFilter] = useState<number | 'ALL'>('ALL');
  const [courseFilter, setCourseFilter]   = useState<number | 'ALL'>('ALL');
  const [page, setPage]               = useState<number>(1);

  const [form, setForm] = useState<CreateEnrollmentRequest>({
    studentId: 0,
    courseId: 0,
  });

  // Dropdown data: lightweight id/name projections, loaded once.
  const loadOptions = useCallback(async (): Promise<void> => {
    const [s, c] = await Promise.all([studentService.options(), courseService.options()]);
    setStudents(s.data);
    setCourses(c.data);
  }, []);

  // The table: one server-side page, filtered by the selected status/student/course.
  const loadEnrollments = useCallback(async (): Promise<void> => {
    try {
      const r = await enrollmentService.search({
        status:    statusFilter  === 'ALL' ? undefined : statusFilter,
        studentId: studentFilter === 'ALL' ? undefined : studentFilter,
        courseId:  courseFilter  === 'ALL' ? undefined : courseFilter,
        page: page - 1,
        size: PAGE_SIZE,
      });
      // Removing the last row of the last page leaves it empty — step back one page.
      if (r.data.content.length === 0 && page > 1) {
        setPage(page - 1);
        return;
      }
      setData(r.data);
      setLoadError('');
    } catch (err) {
      console.error(err);
      setLoadError('Could not load enrollments. Check your connection and try again.');
    } finally {
      setLoading(false);
    }
  }, [statusFilter, studentFilter, courseFilter, page]);

  // Fetch-on-mount (and on filter/page change); results land via setState.
  // See CoursesPage for the rationale.
  useEffect(() => {
    // eslint-disable-next-line react-hooks/set-state-in-effect
    loadOptions().catch((err) => console.error(err));
  }, [loadOptions]);
  // eslint-disable-next-line react-hooks/set-state-in-effect
  useEffect(() => { void loadEnrollments(); }, [loadEnrollments]);

  const handleSubmit = async (e: FormEvent<HTMLFormElement>): Promise<void> => {
    e.preventDefault();
    setError('');
    try {
      await enrollmentService.create(form);
      setForm({ studentId: 0, courseId: 0 });
      setShowForm(false);
      loadEnrollments();
    } catch (err: unknown) {
      setError(getErrorMessage(err, 'Error creating enrollment'));
    }
  };

  // Runs an enrollment action, shows the backend's reason when it fails, and reloads the list.
  const runAction = async (action: () => Promise<unknown>, fallback: string): Promise<void> => {
    setActionError('');
    try {
      await action();
      void loadEnrollments();
    } catch (err) {
      setActionError(getErrorMessage(err, fallback));
    }
  };

  const handleConfirm = (id: number): Promise<void> =>
    runAction(() => enrollmentService.confirm(id), 'Could not confirm the enrollment');

  const handleCancel = (e: Enrollment): Promise<void> =>
    confirm(`Cancel ${e.studentName}'s enrollment in ${e.courseCode}? Any grade is cleared.`)
      ? runAction(() => enrollmentService.cancel(e.id), 'Could not cancel the enrollment')
      : Promise.resolve();

  const handleGrade = (id: number, grade: Grade): Promise<void> =>
    runAction(() => enrollmentService.updateGrade(id, { grade }), 'Could not save the grade');

  const handleUnenroll = (id: number): Promise<void> =>
    confirm('Unenroll this student? This removes the enrollment record permanently.')
      ? runAction(() => enrollmentService.delete(id), 'Could not remove the enrollment')
      : Promise.resolve();

  const enrollments = data?.content ?? [];
  const total       = data?.page.totalElements ?? 0;
  const totalPages  = data?.page.totalPages ?? 1;
  const filtered    = statusFilter !== 'ALL' || studentFilter !== 'ALL' || courseFilter !== 'ALL';

  if (loading) return <LoadingSpinner />;

  if (loadError) {
    return (
      <div className="p-4 sm:p-6">
        <ErrorAlert
          message={loadError}
          actionLabel="Retry"
          onAction={() => { setLoading(true); void loadEnrollments(); loadOptions().catch(() => {}); }}
        />
      </div>
    );
  }

  return (
    <div className="p-4 sm:p-6">
      {/* Header */}
      <div className="flex flex-col sm:flex-row sm:justify-between sm:items-center gap-3 mb-6">
        <div>
          <h1 className="text-xl font-bold text-gray-800">Enrollments</h1>
          <p className="text-sm text-gray-400">
            {total} {filtered ? 'matching' : 'total'}{!isAdmin && ' · in your courses'}
          </p>
        </div>
        <div className="flex flex-wrap gap-2">
          <select
            value={statusFilter}
            onChange={(e) => { setStatusFilter(e.target.value as EnrollmentStatus | 'ALL'); setPage(1); }}
            className="border border-gray-200 rounded-lg px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-blue-500"
          >
            <option value="ALL">All statuses</option>
            <option value="PENDING">Pending</option>
            <option value="CONFIRMED">Confirmed</option>
            <option value="CANCELLED">Cancelled</option>
          </select>
          <select
            value={studentFilter}
            onChange={(e) => { setStudentFilter(e.target.value === 'ALL' ? 'ALL' : parseInt(e.target.value)); setPage(1); }}
            className="border border-gray-200 rounded-lg px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-blue-500"
          >
            <option value="ALL">All students</option>
            {students.map((s: StudentOption) => (
              <option key={s.id} value={s.id}>{s.fullName}</option>
            ))}
          </select>
          <select
            value={courseFilter}
            onChange={(e) => { setCourseFilter(e.target.value === 'ALL' ? 'ALL' : parseInt(e.target.value)); setPage(1); }}
            className="border border-gray-200 rounded-lg px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-blue-500"
          >
            <option value="ALL">All courses</option>
            {courses.map((c: CourseOption) => (
              <option key={c.id} value={c.id}>{c.code}</option>
            ))}
          </select>
          <button
            onClick={() => setShowForm(!showForm)}
            className="bg-blue-600 text-white px-4 py-2 rounded-lg text-sm font-medium hover:bg-blue-700 transition-colors whitespace-nowrap"
          >
            + New Enrollment
          </button>
        </div>
      </div>

      {actionError && (
        <ErrorAlert className="mb-6" message={actionError} actionLabel="Dismiss" onAction={() => setActionError('')} />
      )}

      {/* Form */}
      {showForm && (
        <div className="bg-white rounded-xl shadow-sm border border-gray-100 p-5 mb-6">
          <h2 className="font-semibold text-gray-700 mb-4">New Enrollment</h2>
          {error && <ErrorAlert message={error} className="mb-4" />}
          <form onSubmit={handleSubmit} className="grid grid-cols-1 sm:grid-cols-2 gap-4">
            <div>
              <label className="block text-xs font-medium text-gray-500 mb-1">Student</label>
              <select
                className="w-full border border-gray-200 rounded-lg px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-blue-500"
                value={form.studentId}
                onChange={(e) => setForm({ ...form, studentId: parseInt(e.target.value) })}
                required
              >
                <option value={0}>Select Student</option>
                {students.map((s: StudentOption) => (
                  <option key={s.id} value={s.id}>{s.fullName}</option>
                ))}
              </select>
            </div>
            <div>
              <label className="block text-xs font-medium text-gray-500 mb-1">Course</label>
              <select
                className="w-full border border-gray-200 rounded-lg px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-blue-500"
                value={form.courseId}
                onChange={(e) => setForm({ ...form, courseId: parseInt(e.target.value) })}
                required
              >
                <option value={0}>Select Course</option>
                {courses
                  .filter((c: CourseOption) => c.status === 'ACTIVE')
                  .map((c: CourseOption) => (
                    <option key={c.id} value={c.id}>
                      {c.code} – {c.title}
                    </option>
                  ))}
              </select>
            </div>
            <div className="sm:col-span-2 flex gap-2">
              <button
                type="submit"
                className="bg-blue-600 text-white px-4 py-2 rounded-lg text-sm font-medium hover:bg-blue-700"
              >
                Enroll
              </button>
              <button
                type="button"
                onClick={() => { setShowForm(false); setError(''); }}
                className="bg-gray-100 text-gray-600 px-4 py-2 rounded-lg text-sm font-medium hover:bg-gray-200"
              >
                Cancel
              </button>
            </div>
          </form>
        </div>
      )}

      {/* Table */}
      <div className="bg-white rounded-xl shadow-sm border border-gray-100 overflow-x-auto">
        <table className="w-full text-sm min-w-[720px]">
          <thead className="bg-gray-50 text-gray-400 text-xs uppercase">
            <tr>
              <th className="px-5 py-3 text-left">Student</th>
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
                <td className="px-5 py-3 font-medium text-gray-800">{e.studentName}</td>
                <td className="px-5 py-3 text-gray-600">
                  <span className="font-mono text-blue-600 text-xs">{e.courseCode}</span>
                  {' '}— {e.courseTitle}
                </td>
                <td className="px-5 py-3 text-gray-500">{e.enrolledAt}</td>
                <td className="px-5 py-3 text-gray-600 font-medium">
                  {e.status === 'CONFIRMED' ? (
                    <select
                      value={e.grade}
                      onChange={(ev) => void handleGrade(e.id, ev.target.value as Grade)}
                      aria-label={`Grade for ${e.studentName} in ${e.courseCode}`}
                      className="border border-gray-200 rounded-lg px-2 py-1 text-xs focus:outline-none focus:ring-2 focus:ring-blue-500"
                    >
                      {GRADES.map((g) => (
                        <option key={g} value={g}>{g === 'NOT_GRADED' ? 'Not graded' : g}</option>
                      ))}
                    </select>
                  ) : (
                    <span title="Grades can be given once an enrollment is confirmed">{e.grade === 'NOT_GRADED' ? '—' : e.grade}</span>
                  )}
                </td>
                <td className="px-5 py-3">
                  <StatusBadge status={e.status} />
                </td>
                <td className="px-5 py-3 flex gap-2">
                  {e.status === 'PENDING' && (
                    <button
                      onClick={() => void handleConfirm(e.id)}
                      className="text-green-600 hover:text-green-800 text-xs font-medium"
                    >
                      Confirm
                    </button>
                  )}
                  {e.status !== 'CANCELLED' && (
                    <button
                      onClick={() => void handleCancel(e)}
                      className="text-amber-600 hover:text-amber-800 text-xs font-medium"
                    >
                      Cancel
                    </button>
                  )}
                  {canDelete && (
                    <button
                      onClick={() => void handleUnenroll(e.id)}
                      className="text-red-500 hover:text-red-700 text-xs font-medium"
                    >
                      Unenroll
                    </button>
                  )}
                </td>
              </tr>
            ))}
            {total === 0 && (
              <tr>
                <td colSpan={6} className="px-5 py-8 text-center text-gray-400 text-sm">
                  {filtered ? 'No enrollments match this filter' : 'No enrollments yet — add one above'}
                </td>
              </tr>
            )}
          </tbody>
        </table>
        {total > 0 && <Pagination page={page} totalPages={totalPages} onChange={setPage} />}
      </div>
    </div>
  );
}
