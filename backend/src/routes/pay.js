import { createTopUpInvoice } from '../services/payments.js';

/**
 * POST /pay/invoice { amountStars } → { link }
 * Ссылка createInvoiceLink (XTR); фронт открывает её через WebApp.openInvoice.
 * Зачисление происходит на вебхуке бота (successful_payment), не здесь.
 */
export default async function payRoutes(fastify) {
  fastify.post('/pay/invoice', {
    preHandler: [fastify.authenticate],
    config: { rateLimit: { max: 10, timeWindow: '1 minute' } },
    schema: {
      body: {
        type: 'object',
        required: ['amountStars'],
        properties: { amountStars: { type: 'integer', minimum: 1, maximum: 10000 } },
      },
    },
  }, async (request) => createTopUpInvoice({
    userId: request.userId,
    amountStars: request.body.amountStars,
  }));
}
