import { useCallback, useEffect, useState } from 'react';
import type { Course, Enrollment, EnrollmentStatus, Student } from '../types';
import studentService from '../services/studentService';
import enrollmentService from '../services/enrollmentService';
import courseService from '../services/courseService';
import { useAuth } from '../context/useAuth';

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
      const error = err as { response?: { data?: { message?: string } } };
      setEnrollError(error.response?.data?.message || 'Could not enroll in this course');
    } finally {
      setEnrollingId(null);
    }
  };

  const statusColor: Record<EnrollmentStatus, string> = {
    CONFIRMED: 'bg-green-100 text-green-700',
    PENDING:   'bg-yellow-100 text-yellow-700',
    CANCELLED: 'bg-red-100 text-red-600',
  };

  if (loading) {
    return (
      <div className="flex items-center justify-center h-64">
        <div className="w-8 h-8 border-4 border-blue-600 border-t-transparent rounded-full animate-spin" />
      </div>
    );
  }

  if (loadError) {
    return (
      <div className="p-4 sm:p-6">
        <div className="bg-red-50 border border-red-100 text-red-600 text-sm px-4 py-3 rounded-lg flex items-center justify-between gap-4">
          <span>{loadError}</span>
          <button
            onClick={() => { setLoading(true); fetchMine(); }}
            className="text-red-700 font-medium hover:underline whitespace-nowrap"
          >
            Retry
          </button>
        </div>
      </div>
    );
  }

  // A course is offered if it's ACTIVE and there's no enrollment for it yet —
  // CANCELLED enrollments still count as "already enrolled" here (issue #33's
  // uniqueness constraint has no re-enroll path yet), so those stay hidden too.
  const enrolledCourseIds = new Set(enrollments.map((e) => e.courseId));
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
          {enrollError && (
            <div className="bg-red-50 border border-red-100 text-red-600 text-sm px-4 py-3 rounded-lg mb-6 flex items-center justify-between gap-4">
              <span>{enrollError}</span>
              <button onClick={() => setEnrollError('')} className="text-red-700 font-medium hover:underline">
                Dismiss
              </button>
            </div>
          )}

          <div className="bg-white rounded-xl shadow-sm border border-gray-100 overflow-x-auto mb-6">
            <table className="w-full text-sm min-w-[560px]">
              <thead className="bg-gray-50 text-gray-400 text-xs uppercase">
                <tr>
                  <th className="px-5 py-3 text-left">Course</th>
                  <th className="px-5 py-3 text-left">Enrolled At</th>
                  <th className="px-5 py-3 text-left">Grade</th>
                  <th className="px-5 py-3 text-left">Status</th>
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
                    <td className="px-5 py-3 text-gray-600 font-medium">{e.grade}</td>
                    <td className="px-5 py-3">
                      <span className={`px-2 py-1 rounded-full text-xs font-medium ${statusColor[e.status]}`}>
                        {e.status}
                      </span>
                    </td>
                  </tr>
                ))}
                {enrollments.length === 0 && (
                  <tr>
                    <td colSpan={4} className="px-5 py-8 text-center text-gray-400 text-sm">
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
            <table className="w-full text-sm min-w-[560px]">
              <thead className="bg-gray-50 text-gray-400 text-xs uppercase">
                <tr>
                  <th className="px-5 py-3 text-left">Code</th>
                  <th className="px-5 py-3 text-left">Title</th>
                  <th className="px-5 py-3 text-left">ECTS</th>
                  <th className="px-5 py-3 text-left">Actions</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-gray-50">
                {availableCourses.map((c: Course) => (
                  <tr key={c.id} className="hover:bg-gray-50">
                    <td className="px-5 py-3 font-mono text-blue-600 font-medium">{c.code}</td>
                    <td className="px-5 py-3 text-gray-800">{c.title}</td>
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
                    <td colSpan={4} className="px-5 py-8 text-center text-gray-400 text-sm">
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
