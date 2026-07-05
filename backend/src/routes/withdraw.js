import { withdrawItem } from '../services/withdrawals.js';

/**
 * POST /withdraw { inventoryId } → { status: 'withdrawn' | 'refunded' }
 * Бот шлёт подарок sendGift'ом напрямую на telegram_id из JWT-сессии.
 * Жёсткий rate-limit + Redis-лок + статусная машина в БД (см. сервис).
 */
export default async function withdrawRoutes(fastify) {
  fastify.post('/withdraw', {
    preHandler: [fastify.authenticate],
    config: { rateLimit: { max: 5, timeWindow: '1 minute' } },
    schema: {
      body: {
        type: 'object',
        required: ['inventoryId'],
        properties: { inventoryId: { type: 'integer', minimum: 1 } },
      },
    },
  }, async (request) => withdrawItem({
    userId: request.userId,
    inventoryId: request.body.inventoryId,
    log: request.log,
  }));
}
