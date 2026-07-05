import { config } from './config.js';

export let redis;
/** true → настоящий Redis (нужен для мультипроцессного деплоя). */
export let isRealRedis = false;

if (config.redisUrl && config.redisUrl.startsWith('redis')) {
  const { default: Redis } = await import('ioredis');
  redis = new Redis(config.redisUrl, { maxRetriesPerRequest: 2, lazyConnect: true });
  isRealRedis = true;
} else {
  // In-memory замена (один процесс): кэш каталога, локи, картинки подарков.
  const store = new Map(); // key → { value, expiresAt }
  const alive = (entry) => entry && (!entry.expiresAt || entry.expiresAt > Date.now());
  const read = (key) => {
    const entry = store.get(key);
    if (!alive(entry)) { store.delete(key); return null; }
    return entry.value;
  };
  redis = {
    async get(key) { const v = read(key); return v == null ? null : String(v); },
    async getBuffer(key) { return read(key); },
    async set(key, value, ...args) {
      const nx = args.includes('NX');
      const exIdx = args.indexOf('EX');
      if (nx && read(key) !== null) return null;
      store.set(key, {
        value,
        expiresAt: exIdx >= 0 ? Date.now() + Number(args[exIdx + 1]) * 1000 : null,
      });
      return 'OK';
    },
    async del(...keys) { keys.forEach((k) => store.delete(k)); return keys.length; },
    // Используется только unlock-скриптом: удалить, если токен наш.
    async eval(_script, _n, key, token) {
      if (read(key) === token) { store.delete(key); return 1; }
      return 0;
    },
    async flushdb() { store.clear(); return 'OK'; },
    disconnect() {},
  };
}

const UNLOCK_SCRIPT = `
if redis.call("get", KEYS[1]) == ARGV[1] then
  return redis.call("del", KEYS[1])
end
return 0`;

/**
 * Распределённый лок (SET NX EX). Антифрод-барьер поверх БД-инвариантов
 * (уникальные статусы/ключи), а не вместо них.
 * @returns {Promise<null | (() => Promise<void>)>}
 */
export async function acquireLock(key, ttlSec = 30) {
  const token = `${process.pid}:${Math.random().toString(36).slice(2)}`;
  const ok = await redis.set(key, token, 'EX', ttlSec, 'NX');
  if (!ok) return null;
  return async () => {
    await redis.eval(UNLOCK_SCRIPT, 1, key, token).catch(() => {});
  };
}
