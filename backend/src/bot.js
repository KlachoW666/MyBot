import { Bot } from 'grammy';
import { config } from './config.js';
import { parseTopUpPayload, creditDeposit } from './services/payments.js';

/**
 * grammY-бот. Работает через webhook (см. routes/webhook.js), сам ничего
 * не поллит. Обрабатывает платёжный цикл Telegram Stars:
 *   pre_checkout_query → answerPreCheckoutQuery
 *   message:successful_payment → идемпотентное зачисление баланса
 */
export function createBot(logger, { botInfo } = {}) {
  // botInfo (JSON из env BOT_INFO или из тестов) позволяет пропустить сетевой
  // getMe на старте — иначе grammY бесконечно ретраит его при недоступном API.
  const bot = new Bot(config.botToken, botInfo ? { botInfo } : undefined);

  bot.command('start', (ctx) =>
    ctx.reply('Открывай кейсы с подарками в мини-приложении! Кнопка меню ниже 👇'));

  // Telegram ждёт ответ ≤10 сек, иначе платёж отменится.
  bot.on('pre_checkout_query', async (ctx) => {
    const q = ctx.preCheckoutQuery;
    const valid = q.currency === 'XTR' && parseTopUpPayload(q.invoice_payload, {
      fromId: q.from.id,
      totalAmount: q.total_amount,
    });
    await ctx.answerPreCheckoutQuery(Boolean(valid), valid ? undefined : {
      error_message: 'Некорректный платёж, откройте приложение заново.',
    });
  });

  bot.on('message:successful_payment', async (ctx) => {
    const payment = ctx.message.successful_payment;
    const payload = parseTopUpPayload(payment.invoice_payload, {
      fromId: ctx.from.id,
      totalAmount: payment.total_amount,
    });
    if (!payload) {
      logger.error({ payment, from: ctx.from.id }, 'successful_payment with bad payload');
      return;
    }
    const { duplicate } = await creditDeposit({
      userId: ctx.from.id,
      username: ctx.from.username,
      firstName: ctx.from.first_name,
      amountStars: payment.total_amount,
      chargeId: payment.telegram_payment_charge_id,
    });
    logger.info({
      userId: ctx.from.id,
      amount: payment.total_amount,
      chargeId: payment.telegram_payment_charge_id,
      duplicate,
    }, 'stars deposit processed');
    if (!duplicate) {
      await ctx.reply(`Баланс пополнен на ${payment.total_amount} ⭐`).catch(() => {});
    }
  });

  bot.catch((err) => logger.error({ err: err.error }, 'bot handler error'));
  return bot;
}
