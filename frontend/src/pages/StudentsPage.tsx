import { useCallback, useEffect, useState, type FormEvent } from 'react';
import type { Student, CreateStudentRequest, Page } from '../types';
import studentService from '../services/studentService';
import { getErrorMessage } from '../services/errorMessage';
import { usePermissions } from '../context/usePermissions';
import ErrorAlert from '../components/ErrorAlert';
import LoadingSpinner from '../components/LoadingSpinner';
import Pagination from '../components/Pagination';
import AccountFields from '../components/AccountFields';
import { accountPayload } from '../services/accountPayload';
import { useDebouncedValue } from '../hooks/useDebouncedValue';

const PAGE_SIZE = 10;

const emptyForm: CreateStudentRequest = {
  firstName: '',
  lastName: '',
  matriculationNumber: '',
  email: '',
};

export default function StudentsPage() {
  // Staff roster: TEACHERs may look, only an ADMIN may add, change or remove (see SecurityConfig).
  const { isAdmin } = usePermissions();
  const [actionError, setActionError] = useState<string>('');
  const [data, setData]           = useState<Page<Student> | null>(null);
  const [showForm, setShowForm]   = useState<boolean>(false);
  const [editingId, setEditingId] = useState<number | null>(null);
  const [loading, setLoading]     = useState<boolean>(true);
  const [loadError, setLoadError] = useState<string>('');
  const [error, setError]         = useState<string>('');
  const [query, setQuery]         = useState<string>('');
  const [page, setPage]           = useState<number>(1);

  const [form, setForm] = useState<CreateStudentRequest>(emptyForm);

  // Search and paging run on the server; the query waits for a typing pause.
  const debouncedQuery = useDebouncedValue(query.trim(), 300);

  const loadStudents = useCallback(async (): Promise<void> => {
    try {
      const r = await studentService.search({ q: debouncedQuery || undefined, page: page - 1, size: PAGE_SIZE });
      // Deleting the last row of the last page leaves it empty — step back one page.
      if (r.data.content.length === 0 && page > 1) {
        setPage(page - 1);
        return;
      }
      setData(r.data);
      setLoadError('');
    } catch (err) {
      console.error(err);
      setLoadError('Could not load students. Check your connection and try again.');
    } finally {
      setLoading(false);
    }
  }, [debouncedQuery, page]);

  // Fetch on mount and whenever the search or page changes; result lands via setState.
  // See CoursesPage for the rationale.
  // eslint-disable-next-line react-hooks/set-state-in-effect
  useEffect(() => { void loadStudents(); }, [loadStudents]);

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
      version: s.version,
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
        await studentService.create({ ...form, ...accountPayload(form) });
      }
      closeForm();
      setForm(emptyForm);
      loadStudents();
    } catch (err: unknown) {
      setError(getErrorMessage(err, `Error ${editingId !== null ? 'updating' : 'creating'} student`));
    }
  };

  const handleDelete = async (id: number): Promise<void> => {
    if (!confirm('Delete this student?')) return;
    setActionError('');
    try {
      await studentService.delete(id);
      void loadStudents();
    } catch (err) {
      setActionError(getErrorMessage(err, 'Could not delete the student'));
    }
  };

  const students   = data?.content ?? [];
  const total      = data?.page.totalElements ?? 0;
  const totalPages = data?.page.totalPages ?? 1;

  if (loading) return <LoadingSpinner />;

  if (loadError) {
    return (
      <div className="p-4 sm:p-6">
        <ErrorAlert
          message={loadError}
          actionLabel="Retry"
          onAction={() => { setLoading(true); void loadStudents(); }}
        />
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
            {total} {debouncedQuery ? 'matching' : 'total'}
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
          {isAdmin && (
            <button
              onClick={() => (showForm ? closeForm() : openCreateForm())}
            className="bg-blue-600 text-white px-4 py-2 rounded-lg text-sm font-medium hover:bg-blue-700 transition-colors whitespace-nowrap"
          >
            + Add Student
          </button>
          )}
        </div>
      </div>

      {actionError && (
        <ErrorAlert className="mb-6" message={actionError} actionLabel="Dismiss" onAction={() => setActionError('')} />
      )}

      {/* Form */}
      {isAdmin && showForm && (
        <div className="bg-white rounded-xl shadow-sm border border-gray-100 p-5 mb-6">
          <h2 className="font-semibold text-gray-700 mb-4">{editingId !== null ? 'Edit Student' : 'New Student'}</h2>
          {error && <ErrorAlert message={error} className="mb-4" />}
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
                minLength={2}
                maxLength={40}
                pattern="[A-Za-z0-9-]{2,40}"
                title="2-40 characters: letters, digits or '-'"
                required
              />
            </div>
            <div>
              <label className="block text-xs font-medium text-gray-500 mb-1">Email</label>
              <input
                type="email"
                className="w-full border border-gray-200 rounded-lg px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-blue-500"
                placeholder="anna@example.com"
                value={form.email}
                onChange={(e) => setForm({ ...form, email: e.target.value })}
                required
              />
            </div>
            {editingId === null && (
              <AccountFields values={form} onChange={(patch) => setForm({ ...form, ...patch })} />
            )}
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
              {isAdmin && <th className="px-5 py-3 text-left">Actions</th>}
            </tr>
          </thead>
          <tbody className="divide-y divide-gray-50">
            {students.map((s: Student) => (
              <tr key={s.id} className="hover:bg-gray-50">
                <td className="px-5 py-3 font-medium text-gray-800">{s.fullName}</td>
                <td className="px-5 py-3 text-gray-500 font-mono">{s.matriculationNumber}</td>
                <td className="px-5 py-3 text-gray-500">{s.email}</td>
                {isAdmin && (
                  <td className="px-5 py-3">
                    <div className="flex gap-3">
                      <button
                        onClick={() => openEditForm(s)}
                        className="text-blue-600 hover:text-blue-800 text-xs font-medium"
                      >
                        Edit
                      </button>
                      <button
                        onClick={() => void handleDelete(s.id)}
                        className="text-red-500 hover:text-red-700 text-xs font-medium"
                      >
                        Delete
                      </button>
                    </div>
                  </td>
                )}
              </tr>
            ))}
            {total === 0 && (
              <tr>
                <td colSpan={isAdmin ? 4 : 3} className="px-5 py-8 text-center text-gray-400 text-sm">
                  {debouncedQuery ? 'No students match your search' : isAdmin ? 'No students yet — add one above' : 'No students yet'}
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
