import { useEffect, useState, type FormEvent } from 'react';
import { useAuth } from '../context/useAuth';
import profileService from '../services/profileService';
import { getErrorMessage } from '../services/errorMessage';
import type { Profile, UpdateProfileRequest, ChangePasswordRequest } from '../types';

const inputClass =
  'w-full border border-gray-200 rounded-lg px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-blue-500';
const labelClass = 'block text-sm font-medium text-gray-700 mb-1';

export default function ProfilePage() {
  const { updateUser } = useAuth();

  const [profile, setProfile] = useState<Profile | null>(null);
  const [form, setForm]       = useState<UpdateProfileRequest>({ username: '', email: '' });
  const [loadError, setLoadError] = useState<string>('');
  const [saveMsg, setSaveMsg]     = useState<{ ok: boolean; text: string } | null>(null);
  const [saving, setSaving]       = useState<boolean>(false);

  const [pw, setPw]           = useState<ChangePasswordRequest>({ currentPassword: '', newPassword: '' });
  const [pwMsg, setPwMsg]     = useState<{ ok: boolean; text: string } | null>(null);
  const [pwSaving, setPwSaving] = useState<boolean>(false);

  const hasProfile = profile !== null && profile.firstName !== null;
  const isStudent  = profile?.role === 'STUDENT';
  const isTeacher  = profile?.role === 'TEACHER';

  const applyProfile = (p: Profile): void => {
    setProfile(p);
    setForm({
      username: p.username,
      email: p.email,
      firstName: p.firstName ?? undefined,
      lastName: p.lastName ?? undefined,
      birthDate: p.birthDate ?? undefined,
      department: p.department ?? undefined,
    });
  };

  useEffect(() => {
    let cancelled = false;
    profileService.get()
      .then((r) => { if (!cancelled) applyProfile(r.data); })
      .catch((err: unknown) => { if (!cancelled) setLoadError(getErrorMessage(err, 'Could not load your profile')); });
    return () => { cancelled = true; };
  }, []);

  const handleSave = async (e: FormEvent<HTMLFormElement>): Promise<void> => {
    e.preventDefault();
    setSaveMsg(null);
    setSaving(true);
    try {
      const r = await profileService.update({
        ...form,
        // An emptied optional field is sent as absent, not as an empty string.
        birthDate: form.birthDate || undefined,
        department: form.department || undefined,
      });
      applyProfile(r.data);
      // Keep the sidebar (username/role) in sync with the saved account.
      updateUser({ username: r.data.username, email: r.data.email, role: r.data.role });
      setSaveMsg({ ok: true, text: 'Profile saved' });
    } catch (err: unknown) {
      setSaveMsg({ ok: false, text: getErrorMessage(err, 'Could not save your profile') });
    } finally {
      setSaving(false);
    }
  };

  const handlePassword = async (e: FormEvent<HTMLFormElement>): Promise<void> => {
    e.preventDefault();
    setPwMsg(null);
    setPwSaving(true);
    try {
      await profileService.changePassword(pw);
      setPw({ currentPassword: '', newPassword: '' });
      setPwMsg({ ok: true, text: 'Password changed' });
    } catch (err: unknown) {
      setPwMsg({ ok: false, text: getErrorMessage(err, 'Could not change your password') });
    } finally {
      setPwSaving(false);
    }
  };

  const banner = (m: { ok: boolean; text: string } | null) =>
    m && (
      <div
        className={`text-sm px-4 py-3 rounded-lg mb-4 border ${
          m.ok ? 'bg-green-50 border-green-100 text-green-700' : 'bg-red-50 border-red-100 text-red-600'
        }`}
      >
        {m.text}
      </div>
    );

  if (loadError) {
    return <div className="bg-red-50 border border-red-100 text-red-600 text-sm px-4 py-3 rounded-lg">{loadError}</div>;
  }
  if (!profile) {
    return <p className="text-sm text-gray-400">Loading…</p>;
  }

  return (
    <div className="max-w-xl space-y-8">
      <div>
        <h1 className="text-2xl font-bold text-gray-800">My Profile</h1>
        <p className="text-sm text-gray-400">Your account and personal details ({profile.role})</p>
      </div>

      {/* Account + personal details */}
      <form onSubmit={handleSave} className="bg-white rounded-2xl border border-gray-100 p-6 space-y-4">
        {banner(saveMsg)}

        <div>
          <label className={labelClass}>Username</label>
          <input
            type="text"
            className={inputClass}
            value={form.username}
            onChange={(e) => setForm({ ...form, username: e.target.value })}
            required
          />
        </div>

        <div>
          <label className={labelClass}>Email</label>
          <input
            type="email"
            className={inputClass}
            value={form.email}
            onChange={(e) => setForm({ ...form, email: e.target.value })}
            required
          />
        </div>

        {hasProfile && (
          <>
            <div className="grid grid-cols-2 gap-4">
              <div>
                <label className={labelClass}>First name</label>
                <input
                  type="text"
                  className={inputClass}
                  value={form.firstName ?? ''}
                  onChange={(e) => setForm({ ...form, firstName: e.target.value })}
                  required
                />
              </div>
              <div>
                <label className={labelClass}>Last name</label>
                <input
                  type="text"
                  className={inputClass}
                  value={form.lastName ?? ''}
                  onChange={(e) => setForm({ ...form, lastName: e.target.value })}
                  required
                />
              </div>
            </div>

            {isStudent && (
              <>
                <div>
                  <label className={labelClass}>Birth date</label>
                  <input
                    type="date"
                    className={inputClass}
                    value={form.birthDate ?? ''}
                    onChange={(e) => setForm({ ...form, birthDate: e.target.value })}
                  />
                </div>
                <div>
                  <label className={labelClass}>Matriculation number</label>
                  <input type="text" className={`${inputClass} bg-gray-50 text-gray-500`} value={profile.matriculationNumber ?? ''} disabled />
                  <p className="text-xs text-gray-400 mt-1">Only an administrator can change this.</p>
                </div>
              </>
            )}

            {isTeacher && (
              <div>
                <label className={labelClass}>Department</label>
                <input
                  type="text"
                  className={inputClass}
                  value={form.department ?? ''}
                  onChange={(e) => setForm({ ...form, department: e.target.value })}
                />
              </div>
            )}
          </>
        )}

        <button
          type="submit"
          disabled={saving}
          className="bg-blue-600 text-white px-4 py-2 rounded-lg text-sm font-medium hover:bg-blue-700 disabled:opacity-50 disabled:cursor-not-allowed transition-colors"
        >
          {saving ? 'Saving…' : 'Save changes'}
        </button>
      </form>

      {/* Password */}
      <form onSubmit={handlePassword} className="bg-white rounded-2xl border border-gray-100 p-6 space-y-4">
        <h2 className="text-lg font-semibold text-gray-800">Change password</h2>
        {banner(pwMsg)}

        <div>
          <label className={labelClass}>Current password</label>
          <input
            type="password"
            className={inputClass}
            value={pw.currentPassword}
            onChange={(e) => setPw({ ...pw, currentPassword: e.target.value })}
            autoComplete="current-password"
            required
          />
        </div>
        <div>
          <label className={labelClass}>New password</label>
          <input
            type="password"
            className={inputClass}
            value={pw.newPassword}
            onChange={(e) => setPw({ ...pw, newPassword: e.target.value })}
            autoComplete="new-password"
            required
          />
          <p className="text-xs text-gray-400 mt-1">At least 8 characters, with a letter and a digit.</p>
        </div>

        <button
          type="submit"
          disabled={pwSaving}
          className="bg-blue-600 text-white px-4 py-2 rounded-lg text-sm font-medium hover:bg-blue-700 disabled:opacity-50 disabled:cursor-not-allowed transition-colors"
        >
          {pwSaving ? 'Changing…' : 'Change password'}
        </button>
      </form>
    </div>
  );
}
