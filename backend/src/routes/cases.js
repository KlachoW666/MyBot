import { pool } from '../db/pool.js';
import { openCase } from '../services/case-opening.js';
import { AppError } from '../lib/errors.js';

export default async function casesRoutes(fastify) {
  /** GET /cases → активные кейсы с содержимым (без весов — шансы не палим). */
  fastify.get('/cases', { preHandler: [fastify.authenticate] }, async () => {
    const { rows } = await pool.query(
      `SELECT c.id, c.slug, c.title, c.price_stars,
              COALESCE(json_agg(json_build_object(
                'gift_id', gc.gift_id,
                'emoji', gc.emoji,
                'sticker_file_id', gc.sticker_file_id,
                'star_count', gc.star_count
              ) ORDER BY gc.star_count) FILTER (WHERE gc.gift_id IS NOT NULL), '[]') AS items
       FROM cases c
       LEFT JOIN case_items ci ON ci.case_id = c.id
       LEFT JOIN gifts_catalog gc ON gc.gift_id = ci.gift_id AND gc.is_available
       WHERE c.is_active
       GROUP BY c.id
       ORDER BY c.price_stars`,
    );
    return { cases: rows };
  });

  /** GET /cases/:id */
  fastify.get('/cases/:id', {
    preHandler: [fastify.authenticate],
    schema: { params: { type: 'object', properties: { id: { type: 'integer' } } } },
  }, async (request) => {
    const { rows: [caseRow] } = await pool.query(
      'SELECT id, slug, title, price_stars FROM cases WHERE id = $1 AND is_active',
      [request.params.id],
    );
    if (!caseRow) throw new AppError(404, 'CASE_NOT_FOUND', 'Case not found');
    const { rows: items } = await pool.query(
      `SELECT gc.gift_id, gc.emoji, gc.sticker_file_id, gc.star_count
       FROM case_items ci JOIN gifts_catalog gc ON gc.gift_id = ci.gift_id
       WHERE ci.case_id = $1 AND gc.is_available
       ORDER BY gc.star_count`,
      [caseRow.id],
    );
    return { ...caseRow, items };
  });

  /**
   * POST /cases/:id/open { idempotencyKey } → { inventoryId, gift, balance }
   * Открытие ТОЛЬКО на сервере: баланс, RNG и запись результата — в транзакции.
   * Клиент по ответу лишь рисует анимацию.
   */
  fastify.post('/cases/:id/open', {
    preHandler: [fastify.authenticate],
    config: { rateLimit: { max: 10, timeWindow: '1 minute' } },
    schema: {
      params: { type: 'object', properties: { id: { type: 'integer' } } },
      body: {
        type: 'object',
        required: ['idempotencyKey'],
        properties: {
          idempotencyKey: { type: 'string', minLength: 8, maxLength: 64 },
        },
      },
    },
  }, async (request) => openCase({
    userId: request.userId,
    caseId: request.params.id,
    idempotencyKey: request.body.idempotencyKey,
  }));
}
