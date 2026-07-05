import { config } from './config.js';
import { answerPreCheckoutQuery, sendMessage } from './lib/telegram-api.js';
import { parseTopUpPayload, creditDeposit } from './services/payments.js';

/**
 * Обработчик апдейтов Telegram-вебхука. Без фреймворка: нам нужны ровно
 * три сценария, и все исходящие вызовы идут через наш же Bot API клиент
 * (единая обработка ошибок/ретраев/прокси).
 *
 *   /start               → приветствие с кнопкой мини-аппа
 *   pre_checkout_query   → сверка payload/суммы → answerPreCheckoutQuery
 *   successful_payment   → идемпотентное зачисление Stars на баланс
 */
export async function handleUpdate(update, logger) {
  if (update.pre_checkout_query) {
    return handlePreCheckout(update.pre_checkout_query, logger);
  }
  if (update.message?.successful_payment) {
    return handleSuccessfulPayment(update.message, logger);
  }
  if (update.message?.text?.startsWith('/start')) {
    return handleStart(update.message, logger);
  }
}

async function handleStart(message, logger) {
  const button = config.publicUrl
    ? { inline_keyboard: [[{ text: '🎁 Открыть кейсы', web_app: { url: config.publicUrl } }]] }
    : undefined;
  await sendMessage({
    chatId: message.chat.id,
    text: 'Добро пожаловать в Gift Cases! Открывай кейсы, выигрывай подарки и выводи их прямо в Telegram ⭐',
    replyMarkup: button,
  }).catch((err) => logger.warn({ err }, 'failed to reply to /start'));
}

async function handlePreCheckout(query, logger) {
  const valid = query.currency === 'XTR' && parseTopUpPayload(query.invoice_payload, {
    fromId: query.from.id,
    totalAmount: query.total_amount,
  });
  logger.info({ queryId: query.id, from: query.from.id, amount: query.total_amount, valid: Boolean(valid) },
    'pre_checkout_query');
  await answerPreCheckoutQuery({
    queryId: query.id,
    ok: Boolean(valid),
    errorMessage: 'Некорректный платёж, откройте приложение заново.',
  });
}

async function handleSuccessfulPayment(message, logger) {
  const payment = message.successful_payment;
  const payload = parseTopUpPayload(payment.invoice_payload, {
    fromId: message.from.id,
    totalAmount: payment.total_amount,
  });
  if (!payload) {
    logger.error({ payment, from: message.from.id }, 'successful_payment with bad payload');
    return;
  }
  const { duplicate } = await creditDeposit({
    userId: message.from.id,
    username: message.from.username,
    firstName: message.from.first_name,
    amountStars: payment.total_amount,
    chargeId: payment.telegram_payment_charge_id,
  });
  logger.info({
    userId: message.from.id,
    amount: payment.total_amount,
    chargeId: payment.telegram_payment_charge_id,
    duplicate,
  }, 'stars deposit processed');
  if (!duplicate) {
    await sendMessage({
      chatId: message.chat.id,
      text: `Баланс пополнен на ${payment.total_amount} ⭐ — можно открывать кейсы!`,
    }).catch(() => {});
  }
}
