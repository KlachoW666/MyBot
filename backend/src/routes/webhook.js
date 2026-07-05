import { webhookCallback } from 'grammy';
import { config } from '../config.js';

/**
 * POST /bot/webhook — приём апдейтов Telegram.
 * Двойная защита: HTTPS-домен (setWebhook пускает только https) и сверка
 * заголовка X-Telegram-Bot-Api-Secret-Token с нашим WEBHOOK_SECRET —
 * grammY отвергает запросы с неверным секретом сам (secretToken).
 */
export default async function webhookRoutes(fastify, { bot }) {
  const handler = webhookCallback(bot, 'fastify', { secretToken: config.webhookSecret });
  fastify.post('/bot/webhook', {
    config: { rateLimit: false },
  }, handler);
}
