import { getUpgradeOptions, performUpgrade } from '../services/upgrades.js';

export default async function upgradeRoutes(fastify) {
  /**
   * GET /upgrade/options/:inventoryId — цели для апгрейда предмета:
   * подарки дороже него с посчитанным шансом (1%..75%).
   */
  fastify.get('/upgrade/options/:inventoryId', {
    preHandler: [fastify.authenticate],
    schema: {
      params: {
        type: 'object',
        properties: { inventoryId: { type: 'integer', minimum: 1 } },
      },
    },
  }, async (request) => getUpgradeOptions({
    userId: request.userId,
    inventoryId: request.params.inventoryId,
  }));

  /**
   * POST /upgrade { inventoryId, targetGiftId } → { won, chance_bp, gift|lost }
   * Ролл только на сервере; клиент рисует анимацию по готовому исходу.
   */
  fastify.post('/upgrade', {
    preHandler: [fastify.authenticate],
    config: { rateLimit: { max: 10, timeWindow: '1 minute' } },
    schema: {
      body: {
        type: 'object',
        required: ['inventoryId', 'targetGiftId'],
        properties: {
          inventoryId: { type: 'integer', minimum: 1 },
          targetGiftId: { type: 'string', minLength: 1, maxLength: 64 },
        },
      },
    },
  }, async (request) => performUpgrade({
    userId: request.userId,
    inventoryId: request.body.inventoryId,
    targetGiftId: request.body.targetGiftId,
    log: request.log,
  }));
}
