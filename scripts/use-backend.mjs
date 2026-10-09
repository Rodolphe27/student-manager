#!/usr/bin/env node
// Switches which backend the deployed frontend talks to.
//
//   node scripts/use-backend.mjs status     which backend is active now
//   node scripts/use-backend.mjs spring     point Vercel's /api rewrite at the Spring Boot backend
//   node scripts/use-backend.mjs nest       point it at the NestJS backend
//
// It edits the /api rewrite in frontend/vercel.json (addresses live in backends.json). Commit and push
// afterwards: Vercel redeploys and the app uses the other backend. Both backends serve the same API and
// the same database; only the login session is separate, so users have to sign in again after a switch.
import { readFileSync, writeFileSync } from 'node:fs';
import { dirname, join } from 'node:path';
import { fileURLToPath } from 'node:url';

const root = join(dirname(fileURLToPath(import.meta.url)), '..');
const backends = JSON.parse(readFileSync(join(root, 'backends.json'), 'utf8'));
const vercelPath = join(root, 'frontend', 'vercel.json');
const vercel = JSON.parse(readFileSync(vercelPath, 'utf8'));
const rule = vercel.rewrites.find((r) => r.source === '/api/:path*');
if (!rule) {
  console.error('frontend/vercel.json has no /api/:path* rewrite');
  process.exit(1);
}

const active = () => Object.entries(backends).find(([, url]) => rule.destination.startsWith(url))?.[0] ?? 'unknown';
const choice = process.argv[2];

if (!choice || choice === 'status') {
  console.log(`active backend: ${active()}  (${rule.destination})`);
} else if (backends[choice]) {
  rule.destination = `${backends[choice]}/api/:path*`;
  writeFileSync(vercelPath, JSON.stringify(vercel, null, 2) + '\n');
  console.log(`frontend/vercel.json now points /api at the ${choice} backend: ${rule.destination}`);
  console.log('Commit and push to deploy the switch.');
} else {
  console.error(`unknown backend "${choice}" — use: status | ${Object.keys(backends).join(' | ')}`);
  process.exit(1);
}
