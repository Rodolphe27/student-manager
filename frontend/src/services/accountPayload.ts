// Account fields shared by the New Student / New Teacher forms (see AccountFields).
export interface AccountFieldValues {
  createAccount?: boolean;
  accountUsername?: string;
  accountPassword?: string;
}

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
