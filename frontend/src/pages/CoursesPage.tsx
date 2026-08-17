import { useEffect, useState, type FormEvent } from 'react';
import type { Course, CreateCourseRequest, CourseStatus } from '../types';
import  courseService from '../services/courseService';

const emptyForm: CreateCourseRequest = {
  code: '',
  title: '',
  description: '',
  creditHours: 5,
  status: 'ACTIVE',
};

export default function CoursesPage() {
  const [courses, setCourses]   = useState<Course[]>([]);
  const [showForm, setShowForm] = useState<boolean>(false);
  const [editingId, setEditingId] = useState<number | null>(null);
  const [loading, setLoading]   = useState<boolean>(true);
  const [loadError, setLoadError] = useState<string>('');
  const [error, setError]       = useState<string>('');
  const [query, setQuery]       = useState<string>('');

  const [form, setForm] = useState<CreateCourseRequest>(emptyForm);

  useEffect(() => { loadCourses(); }, []);

  const loadCourses = async (): Promise<void> => {
    setLoadError('');
    try {
      const r = await courseService.getAll();
      setCourses(r.data);
    } catch (err) {
      console.error(err);
      setLoadError('Could not load courses. Check your connection and try again.');
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

  const openEditForm = (c: Course): void => {
    setEditingId(c.id);
    setForm({
      code: c.code,
      title: c.title,
      description: c.description ?? '',
      creditHours: c.creditHours,
      status: c.status,
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
        await courseService.update(editingId, form);
      } else {
        await courseService.create(form);
      }
      closeForm();
      setForm(emptyForm);
      loadCourses();
    } catch (err: unknown) {
      const error = err as { response?: { data?: { message?: string } } };
      setError(error.response?.data?.message || `Error ${editingId !== null ? 'updating' : 'creating'} course`);
    }
  };

  const handleDelete = async (id: number): Promise<void> => {
    if (!confirm('Delete this course?')) return;
    try {
      await courseService.delete(id);
      loadCourses();
    } catch (err) {
      console.error(err);
    }
  };

  const statusColor: Record<CourseStatus, string> = {
    ACTIVE:   'bg-green-100 text-green-700',
    INACTIVE: 'bg-gray-100 text-gray-600',
    ARCHIVED: 'bg-red-100 text-red-600',
  };

  const filteredCourses = courses.filter((c: Course) => {
    const q = query.trim().toLowerCase();
    if (!q) return true;
    return c.code.toLowerCase().includes(q) || c.title.toLowerCase().includes(q);
  });

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
            onClick={() => { setLoading(true); loadCourses(); }}
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
          <h1 className="text-xl font-bold text-gray-800">Courses</h1>
          <p className="text-sm text-gray-400">
            {filteredCourses.length} of {courses.length}
          </p>
        </div>
        <div className="flex gap-2">
          <input
            type="search"
            value={query}
            onChange={(e) => setQuery(e.target.value)}
            placeholder="Search by code or title…"
            className="flex-1 sm:w-72 border border-gray-200 rounded-lg px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-blue-500"
          />
          <button
            onClick={() => (showForm ? closeForm() : openCreateForm())}
            className="bg-blue-600 text-white px-4 py-2 rounded-lg text-sm font-medium hover:bg-blue-700 transition-colors whitespace-nowrap"
          >
            + Add Course
          </button>
        </div>
      </div>

      {/* Form */}
      {showForm && (
        <div className="bg-white rounded-xl shadow-sm border border-gray-100 p-5 mb-6">
          <h2 className="font-semibold text-gray-700 mb-4">{editingId !== null ? 'Edit Course' : 'New Course'}</h2>
          {error && (
            <div className="bg-red-50 border border-red-100 text-red-600 text-sm px-4 py-3 rounded-lg mb-4">
              {error}
            </div>
          )}
          <form onSubmit={handleSubmit} className="grid grid-cols-1 sm:grid-cols-2 gap-4">
            <div>
              <label className="block text-xs font-medium text-gray-500 mb-1">Code</label>
              <input
                className="w-full border border-gray-200 rounded-lg px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-blue-500"
                placeholder="ALG-101"
                value={form.code}
                onChange={(e) => setForm({ ...form, code: e.target.value })}
                required
              />
            </div>
            <div>
              <label className="block text-xs font-medium text-gray-500 mb-1">Title</label>
              <input
                className="w-full border border-gray-200 rounded-lg px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-blue-500"
                placeholder="Algorithmen & Datenstrukturen"
                value={form.title}
                onChange={(e) => setForm({ ...form, title: e.target.value })}
                required
              />
            </div>
            <div className="col-span-2">
              <label className="block text-xs font-medium text-gray-500 mb-1">Description</label>
              <input
                className="w-full border border-gray-200 rounded-lg px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-blue-500"
                placeholder="Optional description"
                value={form.description}
                onChange={(e) => setForm({ ...form, description: e.target.value })}
              />
            </div>
            <div>
              <label className="block text-xs font-medium text-gray-500 mb-1">Credit Hours (ECTS)</label>
              <input
                type="number"
                min={1}
                max={10}
                className="w-full border border-gray-200 rounded-lg px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-blue-500"
                value={form.creditHours}
                onChange={(e) => setForm({ ...form, creditHours: parseInt(e.target.value) })}
                required
              />
            </div>
            <div>
              <label className="block text-xs font-medium text-gray-500 mb-1">Status</label>
              <select
                className="w-full border border-gray-200 rounded-lg px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-blue-500"
                value={form.status}
                onChange={(e) => setForm({ ...form, status: e.target.value as CourseStatus })}
              >
                <option value="ACTIVE">Active</option>
                <option value="INACTIVE">Inactive</option>
                <option value="ARCHIVED">Archived</option>
              </select>
            </div>
            <div className="col-span-2 flex gap-2">
              <button
                type="submit"
                className="bg-blue-600 text-white px-4 py-2 rounded-lg text-sm font-medium hover:bg-blue-700"
              >
                {editingId !== null ? 'Update Course' : 'Save Course'}
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
              <th className="px-5 py-3 text-left">Code</th>
              <th className="px-5 py-3 text-left">Title</th>
              <th className="px-5 py-3 text-left">ECTS</th>
              <th className="px-5 py-3 text-left">Status</th>
              <th className="px-5 py-3 text-left">Actions</th>
            </tr>
          </thead>
          <tbody className="divide-y divide-gray-50">
            {filteredCourses.map((c: Course) => (
              <tr key={c.id} className="hover:bg-gray-50">
                <td className="px-5 py-3 font-mono text-blue-600 font-medium">{c.code}</td>
                <td className="px-5 py-3 text-gray-800">{c.title}</td>
                <td className="px-5 py-3 text-gray-600">{c.creditHours}</td>
                <td className="px-5 py-3">
                  <span className={`px-2 py-1 rounded-full text-xs font-medium ${statusColor[c.status]}`}>
                    {c.status}
                  </span>
                </td>
                <td className="px-5 py-3 flex gap-3">
                  <button
                    onClick={() => openEditForm(c)}
                    className="text-blue-600 hover:text-blue-800 text-xs font-medium"
                  >
                    Edit
                  </button>
                  <button
                    onClick={() => handleDelete(c.id)}
                    className="text-red-500 hover:text-red-700 text-xs font-medium"
                  >
                    Delete
                  </button>
                </td>
              </tr>
            ))}
            {filteredCourses.length === 0 && (
              <tr>
                <td colSpan={5} className="px-5 py-8 text-center text-gray-400 text-sm">
                  {courses.length === 0 ? 'No courses yet — add one above' : 'No courses match your search'}
                </td>
              </tr>
            )}
          </tbody>
        </table>
      </div>
    </div>
  );
}
