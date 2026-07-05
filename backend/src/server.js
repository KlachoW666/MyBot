import { existsSync } from 'node:fs';
import { fileURLToPath } from 'node:url';
import Fastify from 'fastify';
import cors from '@fastify/cors';
import rateLimit from '@fastify/rate-limit';
import fastifyStatic from '@fastify/static';
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
import giftsRoutes from './routes/gifts.js';
import upgradeRoutes from './routes/upgrade.js';
import webhookRoutes from './routes/webhook.js';

const FRONTEND_DIST = process.env.FRONTEND_DIST
  ?? fileURLToPath(new URL('../../frontend/dist', import.meta.url));

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

  // Всё API — под /api: фронт и API живут на одном домене (один туннель/прокси).
  await fastify.register(async (api) => {
    api.get('/health', { config: { rateLimit: false } }, () => ({ ok: true }));
    await api.register(authRoutes);
    await api.register(catalogRoutes);
    await api.register(casesRoutes);
    await api.register(payRoutes);
    await api.register(withdrawRoutes);
    await api.register(meRoutes);
    await api.register(adminRoutes);
    await api.register(giftsRoutes);
    await api.register(upgradeRoutes);
    await api.register(webhookRoutes);
  }, { prefix: '/api' });

  // Прод/тест: раздаём собранный фронт (vite build) прямо из backend'а.
  if (existsSync(FRONTEND_DIST)) {
    await fastify.register(fastifyStatic, { root: FRONTEND_DIST, index: 'index.html' });
    fastify.log.info({ dir: FRONTEND_DIST }, 'serving frontend build');
  }

  return fastify;
}
