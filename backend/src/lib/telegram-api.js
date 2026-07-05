import { config } from '../config.js';

const API_BASE = 'https://api.telegram.org';

/** Ошибка Bot API с разбором кода, описания и retry_after. */
export class TelegramApiError extends Error {
  constructor(method, errorCode, description, parameters = {}) {
    super(`Telegram API ${method} failed: ${errorCode} ${description}`);
    this.method = method;
    this.errorCode = errorCode;
    this.description = description ?? '';
    this.parameters = parameters;
  }

  /** 429 Too Many Requests → сколько секунд ждать. */
  get retryAfter() {
    return this.errorCode === 429 ? (this.parameters.retry_after ?? 1) : null;
  }

  /** Подарок распродан/недоступен (лимитированные gift_id исчезают). */
  get isGiftInvalid() {
    return /GIFT_INVALID|STARGIFT_INVALID|STARGIFT_USAGE_LIMITED|SOLD_OUT/i.test(this.description);
  }

  /** На балансе бота не хватает Stars на отправку подарка. */
  get isInsufficientBotBalance() {
    return /BALANCE_TOO_LOW|NOT_ENOUGH|INSUFFICIENT/i.test(this.description);
  }

  /** Сетевые/5xx-ошибки: имеет смысл повторить. */
  get isTransient() {
    return this.errorCode === 429 || this.errorCode >= 500 || this.errorCode === 0;
  }
}

/**
 * Низкоуровневый вызов Bot API с авто-ретраем на 429/5xx/сетевые ошибки.
 * 429 уважает parameters.retry_after (с потолком maxRetryAfter).
 */
async function call(method, payload = {}, { retries = 3, maxRetryAfter = 15 } = {}) {
  const url = `${API_BASE}/bot${config.botToken}/${method}`;

  for (let attempt = 0; ; attempt++) {
    let error;
    try {
      const response = await fetch(url, {
        method: 'POST',
        headers: { 'content-type': 'application/json' },
        body: JSON.stringify(payload),
        signal: AbortSignal.timeout(30_000),
      });
      const body = await response.json();
      if (body.ok) return body.result;
      error = new TelegramApiError(method, body.error_code ?? response.status,
        body.description, body.parameters ?? {});
    } catch (cause) {
      if (cause instanceof TelegramApiError) throw cause;
      // fetch/timeout/JSON — считаем транзиентной ошибкой с кодом 0.
      error = new TelegramApiError(method, 0, `network error: ${cause.message}`);
    }

    if (!error.isTransient || attempt >= retries) throw error;
    const waitSec = error.retryAfter != null
      ? Math.min(error.retryAfter, maxRetryAfter)
      : Math.min(2 ** attempt, maxRetryAfter); // 1s, 2s, 4s, ...
    await new Promise((resolve) => setTimeout(resolve, waitSec * 1000));
  }
}

/**
 * Список доступных подарков (без параметров).
 * @returns {Promise<Array<object>>} массив объектов Gift
 */
export async function getAvailableGifts() {
  const result = await call('getAvailableGifts');
  return result.gifts ?? [];
}

/**
 * Отправить подарок пользователю. user_id — ТОЛЬКО проверенный telegram_id
 * из сессии: бот шлёт подарок напрямую этому юзеру.
 * Ретраи здесь выключены — идемпотентность контролирует вызывающий
 * (withdrawals-сервис), чтобы не отправить подарок дважды.
 */
export async function sendGift({ userId, giftId, text }) {
  return call('sendGift', {
    user_id: userId,
    gift_id: giftId,
    ...(text ? { text } : {}),
  }, { retries: 0 });
}

/**
 * Инвойс на оплату в Telegram Stars: currency XTR, provider_token не нужен.
 * @returns {Promise<string>} https-ссылка для WebApp.openInvoice
 */
export async function createInvoiceLink({ title, description, payload, amountStars }) {
  return call('createInvoiceLink', {
    title,
    description,
    payload,                 // строка ≤128 байт, вернётся в pre_checkout/successful_payment
    currency: 'XTR',
    prices: [{ label: title, amount: amountStars }],
  });
}

/** Настройка вебхука: только HTTPS + secret_token. */
export async function setWebhook({ url, secretToken }) {
  if (!url.startsWith('https://')) {
    throw new Error('Webhook URL must be HTTPS');
  }
  return call('setWebhook', {
    url,
    secret_token: secretToken,
    allowed_updates: ['message', 'pre_checkout_query'],
  });
}
