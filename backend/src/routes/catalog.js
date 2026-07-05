import { getCatalog } from '../services/catalog.js';

/** GET /catalog → актуальный список подарков (Redis-кэш поверх getAvailableGifts). */
export default async function catalogRoutes(fastify) {
  fastify.get('/catalog', {
    preHandler: [fastify.authenticate],
  }, async () => {
    const gifts = await getCatalog();
    return {
      gifts: gifts.map((gift) => ({
        gift_id: gift.gift_id,
        sticker_file_id: gift.sticker_file_id,
        emoji: gift.emoji,
        star_count: gift.star_count,
        upgrade_star_count: gift.upgrade_star_count,
        total_count: gift.total_count,
        remaining_count: gift.remaining_count,
      })),
    };
  });
}
