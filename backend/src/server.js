import Fastify from 'fastify';
import cors from '@fastify/cors';
import rateLimit from '@fastify/rate-limit';
import { redis } from './redis.js';
import { errorHandler } from './lib/errors.js';
import authPlugin from './plugins/auth.js';
import authRoutes from './routes/auth.js';
import catalogRoutes from './routes/catalog.js';
import casesRoutes from './routes/cases.js';
import payRoutes from './routes/pay.js';
import withdrawRoutes from './routes/withdraw.js';
import meRoutes from './routes/me.js';
import adminRoutes from './routes/admin.js';
import webhookRoutes from './routes/webhook.js';

export async function buildServer() {
  const fastify = Fastify({
    logger: { level: process.env.LOG_LEVEL ?? 'info' },
    trustProxy: true,
  });

  await fastify.register(cors, { origin: true });

  // Rate-limit в Redis: по userId для авторизованных, иначе по IP.
  await fastify.register(rateLimit, {
    global: true,
    max: 120,
    timeWindow: '1 minute',
    redis,
    nameSpace: 'rl:',
    keyGenerator: (request) => (request.userId ? `u:${request.userId}` : `ip:${request.ip}`),
  });

  await fastify.register(authPlugin);
  fastify.setErrorHandler(errorHandler);

  fastify.get('/health', { config: { rateLimit: false } }, () => ({ ok: true }));

  await fastify.register(authRoutes);
  await fastify.register(catalogRoutes);
  await fastify.register(casesRoutes);
  await fastify.register(payRoutes);
  await fastify.register(withdrawRoutes);
  await fastify.register(meRoutes);
  await fastify.register(adminRoutes);
  await fastify.register(webhookRoutes);

  return fastify;
}
