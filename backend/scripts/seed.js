/**
 * Сид 5 кейсов из ЖИВОГО каталога подарков (gift_id не статичны,
 * хардкодить их нельзя). Ровно 5 — все помещаются на один экран.
 * Дорогие подарки получают меньший вес: вес ~ 10000 / star_count.
 */
import { getCatalog } from '../src/services/catalog.js';
import { pool } from '../src/db/pool.js';
import { redis } from '../src/redis.js';

const gifts = await getCatalog({ force: true });
if (gifts.length === 0) {
  console.error('getAvailableGifts вернул пусто — сидить нечего');
  process.exit(1);
}

const sorted = [...gifts].sort((a, b) => a.star_count - b.star_count);
const slice = (from, to) => {
  const part = sorted.slice(
    Math.floor(sorted.length * from), Math.max(Math.floor(sorted.length * to), 1));
  return part.length > 0 ? part : sorted.slice(0, 1);
};

const CASES = [
  { slug: 'starter', title: 'Старт', pool: slice(0, 0.35) },
  { slug: 'bronze', title: 'Бронза', pool: slice(0, 0.55) },
  { slug: 'silver', title: 'Серебро', pool: slice(0.2, 0.75) },
  { slug: 'gold', title: 'Золото', pool: slice(0.4, 1) },
  { slug: 'legend', title: 'Легенда', pool: slice(0.6, 1) },
];

for (const def of CASES) {
  // Цена ≈ 90% средневзвешенной стоимости содержимого (маржа казны 10%).
  const weights = def.pool.map((gift) => Math.max(1, Math.round(10_000 / gift.star_count)));
  const totalWeight = weights.reduce((sum, weight) => sum + weight, 0);
  const expected = def.pool.reduce(
    (sum, gift, i) => sum + gift.star_count * (weights[i] / totalWeight), 0);
  const price = Math.max(1, Math.round(expected / 0.9));

  const { rows: [caseRow] } = await pool.query(
    `INSERT INTO cases (slug, title, price_stars, is_active)
     VALUES ($1, $2, $3, TRUE)
     ON CONFLICT (slug) DO UPDATE SET title = EXCLUDED.title, price_stars = EXCLUDED.price_stars
     RETURNING id`,
    [def.slug, def.title, price],
  );
  await pool.query('DELETE FROM case_items WHERE case_id = $1', [caseRow.id]);
  for (let i = 0; i < def.pool.length; i++) {
    await pool.query(
      'INSERT INTO case_items (case_id, gift_id, weight) VALUES ($1, $2, $3)',
      [caseRow.id, def.pool[i].gift_id, weights[i]],
    );
  }
  console.log(`case "${def.title}": ${def.pool.length} gifts, price ${price} ⭐`);
}

await pool.end();
redis.disconnect();
