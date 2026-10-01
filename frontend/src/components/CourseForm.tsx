import { useState, type FormEvent } from 'react';
import type { CourseStatus, CreateCourseRequest, TeacherOption, Term } from '../types';
import termService from '../services/termService';
import { getErrorMessage } from '../services/errorMessage';
import ErrorAlert from './ErrorAlert';

const inputClass =
  'w-full border border-gray-200 rounded-lg px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-blue-500';

interface CourseFormProps {
  title: string;
  submitLabel: string;
  value: CreateCourseRequest;
  onChange: (value: CreateCourseRequest) => void;
  onSubmit: () => void;
  onCancel: () => void;
  error: string;
  terms: Term[];
  /** Called after an admin adds a term here, with the new term, so the parent can reload its list. */
  onTermCreated: (term: Term) => void;
  /** ADMIN only: who runs the course. A TEACHER always becomes the course's teacher. */
  teachers?: TeacherOption[];
  canAddTerm: boolean;
}

/** Create / edit form for a course. Owns only presentation and the inline "new term" helper. */
export default function CourseForm({
  title, submitLabel, value, onChange, onSubmit, onCancel, error, terms, onTermCreated, teachers, canAddTerm,
}: CourseFormProps) {
  const [addingTerm, setAddingTerm] = useState(false);
  const [termName, setTermName] = useState('');
  const [termStart, setTermStart] = useState('');
  const [termEnd, setTermEnd] = useState('');
  const [termError, setTermError] = useState('');

  const handleSubmit = (e: FormEvent<HTMLFormElement>): void => {
    e.preventDefault();
    onSubmit();
  };

  const saveTerm = async (): Promise<void> => {
    setTermError('');
    try {
      const created = (await termService.create({
        name: termName.trim(),
        startDate: termStart || undefined,
        endDate: termEnd || undefined,
      })).data;
      onTermCreated(created);
      onChange({ ...value, termId: created.id });
      setAddingTerm(false);
      setTermName(''); setTermStart(''); setTermEnd('');
    } catch (err: unknown) {
      setTermError(getErrorMessage(err, 'Could not create the term'));
    }
  };

  return (
    <div className="bg-white rounded-xl shadow-sm border border-gray-100 p-5 mb-6">
      <h2 className="font-semibold text-gray-700 mb-4">{title}</h2>
      {error && <ErrorAlert message={error} className="mb-4" />}
      <form onSubmit={handleSubmit} className="grid grid-cols-1 sm:grid-cols-2 gap-4">
        <div>
          <label htmlFor="course-code" className="block text-xs font-medium text-gray-500 mb-1">Code</label>
          <input
            id="course-code"
            className={inputClass}
            placeholder="CS-101"
            value={value.code}
            onChange={(e) => onChange({ ...value, code: e.target.value })}
            pattern="[A-Za-z0-9]+(-[A-Za-z0-9]+)*"
            title="Alphanumeric segments separated by '-' (e.g. CS-101)"
            required
          />
        </div>
        <div>
          <label htmlFor="course-title" className="block text-xs font-medium text-gray-500 mb-1">Title</label>
          <input
            id="course-title"
            className={inputClass}
            placeholder="Algorithms & Data Structures"
            value={value.title}
            onChange={(e) => onChange({ ...value, title: e.target.value })}
            required
          />
        </div>
        <div className="sm:col-span-2">
          <label htmlFor="course-description" className="block text-xs font-medium text-gray-500 mb-1">Description</label>
          <input
            id="course-description"
            className={inputClass}
            placeholder="Optional description"
            value={value.description}
            onChange={(e) => onChange({ ...value, description: e.target.value })}
          />
        </div>
        <div>
          <label htmlFor="course-credits" className="block text-xs font-medium text-gray-500 mb-1">Credit Hours (ECTS)</label>
          <input
            id="course-credits"
            type="number"
            min={1}
            max={10}
            className={inputClass}
            value={value.creditHours}
            onChange={(e) => onChange({ ...value, creditHours: parseInt(e.target.value) })}
            required
          />
        </div>
        <div>
          <label htmlFor="course-status" className="block text-xs font-medium text-gray-500 mb-1">Status</label>
          <select
            id="course-status"
            className={inputClass}
            value={value.status}
            onChange={(e) => onChange({ ...value, status: e.target.value as CourseStatus })}
          >
            <option value="ACTIVE">Active</option>
            <option value="INACTIVE">Inactive</option>
            <option value="ARCHIVED">Archived</option>
          </select>
        </div>
        {teachers && (
          <div>
            <label htmlFor="course-teacher" className="block text-xs font-medium text-gray-500 mb-1">Teacher</label>
            <select
              id="course-teacher"
              className={inputClass}
              value={value.teacherId ?? ''}
              onChange={(e) => onChange({ ...value, teacherId: e.target.value ? parseInt(e.target.value) : null })}
            >
              <option value="">— Unassigned —</option>
              {teachers.map((t) => (
                <option key={t.id} value={t.id}>
                  {t.fullName}{t.department ? ` (${t.department})` : ''}
                </option>
              ))}
            </select>
          </div>
        )}
        <div>
          <label htmlFor="course-term" className="block text-xs font-medium text-gray-500 mb-1">Term</label>
          <select
            id="course-term"
            className={inputClass}
            value={value.termId ?? ''}
            onChange={(e) => onChange({ ...value, termId: e.target.value ? parseInt(e.target.value) : null })}
          >
            <option value="">— No term —</option>
            {terms.map((t) => (
              <option key={t.id} value={t.id}>{t.name}</option>
            ))}
          </select>
          {canAddTerm && !addingTerm && (
            <button
              type="button"
              onClick={() => setAddingTerm(true)}
              className="mt-1 text-blue-600 hover:text-blue-800 text-xs font-medium"
            >
              + New term
            </button>
          )}
        </div>

        {canAddTerm && addingTerm && (
          <fieldset className="sm:col-span-2 border border-gray-100 rounded-lg p-4 bg-gray-50">
            <legend className="px-1 text-xs font-medium text-gray-500">New term</legend>
            {termError && <ErrorAlert message={termError} className="mb-3" />}
            <div className="grid grid-cols-1 sm:grid-cols-3 gap-3">
              <input
                aria-label="Term name"
                className={inputClass}
                placeholder="Winter 2027/28"
                value={termName}
                onChange={(e) => setTermName(e.target.value)}
              />
              <input
                aria-label="Term start date"
                type="date"
                className={inputClass}
                value={termStart}
                onChange={(e) => setTermStart(e.target.value)}
              />
              <input
                aria-label="Term end date"
                type="date"
                className={inputClass}
                value={termEnd}
                onChange={(e) => setTermEnd(e.target.value)}
              />
            </div>
            <div className="flex gap-2 mt-3">
              <button
                type="button"
                onClick={() => void saveTerm()}
                disabled={!termName.trim()}
                className="bg-blue-600 text-white px-3 py-1.5 rounded-lg text-xs font-medium hover:bg-blue-700 disabled:opacity-50 disabled:cursor-not-allowed"
              >
                Save term
              </button>
              <button
                type="button"
                onClick={() => { setAddingTerm(false); setTermError(''); }}
                className="bg-gray-100 text-gray-600 px-3 py-1.5 rounded-lg text-xs font-medium hover:bg-gray-200"
              >
                Cancel
              </button>
            </div>
          </fieldset>
        )}

        <div className="sm:col-span-2 flex gap-2">
          <button
            type="submit"
            className="bg-blue-600 text-white px-4 py-2 rounded-lg text-sm font-medium hover:bg-blue-700"
          >
            {submitLabel}
          </button>
          <button
            type="button"
            onClick={onCancel}
            className="bg-gray-100 text-gray-600 px-4 py-2 rounded-lg text-sm font-medium hover:bg-gray-200"
          >
            Cancel
          </button>
        </div>
      </form>
    </div>
  );
}
