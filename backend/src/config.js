const required = (name) => {
  const value = process.env[name];
  if (!value) throw new Error(`Missing required env var: ${name}`);
  return value;
};

export const config = {
  port: Number(process.env.PORT ?? 8080),
  host: process.env.HOST ?? '0.0.0.0',

  botToken: required('BOT_TOKEN'),
  webhookSecret: required('WEBHOOK_SECRET'),
  publicUrl: process.env.PUBLIC_URL ?? '',

  jwtSecret: required('JWT_SECRET'),
  sessionTtl: Number(process.env.SESSION_TTL ?? 86_400),
  initDataMaxAge: Number(process.env.INIT_DATA_MAX_AGE ?? 3_600),

  databaseUrl: required('DATABASE_URL'),
  redisUrl: process.env.REDIS_URL ?? 'redis://localhost:6379',

  catalogTtl: Number(process.env.CATALOG_TTL ?? 600),
};
