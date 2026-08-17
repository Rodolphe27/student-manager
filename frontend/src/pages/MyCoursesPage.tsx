import { useEffect, useState } from 'react';
import type { Enrollment, EnrollmentStatus } from '../types';
import studentService from '../services/studentService';
import enrollmentService from '../services/enrollmentService';
import { useAuth } from '../context/AuthContext';

export default function MyCoursesPage() {
  const { user } = useAuth();
  const [enrollments, setEnrollments] = useState<Enrollment[]>([]);
  const [loading, setLoading]         = useState<boolean>(true);
  const [loadError, setLoadError]     = useState<string>('');
  const [linked, setLinked]           = useState<boolean>(true);

  useEffect(() => {
    fetchMine();
  }, []);

  const fetchMine = async (): Promise<void> => {
    setLoadError('');
    try {
      const students = (await studentService.getAll()).data;
      const mine = students.find((s) => s.email.toLowerCase() === user?.email?.toLowerCase());

      if (!mine) {
        setLinked(false);
        setEnrollments([]);
        return;
      }

      setLinked(true);
      const r = await enrollmentService.getByStudent(mine.id);
      setEnrollments(r.data);
    } catch (err) {
      console.error(err);
      setLoadError('Could not load your courses. Check your connection and try again.');
    } finally {
      setLoading(false);
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
        <div className="bg-white rounded-xl shadow-sm border border-gray-100 overflow-x-auto">
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
      )}
    </div>
  );
}
