import { useEffect, useState, type FormEvent } from 'react';
import type { Student, CreateStudentRequest } from '../types';
import studentService from '../services/studentService';

const PAGE_SIZE = 10;

const emptyForm: CreateStudentRequest = {
  firstName: '',
  lastName: '',
  matriculationNumber: '',
  email: '',
};

export default function StudentsPage() {
  const [students, setStudents]   = useState<Student[]>([]);
  const [showForm, setShowForm]   = useState<boolean>(false);
  const [editingId, setEditingId] = useState<number | null>(null);
  const [loading, setLoading]     = useState<boolean>(true);
  const [loadError, setLoadError] = useState<string>('');
  const [error, setError]         = useState<string>('');
  const [query, setQuery]         = useState<string>('');
  const [page, setPage]           = useState<number>(1);

  const [form, setForm] = useState<CreateStudentRequest>(emptyForm);

  useEffect(() => { loadStudents(); }, []);

  const loadStudents = async (): Promise<void> => {
    setLoadError('');
    try {
      const r = await studentService.getAll();
      setStudents(r.data);
    } catch (err) {
      console.error(err);
      setLoadError('Could not load students. Check your connection and try again.');
    } finally {
      setLoading(false);
    }
  };

  const openCreateForm = (): void => {
    setEditingId(null);
    setForm(emptyForm);
    setError('');
    setShowForm(true);
  };

  const openEditForm = (s: Student): void => {
    setEditingId(s.id);
    setForm({
      firstName: s.firstName,
      lastName: s.lastName,
      matriculationNumber: s.matriculationNumber,
      email: s.email,
      birthDate: s.birthDate ?? undefined,
    });
    setError('');
    setShowForm(true);
  };

  const closeForm = (): void => {
    setShowForm(false);
    setEditingId(null);
    setError('');
  };

  const handleSubmit = async (e: FormEvent<HTMLFormElement>): Promise<void> => {
    e.preventDefault();
    setError('');
    try {
      if (editingId !== null) {
        await studentService.update(editingId, form);
      } else {
        await studentService.create(form);
      }
      closeForm();
      setForm(emptyForm);
      loadStudents();
    } catch (err: unknown) {
      const error = err as { response?: { data?: { message?: string } } };
      setError(error.response?.data?.message || `Error ${editingId !== null ? 'updating' : 'creating'} student`);
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

  const totalPages = Math.max(1, Math.ceil(filteredStudents.length / PAGE_SIZE));
  const currentPage = Math.min(page, totalPages);
  const pagedStudents = filteredStudents.slice(
    (currentPage - 1) * PAGE_SIZE,
    currentPage * PAGE_SIZE
  );

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
            onClick={() => { setLoading(true); loadStudents(); }}
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
            onChange={(e) => { setQuery(e.target.value); setPage(1); }}
            placeholder="Search by name, matriculation, or email…"
            className="flex-1 sm:w-72 border border-gray-200 rounded-lg px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-blue-500"
          />
          <button
            onClick={() => (showForm ? closeForm() : openCreateForm())}
            className="bg-blue-600 text-white px-4 py-2 rounded-lg text-sm font-medium hover:bg-blue-700 transition-colors whitespace-nowrap"
          >
            + Add Student
          </button>
        </div>
      </div>

      {/* Form */}
      {showForm && (
        <div className="bg-white rounded-xl shadow-sm border border-gray-100 p-5 mb-6">
          <h2 className="font-semibold text-gray-700 mb-4">{editingId !== null ? 'Edit Student' : 'New Student'}</h2>
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
                {editingId !== null ? 'Update Student' : 'Save Student'}
              </button>
              <button
                type="button"
                onClick={closeForm}
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
            {pagedStudents.map((s: Student) => (
              <tr key={s.id} className="hover:bg-gray-50">
                <td className="px-5 py-3 font-medium text-gray-800">{s.fullName}</td>
                <td className="px-5 py-3 text-gray-500 font-mono">{s.matriculationNumber}</td>
                <td className="px-5 py-3 text-gray-500">{s.email}</td>
                <td className="px-5 py-3 flex gap-3">
                  <button
                    onClick={() => openEditForm(s)}
                    className="text-blue-600 hover:text-blue-800 text-xs font-medium"
                  >
                    Edit
                  </button>
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
        {filteredStudents.length > 0 && (
          <div className="flex items-center justify-between px-5 py-3 border-t border-gray-100">
            <span className="text-xs text-gray-400">
              Page {currentPage} of {totalPages}
            </span>
            <div className="flex gap-2">
              <button
                onClick={() => setPage((p) => Math.max(1, p - 1))}
                disabled={currentPage === 1}
                className="px-3 py-1.5 text-xs font-medium rounded-lg border border-gray-200 text-gray-600 hover:bg-gray-50 disabled:opacity-40 disabled:cursor-not-allowed"
              >
                Previous
              </button>
              <button
                onClick={() => setPage((p) => Math.min(totalPages, p + 1))}
                disabled={currentPage === totalPages}
                className="px-3 py-1.5 text-xs font-medium rounded-lg border border-gray-200 text-gray-600 hover:bg-gray-50 disabled:opacity-40 disabled:cursor-not-allowed"
              >
                Next
              </button>
            </div>
          </div>
        )}
      </div>
    </div>
  );
}
