import Redis from 'ioredis';
import { config } from './config.js';

export const redis = new Redis(config.redisUrl, {
  maxRetriesPerRequest: 2,
});

const UNLOCK_SCRIPT = `
if redis.call("get", KEYS[1]) == ARGV[1] then
  return redis.call("del", KEYS[1])
end
return 0`;

/**
 * Простой распределённый лок (SET NX EX). Используется как антифрод-барьер
 * поверх БД-инвариантов (уникальные статусы/ключи), а не вместо них.
 *
 * @param {string} key
 * @param {number} ttlSec
 * @returns {Promise<null | (() => Promise<void>)>} null, если лок занят;
 *   иначе функция освобождения (удаляет только свой лок).
 */
export async function acquireLock(key, ttlSec = 30) {
  const token = `${process.pid}:${Math.random().toString(36).slice(2)}`;
  const ok = await redis.set(key, token, 'EX', ttlSec, 'NX');
  if (!ok) return null;
  return async () => {
    await redis.eval(UNLOCK_SCRIPT, 1, key, token).catch(() => {});
  };
}
