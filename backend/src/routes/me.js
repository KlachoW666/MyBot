import { pool } from '../db/pool.js';
import { config } from '../config.js';

export default async function meRoutes(fastify) {
  /** GET /me → профиль, баланс и статистика для экрана профиля. */
  fastify.get('/me', { preHandler: [fastify.authenticate] }, async (request) => {
    const [{ rows: [user] }, { rows: [stats] }] = await Promise.all([
      pool.query(
        'SELECT telegram_id, username, first_name, balance, created_at FROM users WHERE telegram_id = $1',
        [request.userId],
      ),
      pool.query(
        `SELECT count(*)::int AS opened,
                COALESCE(sum(star_value), 0)::bigint AS won_stars,
                count(*) FILTER (WHERE status = 'withdrawn')::int AS withdrawn
         FROM inventory WHERE user_id = $1`,
        [request.userId],
      ),
    ]);
    return {
      telegram_id: Number(user.telegram_id),
      username: user.username,
      first_name: user.first_name,
      balance: Number(user.balance),
      created_at: user.created_at,
      is_admin: config.adminIds.includes(request.userId),
      stats: {
        opened: stats.opened,
        won_stars: Number(stats.won_stars),
        withdrawn: stats.withdrawn,
      },
    };
  });

  /** GET /me/inventory → выигрыши юзера со статусами. */
  fastify.get('/me/inventory', { preHandler: [fastify.authenticate] }, async (request) => {
    const { rows } = await pool.query(
      `SELECT i.id, i.gift_id, i.star_value, i.status, i.created_at,
              gc.emoji, gc.sticker_file_id, gc.is_available
       FROM inventory i
       JOIN gifts_catalog gc ON gc.gift_id = i.gift_id
       WHERE i.user_id = $1
       ORDER BY i.created_at DESC
       LIMIT 200`,
      [request.userId],
    );
    return { items: rows };
  });
}
