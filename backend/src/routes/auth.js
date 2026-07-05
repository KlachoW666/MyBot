import { validateInitData } from '../lib/validate-init-data.js';
import { pool } from '../db/pool.js';
import { config } from '../config.js';

/**
 * POST /auth { initData } → { token, user }
 * Единственная точка входа: проверяем подпись Telegram и возраст auth_date,
 * upsert'им юзера по telegram_id, выдаём JWT. Ничему из тела запроса,
 * кроме проверенной подписи, не доверяем.
 */
export default async function authRoutes(fastify) {
  fastify.post('/auth', {
    config: { rateLimit: { max: 20, timeWindow: '1 minute' } },
    schema: {
      body: {
        type: 'object',
        required: ['initData'],
        properties: { initData: { type: 'string', maxLength: 4096 } },
      },
    },
  }, async (request) => {
    const { user } = validateInitData(
      request.body.initData, config.botToken, config.initDataMaxAge);

    const { rows: [dbUser] } = await pool.query(
      `INSERT INTO users (telegram_id, username, first_name)
       VALUES ($1, $2, $3)
       ON CONFLICT (telegram_id) DO UPDATE SET
         username = EXCLUDED.username,
         first_name = EXCLUDED.first_name,
         updated_at = now()
       RETURNING telegram_id, username, first_name, balance`,
      [user.id, user.username ?? null, user.first_name ?? null],
    );

    const token = fastify.jwt.sign({ sub: String(user.id) });
    return {
      token,
      user: {
        telegram_id: Number(dbUser.telegram_id),
        username: dbUser.username,
        first_name: dbUser.first_name,
        balance: Number(dbUser.balance),
      },
    };
  });
}
