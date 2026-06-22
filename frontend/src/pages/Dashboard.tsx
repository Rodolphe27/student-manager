import { useEffect, useState } from 'react';
import StatCard from '../components/StatCard';
import { Student } from '../types';
import { Course } from '../types';
import { Enrollment } from '../types';
import studentService from '../services/studentService';
import courseService from '../services/courseService';
import enrollmentService from '../services/enrollmentService';
import { useAuth } from '../context/AuthContext';

interface Stats {
  students: number;
  courses: number;
  enrollments: number;
  pending: number;
}

export default function Dashboard() {
  const { user } = useAuth();
  const [stats, setStats]           = useState<Stats>({ students: 0, courses: 0, enrollments: 0, pending: 0 });
  const [enrollments, setEnrollments] = useState<Enrollment[]>([]);
  const [loading, setLoading]       = useState<boolean>(true);

  useEffect(() => {
    const fetchData = async (): Promise<void> => {
      try {
        const [s, c, e] = await Promise.all([
          studentService.getAll(),
          courseService.getAll(),
          enrollmentService.getAll(),
        ]);

        const students: Student[]     = s.data;
        const courses: Course[]       = c.data;
        const allEnrollments: Enrollment[] = e.data;
        const pending = allEnrollments.filter(en => en.status === 'PENDING').length;

        setStats({
          students:    students.length,
          courses:     courses.length,
          enrollments: allEnrollments.length,
          pending,
        });
        setEnrollments(allEnrollments.slice(0, 7));
      } catch (err) {
        console.error(err);
      } finally {
        setLoading(false);
      }
    };

    fetchData();
  }, []);

  if (loading) {
    return (
      <div className="flex items-center justify-center h-64">
        <div className="w-8 h-8 border-4 border-blue-600 border-t-transparent rounded-full animate-spin" />
      </div>
    );
  }

  return (
    <div className="p-6">
      <h1 className="text-xl font-bold text-gray-800 mb-1">Dashboard</h1>
      <p className="text-sm text-gray-400 mb-6">
        Welcome back, <span className="text-blue-600 font-medium">{user?.username}</span> 👋
      </p>

      {/* Stat Cards */}
      <div className="grid grid-cols-4 gap-4 mb-6">
        <StatCard label="Total Students"    value={stats.students}    sub="All time"       color="blue" />
        <StatCard label="Active Courses"    value={stats.courses}     sub="Available now"  color="green" />
        <StatCard label="Total Enrollments" value={stats.enrollments} sub="All time"       color="orange" />
        <StatCard label="Pending Reviews"   value={stats.pending}     sub="Need attention" color="red" />
      </div>

      {/* Recent Enrollments */}
      <div className="bg-white rounded-xl shadow-sm border border-gray-100">
        <div className="px-5 py-4 border-b border-gray-100">
          <h2 className="font-semibold text-gray-700">Recent Enrollments</h2>
        </div>
        <table className="w-full text-sm">
          <thead className="bg-gray-50 text-gray-400 text-xs uppercase">
            <tr>
              <th className="px-5 py-3 text-left">Student</th>
              <th className="px-5 py-3 text-left">Course</th>
              <th className="px-5 py-3 text-left">Enrolled At</th>
              <th className="px-5 py-3 text-left">Grade</th>
              <th className="px-5 py-3 text-left">Status</th>
            </tr>
          </thead>
          <tbody className="divide-y divide-gray-50">
            {enrollments.map((e: Enrollment) => (
              <tr key={e.id} className="hover:bg-gray-50">
                <td className="px-5 py-3 font-medium text-gray-800">{e.studentName}</td>
                <td className="px-5 py-3 text-gray-600">{e.courseTitle}</td>
                <td className="px-5 py-3 text-gray-500">{e.enrolledAt}</td>
                <td className="px-5 py-3 text-gray-600">{e.grade}</td>
                <td className="px-5 py-3">
                  <span className={`px-2 py-1 rounded-full text-xs font-medium ${
                    e.status === 'CONFIRMED' ? 'bg-green-100 text-green-700' :
                    e.status === 'PENDING'   ? 'bg-yellow-100 text-yellow-700' :
                                               'bg-red-100 text-red-700'
                  }`}>
                    {e.status}
                  </span>
                </td>
              </tr>
            ))}
            {enrollments.length === 0 && (
              <tr>
                <td colSpan={5} className="px-5 py-8 text-center text-gray-400 text-sm">
                  No enrollments yet
                </td>
              </tr>
            )}
          </tbody>
        </table>
      </div>
    </div>
  );
}
