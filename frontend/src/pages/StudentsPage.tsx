import { useEffect, useState, type FormEvent } from 'react';
import type { Student, CreateStudentRequest } from '../types';
import studentService from '../services/studentService';

export default function StudentsPage() {
  const [students, setStudents]   = useState<Student[]>([]);
  const [showForm, setShowForm]   = useState<boolean>(false);
  const [loading, setLoading]     = useState<boolean>(true);
  const [error, setError]         = useState<string>('');
  const [query, setQuery]         = useState<string>('');

  const [form, setForm] = useState<CreateStudentRequest>({
    firstName: '',
    lastName: '',
    matriculationNumber: '',
    email: '',
  });

  useEffect(() => { loadStudents(); }, []);

  const loadStudents = async (): Promise<void> => {
    try {
      const r = await studentService.getAll();
      setStudents(r.data);
    } catch (err) {
      console.error(err);
    } finally {
      setLoading(false);
    }
  };

  const handleSubmit = async (e: FormEvent<HTMLFormElement>): Promise<void> => {
    e.preventDefault();
    setError('');
    try {
      await studentService.create(form);
      setForm({ firstName: '', lastName: '', matriculationNumber: '', email: '' });
      setShowForm(false);
      loadStudents();
    } catch (err: unknown) {
      const error = err as { response?: { data?: { message?: string } } };
      setError(error.response?.data?.message || 'Error creating student');
    }
  };

  const handleDelete = async (id: number): Promise<void> => {
    if (!confirm('Delete this student?')) return;
    try {
      await studentService.delete(id);
      loadStudents();
    } catch (err) {
      console.error(err);
    }
  };

  const filteredStudents = students.filter((s: Student) => {
    const q = query.trim().toLowerCase();
    if (!q) return true;
    return (
      s.fullName.toLowerCase().includes(q) ||
      s.matriculationNumber.toLowerCase().includes(q) ||
      s.email.toLowerCase().includes(q)
    );
  });

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
          <h1 className="text-xl font-bold text-gray-800">Students</h1>
          <p className="text-sm text-gray-400">
            {filteredStudents.length} of {students.length}
          </p>
        </div>
        <div className="flex gap-2">
          <input
            type="search"
            value={query}
            onChange={(e) => setQuery(e.target.value)}
            placeholder="Search by name, matriculation, or email…"
            className="flex-1 sm:w-72 border border-gray-200 rounded-lg px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-blue-500"
          />
          <button
            onClick={() => setShowForm(!showForm)}
            className="bg-blue-600 text-white px-4 py-2 rounded-lg text-sm font-medium hover:bg-blue-700 transition-colors whitespace-nowrap"
          >
            + Add Student
          </button>
        </div>
      </div>

      {/* Form */}
      {showForm && (
        <div className="bg-white rounded-xl shadow-sm border border-gray-100 p-5 mb-6">
          <h2 className="font-semibold text-gray-700 mb-4">New Student</h2>
          {error && (
            <div className="bg-red-50 border border-red-100 text-red-600 text-sm px-4 py-3 rounded-lg mb-4">
              {error}
            </div>
          )}
          <form onSubmit={handleSubmit} className="grid grid-cols-1 sm:grid-cols-2 gap-4">
            <div>
              <label className="block text-xs font-medium text-gray-500 mb-1">First Name</label>
              <input
                className="w-full border border-gray-200 rounded-lg px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-blue-500"
                placeholder="Anna"
                value={form.firstName}
                onChange={(e) => setForm({ ...form, firstName: e.target.value })}
                required
              />
            </div>
            <div>
              <label className="block text-xs font-medium text-gray-500 mb-1">Last Name</label>
              <input
                className="w-full border border-gray-200 rounded-lg px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-blue-500"
                placeholder="Müller"
                value={form.lastName}
                onChange={(e) => setForm({ ...form, lastName: e.target.value })}
                required
              />
            </div>
            <div>
              <label className="block text-xs font-medium text-gray-500 mb-1">Matriculation Number</label>
              <input
                className="w-full border border-gray-200 rounded-lg px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-blue-500"
                placeholder="MT-12345"
                value={form.matriculationNumber}
                onChange={(e) => setForm({ ...form, matriculationNumber: e.target.value })}
                required
              />
            </div>
            <div>
              <label className="block text-xs font-medium text-gray-500 mb-1">Email</label>
              <input
                type="email"
                className="w-full border border-gray-200 rounded-lg px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-blue-500"
                placeholder="anna@fh-dortmund.de"
                value={form.email}
                onChange={(e) => setForm({ ...form, email: e.target.value })}
                required
              />
            </div>
            <div className="col-span-2 flex gap-2">
              <button
                type="submit"
                className="bg-blue-600 text-white px-4 py-2 rounded-lg text-sm font-medium hover:bg-blue-700"
              >
                Save Student
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
        <table className="w-full text-sm min-w-[560px]">
          <thead className="bg-gray-50 text-gray-400 text-xs uppercase">
            <tr>
              <th className="px-5 py-3 text-left">Name</th>
              <th className="px-5 py-3 text-left">Matriculation</th>
              <th className="px-5 py-3 text-left">Email</th>
              <th className="px-5 py-3 text-left">Actions</th>
            </tr>
          </thead>
          <tbody className="divide-y divide-gray-50">
            {filteredStudents.map((s: Student) => (
              <tr key={s.id} className="hover:bg-gray-50">
                <td className="px-5 py-3 font-medium text-gray-800">{s.fullName}</td>
                <td className="px-5 py-3 text-gray-500 font-mono">{s.matriculationNumber}</td>
                <td className="px-5 py-3 text-gray-500">{s.email}</td>
                <td className="px-5 py-3">
                  <button
                    onClick={() => handleDelete(s.id)}
                    className="text-red-500 hover:text-red-700 text-xs font-medium"
                  >
                    Delete
                  </button>
                </td>
              </tr>
            ))}
            {filteredStudents.length === 0 && (
              <tr>
                <td colSpan={4} className="px-5 py-8 text-center text-gray-400 text-sm">
                  {students.length === 0 ? 'No students yet — add one above' : 'No students match your search'}
                </td>
              </tr>
            )}
          </tbody>
        </table>
      </div>
    </div>
  );
}
