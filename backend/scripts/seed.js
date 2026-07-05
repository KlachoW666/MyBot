/**
 * Сид демо-кейсов из ЖИВОГО каталога подарков (gift_id не статичны,
 * хардкодить их нельзя). Делит доступные подарки на дешёвые/средние/дорогие
 * и собирает 3 кейса: редкие подарки получают меньший вес.
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
const third = Math.max(1, Math.ceil(sorted.length / 3));
const tiers = [
  { slug: 'starter', title: 'Starter Case', pool: sorted.slice(0, third) },
  { slug: 'silver', title: 'Silver Case', pool: sorted.slice(0, third * 2) },
  { slug: 'gold', title: 'Gold Case', pool: sorted },
];

for (const tier of tiers) {
  // Цена кейса ≈ 90% средней стоимости содержимого (положительное матожидание казны).
  const avg = tier.pool.reduce((sum, g) => sum + g.star_count, 0) / tier.pool.length;
  const price = Math.max(1, Math.round(avg * 0.9));

  const { rows: [caseRow] } = await pool.query(
    `INSERT INTO cases (slug, title, price_stars)
     VALUES ($1, $2, $3)
     ON CONFLICT (slug) DO UPDATE SET title = EXCLUDED.title, price_stars = EXCLUDED.price_stars
     RETURNING id`,
    [tier.slug, tier.title, price],
  );
  await pool.query('DELETE FROM case_items WHERE case_id = $1', [caseRow.id]);
  for (const gift of tier.pool) {
    // Вес обратно пропорционален цене: дорогое падает реже.
    const weight = Math.max(1, Math.round(10_000 / gift.star_count));
    await pool.query(
      'INSERT INTO case_items (case_id, gift_id, weight) VALUES ($1, $2, $3)',
      [caseRow.id, gift.gift_id, weight],
    );
  }
  console.log(`case "${tier.title}": ${tier.pool.length} gifts, price ${price} stars`);
}

await pool.end();
redis.disconnect();
