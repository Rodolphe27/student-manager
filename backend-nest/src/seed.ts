import 'reflect-metadata';
import { config } from './config';
import { PgDb } from './db/db';
import { applySchemaIfEmpty, seedDemoData } from './db/bootstrap';

/** npm run seed — creates the tables (if the database is empty) and fills it with sample data. */
async function main() {
  const cfg = config();
  const db = new PgDb(cfg.databaseUrl);
  try {
    await applySchemaIfEmpty(db);
    await seedDemoData(db, cfg.defaultPassword);
  } finally {
    await db.close();
  }
}

main().catch((error) => {
  // eslint-disable-next-line no-console
  console.error(error);
  process.exit(1);
});
