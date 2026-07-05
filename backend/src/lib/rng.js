import crypto from 'node:crypto';

/**
 * Серверный взвешенный RNG. crypto.randomInt — CSPRNG без модульного смещения.
 * Клиент никогда не видит ни весов, ни ролла — только готовый результат.
 *
 * @template {{ weight: number }} T
 * @param {T[]} items непустой массив с целыми weight > 0
 * @returns {T}
 */
export function pickWeighted(items) {
  const total = items.reduce((sum, item) => sum + item.weight, 0);
  let roll = crypto.randomInt(total); // [0, total)
  for (const item of items) {
    roll -= item.weight;
    if (roll < 0) return item;
  }
  return items[items.length - 1]; // недостижимо, страховка
}
