import crypto from 'node:crypto';
import { config } from '../config.js';
import { handleUpdate } from '../bot.js';

/**
 * POST /bot/webhook — приём апдейтов Telegram.
 * Двойная защита: HTTPS-домен (setWebhook пускает только https) и сверка
 * X-Telegram-Bot-Api-Secret-Token с WEBHOOK_SECRET (timing-safe).
 * Отвечаем 200 сразу после обработки; ошибки логируем, но не отдаём
 * Telegram'у 5xx без нужды — иначе он зациклит доставку.
 */
export default async function webhookRoutes(fastify) {
  fastify.post('/bot/webhook', { config: { rateLimit: false } }, async (request, reply) => {
    const header = String(request.headers['x-telegram-bot-api-secret-token'] ?? '');
    const expected = config.webhookSecret;
    const valid = header.length === expected.length &&
      crypto.timingSafeEqual(Buffer.from(header), Buffer.from(expected));
    if (!valid) {
      return reply.status(401).send({ error: 'UNAUTHORIZED' });
    }

    try {
      await handleUpdate(request.body ?? {}, request.log);
    } catch (error) {
      request.log.error({ err: error }, 'webhook update failed');
    }
    return { ok: true };
  });
}
