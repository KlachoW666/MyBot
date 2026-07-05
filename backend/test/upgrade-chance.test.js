import test from 'node:test';
import assert from 'node:assert/strict';

process.env.BOT_TOKEN ??= 't';
process.env.WEBHOOK_SECRET ??= 's';
process.env.JWT_SECRET ??= 'j';
process.env.DATABASE_URL ??= 'postgres://x/x';

const { computeChanceBp } = await import('../src/services/upgrades.js');

test('10 stars → 100 stars = 10% (1000 bp)', () => {
  assert.equal(computeChanceBp(10, 100), 1000);
});

test('ratio is proportional', () => {
  assert.equal(computeChanceBp(25, 100), 2500);  // 25%
  assert.equal(computeChanceBp(50, 200), 2500);  // 25%
  assert.equal(computeChanceBp(15, 50), 3000);   // 30%
});

test('capped at 75% max', () => {
  assert.equal(computeChanceBp(90, 100), 7500);   // 90% → 75%
  assert.equal(computeChanceBp(99, 100), 7500);
  assert.equal(computeChanceBp(75, 100), 7500);   // ровно на пороге
});

test('floored at 1% min', () => {
  assert.equal(computeChanceBp(1, 1000), 100);    // 0.1% → 1%
  assert.equal(computeChanceBp(1, 10000), 100);
  assert.equal(computeChanceBp(1, 100), 100);     // ровно на пороге
});
