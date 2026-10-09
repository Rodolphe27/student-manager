/** Settings from the environment, read when needed so tests can change them. */
export function config() {
  const env = process.env;
  return {
    port: Number(env.PORT ?? 5030),
    databaseUrl: env.DATABASE_URL ?? 'postgresql://postgres:postgres@localhost:5432/studentmanager',
    allowedOrigins: (env.CORS_ALLOWED_ORIGINS ?? 'http://localhost:5173,http://localhost')
      .split(',')
      .map((s) => s.trim())
      .filter(Boolean),
    defaultPassword: env.DEFAULT_ACCOUNT_PASSWORD ?? 'testuser12',
    anthropicApiKey: env.ANTHROPIC_API_KEY ?? '',
    chatModel: env.CHAT_MODEL ?? 'claude-opus-5-5',
    sessionSecret: env.SESSION_SECRET ?? 'dev-only-secret-change-me',
    production: env.NODE_ENV === 'production',
  };
}
