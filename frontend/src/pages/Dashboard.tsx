import { useEffect, useState } from 'react';
import StatCard from '../components/StatCard';
import type { Enrollment } from '../types';
import studentService from '../services/studentService';
import courseService from '../services/courseService';
import enrollmentService from '../services/enrollmentService';
import { useAuth } from '../context/AuthContext';

interface Card {
  label: string;
  value: number;
  sub: string;
  color: 'blue' | 'green' | 'orange' | 'red';
}

export default function Dashboard() {
  const { user, loading: authLoading } = useAuth();
  const isStaff = user?.role === 'ADMIN' || user?.role === 'TEACHER';

  const [cards, setCards]             = useState<Card[]>([]);
  const [enrollments, setEnrollments] = useState<Enrollment[]>([]);
  const [loading, setLoading]         = useState<boolean>(true);
  const [loadError, setLoadError]     = useState<string>('');

  // Wait for the auth context to resolve before fetching — the role decides
  // which endpoints we're allowed to call.
  useEffect(() => {
    if (!authLoading) fetchData();
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [authLoading]);

  const fetchData = async (): Promise<void> => {
    setLoadError('');
    setLoading(true);
    try {
      if (isStaff) {
        // Staff see totals across everyone.
        const [s, c, e] = await Promise.all([
          studentService.getAll(),
          courseService.getAll(),
          enrollmentService.getAll(),
        ]);
        const all = e.data;
        setCards([
          { label: 'Total Students',    value: s.data.length, sub: 'All time',       color: 'blue' },
          { label: 'Active Courses',    value: c.data.length, sub: 'Available now',   color: 'green' },
          { label: 'Total Enrollments', value: all.length,    sub: 'All time',        color: 'orange' },
          { label: 'Pending Reviews',   value: all.filter(en => en.status === 'PENDING').length, sub: 'Need attention', color: 'red' },
        ]);
        setEnrollments(all.slice(0, 7));
      } else {
        // A STUDENT can't read the full student/enrollment lists (403), so the
        // dashboard is scoped to their own record.
        const courses = (await courseService.getAll()).data;

        let myId: number | null = null;
        try {
          myId = (await studentService.getMe()).data.id;
        } catch (err) {
          // 404 = no student record linked to this account yet; anything else is a real error.
          if ((err as { response?: { status?: number } }).response?.status !== 404) throw err;
        }

        const mine = myId != null ? (await enrollmentService.getByStudent(myId)).data : [];
        setCards([
          { label: 'Available Courses', value: courses.length, sub: 'In the catalog',    color: 'green' },
          { label: 'My Enrollments',    value: mine.length,    sub: 'All time',           color: 'blue' },
          { label: 'Confirmed',         value: mine.filter(en => en.status === 'CONFIRMED').length, sub: 'Active',          color: 'orange' },
          { label: 'Pending',           value: mine.filter(en => en.status === 'PENDING').length,   sub: 'Awaiting review', color: 'red' },
        ]);
        setEnrollments(mine.slice(0, 7));
      }
    } catch (err) {
      console.error(err);
      setLoadError('Could not load dashboard data. Check your connection and try again.');
    } finally {
      setLoading(false);
    }
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
            onClick={fetchData}
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
      <h1 className="text-xl font-bold text-gray-800 mb-1">Dashboard</h1>
      <p className="text-sm text-gray-400 mb-6">
        Welcome back, <span className="text-blue-600 font-medium">{user?.username}</span> 👋
      </p>

      {/* Stat Cards */}
      <div className="grid grid-cols-2 sm:grid-cols-4 gap-4 mb-6">
        {cards.map(card => (
          <StatCard key={card.label} label={card.label} value={card.value} sub={card.sub} color={card.color} />
        ))}
      </div>

      {/* Recent Enrollments */}
      <div className="bg-white rounded-xl shadow-sm border border-gray-100 overflow-x-auto">
        <div className="px-5 py-4 border-b border-gray-100">
          <h2 className="font-semibold text-gray-700">{isStaff ? 'Recent Enrollments' : 'My Recent Enrollments'}</h2>
        </div>
        <table className="w-full text-sm min-w-[600px]">
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
