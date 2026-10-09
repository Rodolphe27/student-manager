// Starts a throw-away Postgres for the tests and keeps it running:  node scripts/test-db.mjs
// Then run the tests with  TEST_DATABASE_URL=postgresql://postgres:postgres@localhost:54329/postgres npm test
// (Without it the tests start their own Postgres — correct, but slower.)
import EmbeddedPostgres from 'embedded-postgres';
import { mkdtempSync } from 'node:fs';
import { tmpdir } from 'node:os';
import { join } from 'node:path';

const port = Number(process.env.TEST_DB_PORT ?? 54329);
const dir = mkdtempSync(join(tmpdir(), 'nest-testdb-'));
const server = new EmbeddedPostgres({
  databaseDir: join(dir, 'data'), user: 'postgres', password: 'postgres', port, persistent: false,
  onLog: () => {}, onError: () => {},
});
await server.initialise();
await server.start();
console.log(`test database ready on port ${port}`);
const stop = async () => { await server.stop(); process.exit(0); };
process.on('SIGINT', stop);
process.on('SIGTERM', stop);
setInterval(() => {}, 1 << 30);
