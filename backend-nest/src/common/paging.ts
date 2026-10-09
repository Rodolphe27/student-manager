import { ApiError } from './errors';

export interface PageOf<T> {
  content: T[];
  page: { size: number; number: number; totalElements: number; totalPages: number };
}

export interface Sorting {
  /** allowed ?sort= property → SQL expression */
  columns: Record<string, string>;
  /** e.g. [['last_name','asc'],['first_name','asc']] — used when ?sort= is absent */
  defaults: Array<[string, 'asc' | 'desc']>;
}

export interface PageRequest {
  page: number;
  size: number;
  orderBy: string;
}

const DEFAULT_SIZE = 10;
const MAX_SIZE = 100;

/** Spring Data's Pageable: ?page=0&size=10&sort=lastName,asc (repeatable). */
export function pageRequest(query: Record<string, unknown>, sorting: Sorting): PageRequest {
  let page = Number.parseInt(String(query.page ?? ''), 10);
  if (!Number.isFinite(page) || page < 0) page = 0;
  let size = Number.parseInt(String(query.size ?? ''), 10);
  if (!Number.isFinite(size) || size < 1) size = DEFAULT_SIZE;
  size = Math.min(size, MAX_SIZE);

  const raw = query.sort;
  const parts = (Array.isArray(raw) ? raw : raw === undefined ? [] : [raw]).map(String).filter((s) => s.trim() !== '');
  let orders: Array<[string, 'asc' | 'desc']>;
  if (parts.length === 0) {
    orders = sorting.defaults;
  } else {
    orders = parts.map((part) => {
      const [prop, dir] = part.split(',').map((s) => s.trim());
      const column = sorting.columns[prop];
      if (!column) throw new ApiError(400, `Invalid sort property '${prop}'`);
      return [column, dir?.toLowerCase() === 'desc' ? 'desc' : 'asc'];
    });
  }
  return { page, size, orderBy: orders.map(([c, d]) => `${c} ${d}`).join(', ') };
}

export function toPage<T>(content: T[], total: number, req: PageRequest): PageOf<T> {
  return {
    content,
    page: { size: req.size, number: req.page, totalElements: total, totalPages: Math.ceil(total / req.size) },
  };
}
