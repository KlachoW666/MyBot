import Fastify from 'fastify';
import cors from '@fastify/cors';
import rateLimit from '@fastify/rate-limit';
import { redis } from './redis.js';
import { errorHandler } from './lib/errors.js';
import { createBot } from './bot.js';
import authPlugin from './plugins/auth.js';
import authRoutes from './routes/auth.js';
import catalogRoutes from './routes/catalog.js';
import casesRoutes from './routes/cases.js';
import payRoutes from './routes/pay.js';
import withdrawRoutes from './routes/withdraw.js';
import meRoutes from './routes/me.js';
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

  const botInfo = process.env.BOT_INFO ? JSON.parse(process.env.BOT_INFO) : undefined;
  const bot = createBot(fastify.log, { botInfo });
  if (!bot.isInited()) {
    // getMe с фейл-фастом: лучше упасть с внятной ошибкой, чем висеть на старте.
    await Promise.race([
      bot.init(),
      new Promise((_, reject) => setTimeout(
        () => reject(new Error('bot.init() timed out — Telegram API unreachable?')), 15_000)),
    ]);
  }

  fastify.get('/health', { config: { rateLimit: false } }, () => ({ ok: true }));

  await fastify.register(authRoutes);
  await fastify.register(catalogRoutes);
  await fastify.register(casesRoutes);
  await fastify.register(payRoutes);
  await fastify.register(withdrawRoutes);
  await fastify.register(meRoutes);
  await fastify.register(webhookRoutes, { bot });

  return fastify;
}
