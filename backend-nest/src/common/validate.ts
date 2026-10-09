import { ApiError, malformedBody, validationFailed } from './errors';

export type Rule =
  | { kind: 'string'; required?: string; pattern?: [RegExp, string]; email?: string; max?: [number, string] }
  | { kind: 'long'; required?: string }
  | { kind: 'int'; min?: [number, string]; max?: [number, string] }
  | { kind: 'boolean' }
  | { kind: 'date' }
  | { kind: 'enum'; values: readonly string[]; required?: string };

export type Schema = Record<string, Rule>;

const EMAIL = /^[^\s@]+@[^\s@]+$/;
const ISO_DATE = /^\d{4}-\d{2}-\d{2}$/;

/**
 * Checks a JSON body against a schema and returns a copy that holds only the schema's fields.
 * Wrong JSON types → 400 "Malformed request body" (Jackson); broken constraints → 400 "Validation failed"
 * with {field: message} (Bean Validation). Missing optional fields come back as null (ints as 0).
 */
export function parseBody<T>(body: unknown, schema: Schema): T {
  if (body === null || typeof body !== 'object' || Array.isArray(body)) throw malformedBody();
  const input = body as Record<string, unknown>;
  const out: Record<string, unknown> = {};
  const errors: Record<string, string> = {};

  for (const [field, rule] of Object.entries(schema)) {
    const raw = input[field];
    const value = raw === undefined ? null : raw;
    switch (rule.kind) {
      case 'string': {
        if (value !== null && typeof value !== 'string') throw malformedBody();
        const text = value as string | null;
        if (rule.required && (text === null || text.trim() === '')) {
          errors[field] = rule.required;
        } else if (text !== null) {
          if (rule.max && text.length > rule.max[0]) errors[field] = rule.max[1];
          else if (rule.email && !EMAIL.test(text)) errors[field] = rule.email;
          else if (rule.pattern && !rule.pattern[0].test(text)) errors[field] = rule.pattern[1];
        }
        out[field] = text;
        break;
      }
      case 'long': {
        if (value !== null && !(typeof value === 'number' && Number.isInteger(value))) throw malformedBody();
        if (value === null && rule.required) errors[field] = rule.required;
        out[field] = value;
        break;
      }
      case 'int': {
        if (value !== null && !(typeof value === 'number' && Number.isInteger(value))) throw malformedBody();
        const n = (value as number | null) ?? 0;
        if (rule.min && n < rule.min[0]) errors[field] = rule.min[1];
        else if (rule.max && n > rule.max[0]) errors[field] = rule.max[1];
        out[field] = n;
        break;
      }
      case 'boolean': {
        if (value !== null && typeof value !== 'boolean') throw malformedBody();
        out[field] = value ?? false;
        break;
      }
      case 'date': {
        if (value !== null && !(typeof value === 'string' && ISO_DATE.test(value) && !Number.isNaN(Date.parse(value)))) {
          throw malformedBody();
        }
        out[field] = value;
        break;
      }
      case 'enum': {
        if (value !== null && (typeof value !== 'string' || !rule.values.includes(value))) throw malformedBody();
        if (value === null && rule.required) errors[field] = rule.required;
        out[field] = value;
        break;
      }
    }
  }
  if (Object.keys(errors).length > 0) throw validationFailed(errors);
  return out as T;
}

/** Path variable → positive-or-zero integer, or 400 "Invalid value for parameter 'name'". */
export function pathId(value: string, name = 'id'): number {
  if (!/^-?\d+$/.test(value)) throw new ApiError(400, `Invalid value for parameter '${name}'`);
  return Number(value);
}

/** Query parameter holding one of an enum's values (absent/blank → null). */
export function enumParam<T extends string>(value: unknown, values: readonly T[], name: string): T | null {
  if (value === undefined || value === null || value === '') return null;
  if (typeof value !== 'string' || !values.includes(value as T)) {
    throw new ApiError(400, `Invalid value for parameter '${name}'`);
  }
  return value as T;
}

export function longParam(value: unknown, name: string): number | null {
  if (value === undefined || value === null || value === '') return null;
  if (typeof value !== 'string' || !/^-?\d+$/.test(value)) {
    throw new ApiError(400, `Invalid value for parameter '${name}'`);
  }
  return Number(value);
}

export const USERNAME = [/^[a-zA-Z0-9_.-]{3,32}$/, "Username must be 3-32 characters: letters, digits, '.', '_' or '-'"] as [RegExp, string];
export const PASSWORD = [/^(?=.*[A-Za-z])(?=.*\d).{8,}$/, 'Password must be at least 8 characters and include a letter and a digit'] as [RegExp, string];
export const DEPARTMENT = [/^[A-Za-z .'-]{2,100}$/, "Department must be 2-100 characters: letters, spaces, '.', ''' or '-'"] as [RegExp, string];
