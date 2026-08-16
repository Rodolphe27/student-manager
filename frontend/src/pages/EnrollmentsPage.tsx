import { useEffect, useState, type FormEvent } from 'react';
import type { Enrollment, Student, Course, CreateEnrollmentRequest, EnrollmentStatus } from '../types';
import enrollmentService from '../services/enrollmentService';
import studentService from '../services/studentService';
import courseService from '../services/courseService';

export default function EnrollmentsPage() {
  const [enrollments, setEnrollments] = useState<Enrollment[]>([]);
  const [students, setStudents]       = useState<Student[]>([]);
  const [courses, setCourses]         = useState<Course[]>([]);
  const [showForm, setShowForm]       = useState<boolean>(false);
  const [loading, setLoading]         = useState<boolean>(true);
  const [error, setError]             = useState<string>('');
  const [statusFilter, setStatusFilter] = useState<EnrollmentStatus | 'ALL'>('ALL');

  const [form, setForm] = useState<CreateEnrollmentRequest>({
    studentId: 0,
    courseId: 0,
  });

  useEffect(() => {
    const fetchAll = async (): Promise<void> => {
      try {
        const [e, s, c] = await Promise.all([
          enrollmentService.getAll(),
          studentService.getAll(),
          courseService.getAll(),
        ]);
        setEnrollments(e.data);
        setStudents(s.data);
        setCourses(c.data);
      } catch (err) {
        console.error(err);
      } finally {
        setLoading(false);
      }
    };
    fetchAll();
  }, []);

  const loadEnrollments = async (): Promise<void> => {
    const r = await enrollmentService.getAll();
    setEnrollments(r.data);
  };

  const handleSubmit = async (e: FormEvent<HTMLFormElement>): Promise<void> => {
    e.preventDefault();
    setError('');
    try {
      await enrollmentService.create(form);
      setForm({ studentId: 0, courseId: 0 });
      setShowForm(false);
      loadEnrollments();
    } catch (err: unknown) {
      const error = err as { response?: { data?: { message?: string } } };
      setError(error.response?.data?.message || 'Error creating enrollment');
    }
  };

  const handleConfirm = async (id: number): Promise<void> => {
    try {
      await enrollmentService.confirm(id);
      loadEnrollments();
    } catch (err) {
      console.error(err);
    }
  };

  const handleCancel = async (id: number): Promise<void> => {
    try {
      await enrollmentService.cancel(id);
      loadEnrollments();
    } catch (err) {
      console.error(err);
    }
  };

  const statusColor: Record<EnrollmentStatus, string> = {
    CONFIRMED: 'bg-green-100 text-green-700',
    PENDING:   'bg-yellow-100 text-yellow-700',
    CANCELLED: 'bg-red-100 text-red-600',
  };

  const filteredEnrollments = enrollments.filter((e: Enrollment) =>
    statusFilter === 'ALL' ? true : e.status === statusFilter
  );

  if (loading) {
    return (
      <div className="flex items-center justify-center h-64">
        <div className="w-8 h-8 border-4 border-blue-600 border-t-transparent rounded-full animate-spin" />
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
            {filteredEnrollments.length} of {enrollments.length}
          </p>
        </div>
        <div className="flex gap-2">
          <select
            value={statusFilter}
            onChange={(e) => setStatusFilter(e.target.value as EnrollmentStatus | 'ALL')}
            className="border border-gray-200 rounded-lg px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-blue-500"
          >
            <option value="ALL">All statuses</option>
            <option value="PENDING">Pending</option>
            <option value="CONFIRMED">Confirmed</option>
            <option value="CANCELLED">Cancelled</option>
          </select>
          <button
            onClick={() => setShowForm(!showForm)}
            className="bg-blue-600 text-white px-4 py-2 rounded-lg text-sm font-medium hover:bg-blue-700 transition-colors whitespace-nowrap"
          >
            + New Enrollment
          </button>
        </div>
      </div>

      {/* Form */}
      {showForm && (
        <div className="bg-white rounded-xl shadow-sm border border-gray-100 p-5 mb-6">
          <h2 className="font-semibold text-gray-700 mb-4">New Enrollment</h2>
          {error && (
            <div className="bg-red-50 border border-red-100 text-red-600 text-sm px-4 py-3 rounded-lg mb-4">
              {error}
            </div>
          )}
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
                {students.map((s: Student) => (
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
                  .filter((c: Course) => c.status === 'ACTIVE')
                  .map((c: Course) => (
                    <option key={c.id} value={c.id}>
                      {c.code} – {c.title}
                    </option>
                  ))}
              </select>
            </div>
            <div className="col-span-2 flex gap-2">
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
            {filteredEnrollments.map((e: Enrollment) => (
              <tr key={e.id} className="hover:bg-gray-50">
                <td className="px-5 py-3 font-medium text-gray-800">{e.studentName}</td>
                <td className="px-5 py-3 text-gray-600">
                  <span className="font-mono text-blue-600 text-xs">{e.courseCode}</span>
                  {' '}— {e.courseTitle}
                </td>
                <td className="px-5 py-3 text-gray-500">{e.enrolledAt}</td>
                <td className="px-5 py-3 text-gray-600 font-medium">{e.grade}</td>
                <td className="px-5 py-3">
                  <span className={`px-2 py-1 rounded-full text-xs font-medium ${statusColor[e.status]}`}>
                    {e.status}
                  </span>
                </td>
                <td className="px-5 py-3 flex gap-2">
                  {e.status === 'PENDING' && (
                    <button
                      onClick={() => handleConfirm(e.id)}
                      className="text-green-600 hover:text-green-800 text-xs font-medium"
                    >
                      Confirm
                    </button>
                  )}
                  {e.status !== 'CANCELLED' && (
                    <button
                      onClick={() => handleCancel(e.id)}
                      className="text-red-500 hover:text-red-700 text-xs font-medium"
                    >
                      Cancel
                    </button>
                  )}
                </td>
              </tr>
            ))}
            {filteredEnrollments.length === 0 && (
              <tr>
                <td colSpan={6} className="px-5 py-8 text-center text-gray-400 text-sm">
                  {enrollments.length === 0 ? 'No enrollments yet — add one above' : 'No enrollments match this filter'}
                </td>
              </tr>
            )}
          </tbody>
        </table>
      </div>
    </div>
  );
}
