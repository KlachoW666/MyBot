import { pool } from '../db/pool.js';
import { redis } from '../redis.js';
import { downloadFile } from '../lib/telegram-api.js';

const CACHE_TTL = 86_400; // сутки: артворк подарка не меняется

const CONTENT_TYPES = {
  webp: 'image/webp', png: 'image/png', jpg: 'image/jpeg', jpeg: 'image/jpeg',
  svg: 'image/svg+xml', webm: 'video/webm',
};

/**
 * GET /gifts/:giftId/image — реальная картинка подарка из Telegram.
 * Берём thumbnail стикера (статичный webp) — это настоящий артворк подарка;
 * сам стикер обычно анимированный .tgs, который <img> не покажет.
 * Скачивается через getFile один раз и кэшируется в Redis.
 * Эндпоинт публичный (нужен для <img src>), gift_id не секрет.
 */
export default async function giftsRoutes(fastify) {
  fastify.get('/gifts/:giftId/image', {
    config: { rateLimit: { max: 300, timeWindow: '1 minute' } },
    schema: { params: { type: 'object', properties: { giftId: { type: 'string', maxLength: 64 } } } },
  }, async (request, reply) => {
    const { giftId } = request.params;
    const cacheKey = `gift:img:${giftId}`;

    const [cachedType, cachedBody] = await Promise.all([
      redis.get(`${cacheKey}:type`).catch(() => null),
      redis.getBuffer(cacheKey).catch(() => null),
    ]);
    if (cachedType && cachedBody?.length) {
      return reply.header('cache-control', 'public, max-age=86400').type(cachedType).send(cachedBody);
    }

    const { rows: [gift] } = await pool.query(
      'SELECT snapshot FROM gifts_catalog WHERE gift_id = $1', [giftId]);
    const sticker = gift?.snapshot?.sticker;
    if (!sticker) return reply.status(404).send({ error: 'NOT_FOUND' });

    const fileId = sticker.thumbnail?.file_id ?? sticker.file_id;
    try {
      const { buffer, filePath } = await downloadFile(fileId);
      const ext = filePath.split('.').pop()?.toLowerCase();
      const type = CONTENT_TYPES[ext] ?? 'application/octet-stream';
      await redis.set(cacheKey, buffer, 'EX', CACHE_TTL).catch(() => {});
      await redis.set(`${cacheKey}:type`, type, 'EX', CACHE_TTL).catch(() => {});
      return reply.header('cache-control', 'public, max-age=86400').type(type).send(buffer);
    } catch (error) {
      request.log.warn({ err: error, giftId }, 'gift image fetch failed');
      return reply.status(404).send({ error: 'IMAGE_UNAVAILABLE' });
    }
  });
}
