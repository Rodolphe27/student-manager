import { useCallback, useEffect, useState } from 'react';
import type { Course, CourseStatus, CreateCourseRequest, Page, TeacherOption, Term } from '../types';
import courseService from '../services/courseService';
import teacherService from '../services/teacherService';
import termService from '../services/termService';
import { getErrorMessage } from '../services/errorMessage';
import { usePermissions } from '../context/usePermissions';
import CourseForm from '../components/CourseForm';
import ErrorAlert from '../components/ErrorAlert';
import LoadingSpinner from '../components/LoadingSpinner';
import Pagination from '../components/Pagination';
import StatusBadge from '../components/StatusBadge';
import { useDebouncedValue } from '../hooks/useDebouncedValue';

const PAGE_SIZE = 10;

const emptyForm: CreateCourseRequest = {
  code: '',
  title: '',
  description: '',
  creditHours: 5,
  status: 'ACTIVE',
  teacherId: null,
  termId: null,
};

export default function CoursesPage() {
  const { isAdmin, isTeacher, isStaff } = usePermissions();

  const [data, setData]           = useState<Page<Course> | null>(null);
  const [showForm, setShowForm]   = useState<boolean>(false);
  const [editingId, setEditingId] = useState<number | null>(null);
  const [loading, setLoading]     = useState<boolean>(true);
  const [loadError, setLoadError] = useState<string>('');
  const [error, setError]         = useState<string>('');
  const [actionError, setActionError] = useState<string>('');
  const [query, setQuery]         = useState<string>('');
  const [statusFilter, setStatusFilter] = useState<CourseStatus | 'ALL'>('ALL');
  const [page, setPage]           = useState<number>(1);
  const [form, setForm]           = useState<CreateCourseRequest>(emptyForm);

  const [terms, setTerms]       = useState<Term[]>([]);
  const [teachers, setTeachers] = useState<TeacherOption[]>([]);
  // A TEACHER's own teacher id: undefined while loading, null when the account has no profile.
  const [myTeacherId, setMyTeacherId] = useState<number | null | undefined>(undefined);

  // Search and paging run on the server; the query waits for a typing pause.
  const debouncedQuery = useDebouncedValue(query.trim(), 300);

  // Who may change which course: ADMIN any; a TEACHER only the courses they run.
  const canCreate = isAdmin || (isTeacher && myTeacherId != null);
  const canManage = (c: Course): boolean =>
    isAdmin || (isTeacher && myTeacherId != null && c.teacherId === myTeacherId);

  const loadCourses = useCallback(async (): Promise<void> => {
    try {
      const r = await courseService.search({
        q: debouncedQuery || undefined,
        status: statusFilter === 'ALL' ? undefined : statusFilter,
        page: page - 1,
        size: PAGE_SIZE,
      });
      // Deleting the last row of the last page leaves it empty — step back one page.
      if (r.data.content.length === 0 && page > 1) {
        setPage(page - 1);
        return;
      }
      setData(r.data);
      setLoadError('');
    } catch (err) {
      console.error(err);
      setLoadError('Could not load courses. Check your connection and try again.');
    } finally {
      setLoading(false);
    }
  }, [debouncedQuery, statusFilter, page]);

  // Lookup data for the form. Terms: staff. Teacher dropdown: ADMIN. Own teacher id: TEACHER.
  const loadLookups = useCallback(async (): Promise<void> => {
    if (isStaff) {
      setTerms((await termService.list()).data);
    }
    if (isAdmin) {
      setTeachers((await teacherService.options()).data);
    }
    if (isTeacher) {
      try {
        setMyTeacherId((await teacherService.getMe()).data.id);
      } catch (err) {
        const status = (err as { response?: { status?: number } }).response?.status;
        if (status !== 404) throw err;
        setMyTeacherId(null);
      }
    }
  }, [isStaff, isAdmin, isTeacher]);

  // Fetch-on-mount: the effect kicks off an async load whose result lands via
  // setState. react-hooks/set-state-in-effect flags every such pattern; it's
  // intentional here (revisit if this app adopts React Query / Suspense).
  // eslint-disable-next-line react-hooks/set-state-in-effect
  useEffect(() => { void loadCourses(); }, [loadCourses]);
  useEffect(() => {
    // eslint-disable-next-line react-hooks/set-state-in-effect
    loadLookups().catch((err) => console.error(err));
  }, [loadLookups]);

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
      teacherId: c.teacherId,
      termId: c.termId,
      version: c.version,
    });
    setError('');
    setShowForm(true);
  };

  const closeForm = (): void => {
    setShowForm(false);
    setEditingId(null);
    setError('');
  };

  const handleSubmit = async (): Promise<void> => {
    setError('');
    try {
      if (editingId !== null) {
        await courseService.update(editingId, form);
      } else {
        await courseService.create(form);
      }
      closeForm();
      setForm(emptyForm);
      void loadCourses();
    } catch (err: unknown) {
      setError(getErrorMessage(err, `Error ${editingId !== null ? 'updating' : 'creating'} course`));
    }
  };

  const handleDelete = async (c: Course): Promise<void> => {
    if (!confirm(`Delete course ${c.code}?`)) return;
    setActionError('');
    try {
      await courseService.delete(c.id);
      void loadCourses();
    } catch (err) {
      setActionError(getErrorMessage(err, 'Could not delete the course'));
    }
  };

  const courses    = data?.content ?? [];
  const total      = data?.page.totalElements ?? 0;
  const totalPages = data?.page.totalPages ?? 1;
  const filtered   = debouncedQuery !== '' || statusFilter !== 'ALL';
  const columnCount = isStaff ? 7 : 6;

  if (loading) return <LoadingSpinner />;

  if (loadError) {
    return (
      <div className="p-4 sm:p-6">
        <ErrorAlert
          message={loadError}
          actionLabel="Retry"
          onAction={() => { setLoading(true); void loadCourses(); }}
        />
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
            {total} {filtered ? 'matching' : 'total'}
          </p>
        </div>
        <div className="flex flex-wrap gap-2">
          <input
            type="search"
            value={query}
            onChange={(e) => { setQuery(e.target.value); setPage(1); }}
            placeholder="Search by code or title…"
            aria-label="Search courses"
            className="flex-1 sm:w-64 border border-gray-200 rounded-lg px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-blue-500"
          />
          <select
            value={statusFilter}
            onChange={(e) => { setStatusFilter(e.target.value as CourseStatus | 'ALL'); setPage(1); }}
            aria-label="Filter by status"
            className="border border-gray-200 rounded-lg px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-blue-500"
          >
            <option value="ALL">All statuses</option>
            <option value="ACTIVE">Active</option>
            <option value="INACTIVE">Inactive</option>
            <option value="ARCHIVED">Archived</option>
          </select>
          {canCreate && (
            <button
              onClick={() => (showForm ? closeForm() : openCreateForm())}
              className="bg-blue-600 text-white px-4 py-2 rounded-lg text-sm font-medium hover:bg-blue-700 transition-colors whitespace-nowrap"
            >
              + Add Course
            </button>
          )}
        </div>
      </div>

      {isTeacher && myTeacherId === null && (
        <ErrorAlert
          className="mb-6"
          message="Your account is not linked to a teacher profile yet, so you cannot create or manage courses. Ask an admin to link it."
        />
      )}
      {actionError && (
        <ErrorAlert className="mb-6" message={actionError} actionLabel="Dismiss" onAction={() => setActionError('')} />
      )}

      {/* Form */}
      {showForm && (
        <CourseForm
          title={editingId !== null ? 'Edit Course' : 'New Course'}
          submitLabel={editingId !== null ? 'Update Course' : 'Save Course'}
          value={form}
          onChange={setForm}
          onSubmit={() => void handleSubmit()}
          onCancel={closeForm}
          error={error}
          terms={terms}
          onTermCreated={(term) => setTerms((prev) => [term, ...prev])}
          teachers={isAdmin ? teachers : undefined}
          canAddTerm={isAdmin}
        />
      )}

      {/* Table */}
      <div className="bg-white rounded-xl shadow-sm border border-gray-100 overflow-x-auto">
        <table className="w-full text-sm min-w-[760px]">
          <thead className="bg-gray-50 text-gray-400 text-xs uppercase">
            <tr>
              <th className="px-5 py-3 text-left">Code</th>
              <th className="px-5 py-3 text-left">Title</th>
              <th className="px-5 py-3 text-left">Teacher</th>
              <th className="px-5 py-3 text-left">Term</th>
              <th className="px-5 py-3 text-left">ECTS</th>
              <th className="px-5 py-3 text-left">Status</th>
              {isStaff && <th className="px-5 py-3 text-left">Actions</th>}
            </tr>
          </thead>
          <tbody className="divide-y divide-gray-50">
            {courses.map((c: Course) => (
              <tr key={c.id} className="hover:bg-gray-50">
                <td className="px-5 py-3 font-mono text-blue-600 font-medium whitespace-nowrap">{c.code}</td>
                <td className="px-5 py-3 text-gray-800">{c.title}</td>
                <td className="px-5 py-3 text-gray-600">{c.teacherName ?? <span className="text-gray-300">—</span>}</td>
                <td className="px-5 py-3 text-gray-600">{c.termName ?? <span className="text-gray-300">—</span>}</td>
                <td className="px-5 py-3 text-gray-600">{c.creditHours}</td>
                <td className="px-5 py-3"><StatusBadge status={c.status} /></td>
                {isStaff && (
                  <td className="px-5 py-3">
                    {canManage(c) && (
                      <div className="flex gap-3">
                        <button
                          onClick={() => openEditForm(c)}
                          className="text-blue-600 hover:text-blue-800 text-xs font-medium"
                        >
                          Edit
                        </button>
                        <button
                          onClick={() => void handleDelete(c)}
                          className="text-red-500 hover:text-red-700 text-xs font-medium"
                        >
                          Delete
                        </button>
                      </div>
                    )}
                  </td>
                )}
              </tr>
            ))}
            {total === 0 && (
              <tr>
                <td colSpan={columnCount} className="px-5 py-8 text-center text-gray-400 text-sm">
                  {filtered ? 'No courses match your filter' : canCreate ? 'No courses yet — add one above' : 'No courses yet'}
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
