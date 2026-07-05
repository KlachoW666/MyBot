import { pool, withTransaction } from '../db/pool.js';
import { getCatalog } from '../services/catalog.js';
import { AppError } from '../lib/errors.js';
import { config } from '../config.js';

/**
 * /admin/* — только для ID из ADMIN_IDS (по умолчанию 8486449177).
 * Права проверяются на каждый запрос по проверенному userId из JWT.
 */
export default async function adminRoutes(fastify) {
  const guard = { preHandler: [fastify.authenticate, fastify.requireAdmin] };

  /** GET /admin/stats — сводка по всей системе. */
  fastify.get('/admin/stats', guard, async () => {
    const [users, opens, deposits, withdrawals, inventory] = await Promise.all([
      pool.query('SELECT count(*)::int AS n, COALESCE(sum(balance),0)::bigint AS total_balance FROM users'),
      pool.query(`SELECT count(*)::int AS n, COALESCE(-sum(amount),0)::bigint AS stars
                  FROM transactions WHERE type = 'case_open'`),
      pool.query(`SELECT count(*)::int AS n, COALESCE(sum(amount),0)::bigint AS stars
                  FROM transactions WHERE type = 'deposit'`),
      pool.query(`SELECT status, count(*)::int AS n FROM withdrawals GROUP BY status`),
      pool.query(`SELECT status, count(*)::int AS n FROM inventory GROUP BY status`),
    ]);
    return {
      users: users.rows[0].n,
      users_balance: Number(users.rows[0].total_balance),
      opens: opens.rows[0].n,
      opens_stars: Number(opens.rows[0].stars),
      deposits: deposits.rows[0].n,
      deposits_stars: Number(deposits.rows[0].stars),
      withdrawals: Object.fromEntries(withdrawals.rows.map((row) => [row.status, row.n])),
      inventory: Object.fromEntries(inventory.rows.map((row) => [row.status, row.n])),
    };
  });

  /** GET /admin/cases — все кейсы (включая выключенные) с весами. */
  fastify.get('/admin/cases', guard, async () => {
    const { rows } = await pool.query(
      `SELECT c.id, c.slug, c.title, c.price_stars, c.is_active,
              COALESCE(json_agg(json_build_object(
                'gift_id', ci.gift_id, 'weight', ci.weight,
                'emoji', gc.emoji, 'star_count', gc.star_count,
                'is_available', gc.is_available
              ) ORDER BY gc.star_count) FILTER (WHERE ci.id IS NOT NULL), '[]') AS items
       FROM cases c
       LEFT JOIN case_items ci ON ci.case_id = c.id
       LEFT JOIN gifts_catalog gc ON gc.gift_id = ci.gift_id
       GROUP BY c.id ORDER BY c.price_stars`,
    );
    return { cases: rows, max_active: config.maxActiveCases };
  });

  /**
   * POST /admin/cases — создать/обновить кейс целиком (upsert по slug).
   * Активных кейсов не больше 5 — все должны помещаться на один экран.
   */
  fastify.post('/admin/cases', {
    ...guard,
    schema: {
      body: {
        type: 'object',
        required: ['slug', 'title', 'price_stars', 'items'],
        properties: {
          slug: { type: 'string', minLength: 1, maxLength: 32, pattern: '^[a-z0-9-]+$' },
          title: { type: 'string', minLength: 1, maxLength: 64 },
          price_stars: { type: 'integer', minimum: 1, maximum: 100000 },
          is_active: { type: 'boolean', default: true },
          items: {
            type: 'array',
            minItems: 1,
            maxItems: 50,
            items: {
              type: 'object',
              required: ['gift_id', 'weight'],
              properties: {
                gift_id: { type: 'string', minLength: 1 },
                weight: { type: 'integer', minimum: 1, maximum: 1000000 },
              },
            },
          },
        },
      },
    },
  }, async (request) => {
    const { slug, title, price_stars, is_active, items } = request.body;
    return withTransaction(async (client) => {
      if (is_active) {
        const { rows: [{ n }] } = await client.query(
          'SELECT count(*)::int AS n FROM cases WHERE is_active AND slug <> $1', [slug]);
        if (n >= config.maxActiveCases) {
          throw new AppError(409, 'TOO_MANY_CASES',
            `Максимум ${config.maxActiveCases} активных кейсов (один экран)`);
        }
      }
      const { rows: [caseRow] } = await client.query(
        `INSERT INTO cases (slug, title, price_stars, is_active)
         VALUES ($1, $2, $3, $4)
         ON CONFLICT (slug) DO UPDATE SET
           title = EXCLUDED.title, price_stars = EXCLUDED.price_stars,
           is_active = EXCLUDED.is_active
         RETURNING id, slug, title, price_stars, is_active`,
        [slug, title, price_stars, is_active ?? true],
      );
      await client.query('DELETE FROM case_items WHERE case_id = $1', [caseRow.id]);
      for (const item of items) {
        await client.query(
          `INSERT INTO case_items (case_id, gift_id, weight) VALUES ($1, $2, $3)
           ON CONFLICT (case_id, gift_id) DO UPDATE SET weight = EXCLUDED.weight`,
          [caseRow.id, item.gift_id, item.weight],
        );
      }
      return caseRow;
    });
  });

  /** POST /admin/cases/:id/toggle — включить/выключить кейс. */
  fastify.post('/admin/cases/:id/toggle', {
    ...guard,
    schema: { params: { type: 'object', properties: { id: { type: 'integer' } } } },
  }, async (request) => {
    return withTransaction(async (client) => {
      const { rows: [caseRow] } = await client.query(
        'SELECT id, is_active FROM cases WHERE id = $1 FOR UPDATE', [request.params.id]);
      if (!caseRow) throw new AppError(404, 'CASE_NOT_FOUND', 'Case not found');
      if (!caseRow.is_active) {
        const { rows: [{ n }] } = await client.query(
          'SELECT count(*)::int AS n FROM cases WHERE is_active');
        if (n >= config.maxActiveCases) {
          throw new AppError(409, 'TOO_MANY_CASES',
            `Максимум ${config.maxActiveCases} активных кейсов`);
        }
      }
      const { rows: [updated] } = await client.query(
        'UPDATE cases SET is_active = NOT is_active WHERE id = $1 RETURNING id, is_active',
        [caseRow.id]);
      return updated;
    });
  });

  /** POST /admin/catalog/refresh — принудительно перечитать getAvailableGifts. */
  fastify.post('/admin/catalog/refresh', guard, async () => {
    const gifts = await getCatalog({ force: true });
    return { refreshed: gifts.length };
  });

  /**
   * GET /admin/catalog — полный каталог для конструктора кейсов:
   * цены, лимитированность (total/remaining), апгрейд, доступность.
   */
  fastify.get('/admin/catalog', guard, async () => {
    await getCatalog().catch(() => {}); // освежить, если кэш протух
    const { rows } = await pool.query(
      `SELECT gift_id, emoji, star_count, upgrade_star_count,
              total_count, remaining_count, is_available, updated_at
       FROM gifts_catalog ORDER BY star_count, gift_id`,
    );
    return { gifts: rows };
  });

  /** GET /admin/withdrawals — последние выводы с результатами Bot API. */
  fastify.get('/admin/withdrawals', guard, async () => {
    const { rows } = await pool.query(
      `SELECT w.id, w.inventory_id, w.user_id, w.gift_id, w.status, w.attempts,
              w.tg_result, w.created_at, gc.emoji
       FROM withdrawals w
       LEFT JOIN gifts_catalog gc ON gc.gift_id = w.gift_id
       ORDER BY w.created_at DESC LIMIT 100`,
    );
    return { withdrawals: rows };
  });

  /**
   * POST /admin/balance — ручная корректировка баланса (например, начислить
   * себе Stars для теста). Пишется в transactions type=adjustment.
   */
  fastify.post('/admin/balance', {
    ...guard,
    schema: {
      body: {
        type: 'object',
        required: ['telegram_id', 'amount'],
        properties: {
          telegram_id: { type: 'integer' },
          amount: { type: 'integer', minimum: -1000000, maximum: 1000000 },
        },
      },
    },
  }, async (request) => {
    const { telegram_id, amount } = request.body;
    return withTransaction(async (client) => {
      const { rows: [user] } = await client.query(
        `INSERT INTO users (telegram_id, balance) VALUES ($1, 0)
         ON CONFLICT (telegram_id) DO UPDATE SET updated_at = now()
         RETURNING balance`,
        [telegram_id],
      );
      if (Number(user.balance) + amount < 0) {
        throw new AppError(409, 'NEGATIVE_BALANCE', 'Баланс не может стать отрицательным');
      }
      const { rows: [updated] } = await client.query(
        'UPDATE users SET balance = balance + $1, updated_at = now() WHERE telegram_id = $2 RETURNING balance',
        [amount, telegram_id],
      );
      await client.query(
        `INSERT INTO transactions (user_id, type, amount, ref)
         VALUES ($1, 'adjustment', $2, $3)`,
        [telegram_id, amount, `admin:${request.userId}`],
      );
      return { telegram_id, balance: Number(updated.balance) };
    });
  });
}
