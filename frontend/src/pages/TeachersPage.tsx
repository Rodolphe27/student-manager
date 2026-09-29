import { useCallback, useEffect, useState, type FormEvent } from 'react';
import type { Teacher, CreateTeacherRequest, RegistrationInvite } from '../types';
import teacherService from '../services/teacherService';
import InviteModal from '../components/InviteModal';

const PAGE_SIZE = 10;

const emptyForm: CreateTeacherRequest = {
  firstName: '',
  lastName: '',
  email: '',
  department: '',
};

export default function TeachersPage() {
  const [teachers, setTeachers]   = useState<Teacher[]>([]);
  const [showForm, setShowForm]   = useState<boolean>(false);
  const [editingId, setEditingId] = useState<number | null>(null);
  const [loading, setLoading]     = useState<boolean>(true);
  const [loadError, setLoadError] = useState<string>('');
  const [error, setError]         = useState<string>('');
  const [query, setQuery]         = useState<string>('');
  const [page, setPage]           = useState<number>(1);
  const [invite, setInvite]       = useState<{ data: RegistrationInvite; teacherName: string } | null>(null);
  const [inviteError, setInviteError] = useState<string>('');

  const [form, setForm] = useState<CreateTeacherRequest>(emptyForm);

  const loadTeachers = useCallback(async (): Promise<void> => {
    try {
      const r = await teacherService.getAll();
      setTeachers(r.data);
      setLoadError('');
    } catch (err) {
      console.error(err);
      setLoadError('Could not load teachers. Check your connection and try again.');
    } finally {
      setLoading(false);
    }
  }, []);

  // Fetch-on-mount; result lands via setState. See CoursesPage for the rationale.
  // eslint-disable-next-line react-hooks/set-state-in-effect
  useEffect(() => { void loadTeachers(); }, [loadTeachers]);

  const openCreateForm = (): void => {
    setEditingId(null);
    setForm(emptyForm);
    setError('');
    setShowForm(true);
  };

  const openEditForm = (t: Teacher): void => {
    setEditingId(t.id);
    setForm({
      firstName: t.firstName,
      lastName: t.lastName,
      email: t.email,
      department: t.department ?? '',
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
      // department is optional on the backend, but its @Pattern only treats a
      // missing (null) value as valid — an empty string still has to match the
      // 2-100 char pattern and would be rejected. Omit it entirely when blank.
      const trimmedDepartment = form.department?.trim();
      const payload: CreateTeacherRequest = {
        ...form,
        department: trimmedDepartment ? trimmedDepartment : undefined,
      };
      if (editingId !== null) {
        await teacherService.update(editingId, payload);
      } else {
        await teacherService.create(payload);
      }
      closeForm();
      setForm(emptyForm);
      loadTeachers();
    } catch (err: unknown) {
      const error = err as { response?: { data?: { message?: string } } };
      setError(error.response?.data?.message || `Error ${editingId !== null ? 'updating' : 'creating'} teacher`);
    }
  };

  const handleDelete = async (id: number): Promise<void> => {
    if (!confirm('Delete this teacher?')) return;
    try {
      await teacherService.delete(id);
      loadTeachers();
    } catch (err) {
      console.error(err);
    }
  };

  const handleInvite = async (t: Teacher): Promise<void> => {
    setInviteError('');
    try {
      const r = await teacherService.issueInvite(t.id);
      setInvite({ data: r.data, teacherName: t.fullName });
    } catch (err: unknown) {
      const error = err as { response?: { data?: { message?: string } } };
      setInviteError(error.response?.data?.message || `Could not send invite for ${t.fullName}`);
    }
  };

  const filteredTeachers = teachers.filter((t: Teacher) => {
    const q = query.trim().toLowerCase();
    if (!q) return true;
    return (
      t.fullName.toLowerCase().includes(q) ||
      t.email.toLowerCase().includes(q) ||
      (t.department ?? '').toLowerCase().includes(q)
    );
  });

  const totalPages = Math.max(1, Math.ceil(filteredTeachers.length / PAGE_SIZE));
  const currentPage = Math.min(page, totalPages);
  const pagedTeachers = filteredTeachers.slice(
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
            onClick={() => { setLoading(true); loadTeachers(); }}
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
          <h1 className="text-xl font-bold text-gray-800">Teachers</h1>
          <p className="text-sm text-gray-400">
            {filteredTeachers.length} of {teachers.length}
          </p>
        </div>
        <div className="flex gap-2">
          <input
            type="search"
            value={query}
            onChange={(e) => { setQuery(e.target.value); setPage(1); }}
            placeholder="Search by name, email, or department…"
            className="flex-1 sm:w-72 border border-gray-200 rounded-lg px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-blue-500"
          />
          <button
            onClick={() => (showForm ? closeForm() : openCreateForm())}
            className="bg-blue-600 text-white px-4 py-2 rounded-lg text-sm font-medium hover:bg-blue-700 transition-colors whitespace-nowrap"
          >
            + Add Teacher
          </button>
        </div>
      </div>

      {inviteError && (
        <div className="bg-red-50 border border-red-100 text-red-600 text-sm px-4 py-3 rounded-lg mb-6 flex items-center justify-between gap-4">
          <span>{inviteError}</span>
          <button onClick={() => setInviteError('')} className="text-red-700 font-medium hover:underline">
            Dismiss
          </button>
        </div>
      )}

      {invite && (
        <InviteModal
          invite={invite.data}
          targetName={invite.teacherName}
          onClose={() => setInvite(null)}
        />
      )}

      {/* Form */}
      {showForm && (
        <div className="bg-white rounded-xl shadow-sm border border-gray-100 p-5 mb-6">
          <h2 className="font-semibold text-gray-700 mb-4">{editingId !== null ? 'Edit Teacher' : 'New Teacher'}</h2>
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
                placeholder="Jan"
                value={form.firstName}
                onChange={(e) => setForm({ ...form, firstName: e.target.value })}
                required
              />
            </div>
            <div>
              <label className="block text-xs font-medium text-gray-500 mb-1">Last Name</label>
              <input
                className="w-full border border-gray-200 rounded-lg px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-blue-500"
                placeholder="Schmidt"
                value={form.lastName}
                onChange={(e) => setForm({ ...form, lastName: e.target.value })}
                required
              />
            </div>
            <div>
              <label className="block text-xs font-medium text-gray-500 mb-1">Email</label>
              <input
                type="email"
                className="w-full border border-gray-200 rounded-lg px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-blue-500"
                placeholder="jan.schmidt@example.com"
                value={form.email}
                onChange={(e) => setForm({ ...form, email: e.target.value })}
                required
              />
            </div>
            <div>
              <label className="block text-xs font-medium text-gray-500 mb-1">Department</label>
              <input
                className="w-full border border-gray-200 rounded-lg px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-blue-500"
                placeholder="Computer Science"
                value={form.department}
                onChange={(e) => setForm({ ...form, department: e.target.value })}
                pattern="[A-Za-z .'-]{2,100}"
                title="2-100 characters: letters, spaces, '.', &apos;&apos; or '-' — leave blank to omit"
              />
            </div>
            <div className="col-span-2 flex gap-2">
              <button
                type="submit"
                className="bg-blue-600 text-white px-4 py-2 rounded-lg text-sm font-medium hover:bg-blue-700"
              >
                {editingId !== null ? 'Update Teacher' : 'Save Teacher'}
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
        <table className="w-full text-sm min-w-[640px]">
          <thead className="bg-gray-50 text-gray-400 text-xs uppercase">
            <tr>
              <th className="px-5 py-3 text-left">Name</th>
              <th className="px-5 py-3 text-left">Email</th>
              <th className="px-5 py-3 text-left">Department</th>
              <th className="px-5 py-3 text-left">Actions</th>
            </tr>
          </thead>
          <tbody className="divide-y divide-gray-50">
            {pagedTeachers.map((t: Teacher) => (
              <tr key={t.id} className="hover:bg-gray-50">
                <td className="px-5 py-3 font-medium text-gray-800">{t.fullName}</td>
                <td className="px-5 py-3 text-gray-500">{t.email}</td>
                <td className="px-5 py-3 text-gray-500">{t.department || '—'}</td>
                <td className="px-5 py-3 flex gap-3">
                  <button
                    onClick={() => handleInvite(t)}
                    className="text-green-600 hover:text-green-800 text-xs font-medium"
                  >
                    Send Invite
                  </button>
                  <button
                    onClick={() => openEditForm(t)}
                    className="text-blue-600 hover:text-blue-800 text-xs font-medium"
                  >
                    Edit
                  </button>
                  <button
                    onClick={() => handleDelete(t.id)}
                    className="text-red-500 hover:text-red-700 text-xs font-medium"
                  >
                    Delete
                  </button>
                </td>
              </tr>
            ))}
            {filteredTeachers.length === 0 && (
              <tr>
                <td colSpan={4} className="px-5 py-8 text-center text-gray-400 text-sm">
                  {teachers.length === 0 ? 'No teachers yet — add one above' : 'No teachers match your search'}
                </td>
              </tr>
            )}
          </tbody>
        </table>
        {filteredTeachers.length > 0 && (
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
