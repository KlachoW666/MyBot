import fp from 'fastify-plugin';
import fastifyJwt from '@fastify/jwt';
import { config } from '../config.js';

/**
 * JWT-сессии. Токен выдаётся только после валидации initData (см. routes/auth.js);
 * в payload — исключительно проверенный telegram_id (`sub`).
 * request.userId — единственный источник идентичности во всех роутах.
 */
export default fp(async (fastify) => {
  await fastify.register(fastifyJwt, {
    secret: config.jwtSecret,
    sign: { expiresIn: config.sessionTtl },
  });

  fastify.decorateRequest('userId', null);

  fastify.decorate('authenticate', async (request, reply) => {
    try {
      await request.jwtVerify();
      request.userId = Number(request.user.sub);
    } catch {
      return reply.status(401).send({ error: 'UNAUTHORIZED', message: 'Invalid or expired token' });
    }
  });

  // Админ-доступ проверяем по конфигу на каждый запрос (не в JWT):
  // удаление ID из ADMIN_IDS отзывает права сразу, без ожидания истечения токена.
  fastify.decorate('requireAdmin', async (request, reply) => {
    if (!config.adminIds.includes(request.userId)) {
      return reply.status(403).send({ error: 'FORBIDDEN', message: 'Admin only' });
    }
  });
});
