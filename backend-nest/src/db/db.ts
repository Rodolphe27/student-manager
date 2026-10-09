import { AsyncLocalStorage } from 'node:async_hooks';
import { Pool, types } from 'pg';

// bigint → number (ids, counts, versions are far below 2^53); date → 'YYYY-MM-DD' string, not a JS Date.
types.setTypeParser(20, (v) => Number(v));
types.setTypeParser(1082, (v) => v);

// eslint-disable-next-line @typescript-eslint/no-explicit-any
export type Row = Record<string, any>;

export interface Queryable {
  query(sql: string, params?: unknown[]): Promise<{ rows: Row[]; rowCount: number }>;
}

export interface Db extends Queryable {
  tx<T>(fn: (q: Queryable) => Promise<T>): Promise<T>;
  close(): Promise<void>;
}

export const DB = Symbol('DB');

/** Who is acting — written to created_by / updated_by like Spring's AuditorAware. */
export const requestContext = new AsyncLocalStorage<{ actor: string | null }>();
export const currentActor = (): string | null => requestContext.getStore()?.actor ?? null;

/** Timestamps in UTC, like a JVM running on Render. */
export const NOW = "(now() at time zone 'utc')";

export class PgDb implements Db {
  readonly pool: Pool;

  constructor(connectionString: string) {
    const needsSsl = /sslmode=require/.test(connectionString) || /supabase\.(co|com)/.test(connectionString);
    this.pool = new Pool({
      connectionString: connectionString.replace(/[?&]sslmode=require/, ''),
      ssl: needsSsl ? { rejectUnauthorized: false } : undefined,
      max: 5,
    });
  }

  async query(sql: string, params: unknown[] = []) {
    const result = await this.pool.query(sql, params as unknown[]);
    return { rows: result.rows, rowCount: result.rowCount ?? 0 };
  }

  async tx<T>(fn: (q: Queryable) => Promise<T>): Promise<T> {
    const client = await this.pool.connect();
    try {
      await client.query('BEGIN');
      const result = await fn({
        query: async (sql, params = []) => {
          const r = await client.query(sql, params as unknown[]);
          return { rows: r.rows, rowCount: r.rowCount ?? 0 };
        },
      });
      await client.query('COMMIT');
      return result;
    } catch (error) {
      await client.query('ROLLBACK').catch(() => undefined);
      throw error;
    } finally {
      client.release();
    }
  }

  async close() {
    await this.pool.end();
  }
}
