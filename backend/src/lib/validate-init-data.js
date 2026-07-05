import crypto from 'node:crypto';
import { AppError } from './errors.js';

/**
 * Валидация Telegram WebApp initData.
 *
 * Алгоритм (https://core.telegram.org/bots/webapps#validating-data-received-via-the-mini-app):
 *   secret_key        = HMAC_SHA256(key="WebAppData", data=bot_token)
 *   data_check_string = отсортированные "key=value" всех полей кроме hash, через "\n"
 *   hash              = hex(HMAC_SHA256(key=secret_key, data=data_check_string))
 *
 * Дополнительно проверяем auth_date: initData не старше maxAgeSec (защита от replay).
 *
 * @param {string} initData сырая строка window.Telegram.WebApp.initData
 * @param {string} botToken токен бота
 * @param {number} maxAgeSec максимальный возраст auth_date, сек
 * @returns {{ user: {id:number, username?:string, first_name?:string}, authDate: number }}
 * @throws {AppError} 401 при любой проблеме с подписью/возрастом
 */
export function validateInitData(initData, botToken, maxAgeSec = 3600) {
  if (typeof initData !== 'string' || initData.length === 0 || initData.length > 4096) {
    throw new AppError(401, 'INIT_DATA_INVALID', 'initData is missing or malformed');
  }

  const params = new URLSearchParams(initData);
  const receivedHash = params.get('hash');
  if (!receivedHash || !/^[0-9a-f]{64}$/i.test(receivedHash)) {
    throw new AppError(401, 'INIT_DATA_INVALID', 'hash field is missing');
  }
  params.delete('hash');

  // data_check_string: пары key=value, отсортированные по ключу.
  const dataCheckString = [...params.entries()]
    .map(([key, value]) => `${key}=${value}`)
    .sort()
    .join('\n');

  const secretKey = crypto.createHmac('sha256', 'WebAppData').update(botToken).digest();
  const computedHash = crypto.createHmac('sha256', secretKey).update(dataCheckString).digest();

  const ok = crypto.timingSafeEqual(computedHash, Buffer.from(receivedHash, 'hex'));
  if (!ok) {
    throw new AppError(401, 'INIT_DATA_INVALID', 'initData signature mismatch');
  }

  const authDate = Number(params.get('auth_date'));
  if (!Number.isFinite(authDate) || authDate <= 0) {
    throw new AppError(401, 'INIT_DATA_INVALID', 'auth_date is missing');
  }
  const ageSec = Math.floor(Date.now() / 1000) - authDate;
  if (ageSec > maxAgeSec) {
    throw new AppError(401, 'INIT_DATA_EXPIRED', 'initData is too old, reopen the app');
  }

  let user;
  try {
    user = JSON.parse(params.get('user') ?? 'null');
  } catch {
    user = null;
  }
  if (!user || !Number.isInteger(user.id)) {
    throw new AppError(401, 'INIT_DATA_INVALID', 'user field is missing');
  }

  return { user, authDate };
}
