// "Also create a login account" block for the New Student / New Teacher forms.
// Both fields are optional: an empty username is derived from the email (the part
// before the @), an empty password uses the backend's default password.
export interface AccountFieldValues {
  createAccount?: boolean;
  accountUsername?: string;
  accountPassword?: string;
}

interface AccountFieldsProps {
  values: AccountFieldValues;
  onChange: (patch: AccountFieldValues) => void;
}

const inputClass =
  'w-full border border-gray-200 rounded-lg px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-blue-500';

// Turns the form's account fields into what the API expects: nothing at all when no
// account is requested, and no empty strings (the backend's @Pattern rejects "").
export function accountPayload(values: AccountFieldValues): AccountFieldValues {
  if (!values.createAccount) {
    return { createAccount: false, accountUsername: undefined, accountPassword: undefined };
  }
  return {
    createAccount: true,
    accountUsername: values.accountUsername?.trim() || undefined,
    accountPassword: values.accountPassword || undefined,
  };
}

export default function AccountFields({ values, onChange }: AccountFieldsProps) {
  return (
    <div className="col-span-2 space-y-4">
      <label className="flex items-center gap-2 text-sm text-gray-700">
        <input
          type="checkbox"
          checked={values.createAccount ?? false}
          onChange={(e) => onChange({ createAccount: e.target.checked })}
        />
        Also create a login account
      </label>

      {values.createAccount && (
        <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
          <div>
            <label className="block text-xs font-medium text-gray-500 mb-1">Username</label>
            <input
              className={inputClass}
              placeholder="Optional"
              value={values.accountUsername ?? ''}
              onChange={(e) => onChange({ accountUsername: e.target.value })}
            />
            <p className="text-xs text-gray-400 mt-1">Leave empty to use the part of the email before the @.</p>
          </div>
          <div>
            <label className="block text-xs font-medium text-gray-500 mb-1">Password</label>
            <input
              type="password"
              className={inputClass}
              placeholder="Optional"
              autoComplete="new-password"
              value={values.accountPassword ?? ''}
              onChange={(e) => onChange({ accountPassword: e.target.value })}
            />
            <p className="text-xs text-gray-400 mt-1">
              Leave empty to use the default password. The user can change it on their profile page.
            </p>
          </div>
        </div>
      )}
    </div>
  );
}
