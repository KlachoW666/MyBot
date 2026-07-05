import test from 'node:test';
import assert from 'node:assert/strict';
import { pickWeighted } from '../src/lib/rng.js';

test('always returns one of the items', () => {
  const items = [{ id: 'a', weight: 1 }, { id: 'b', weight: 5 }];
  for (let i = 0; i < 200; i++) {
    assert.ok(items.includes(pickWeighted(items)));
  }
});

test('single item is always picked', () => {
  const only = { id: 'x', weight: 3 };
  assert.equal(pickWeighted([only]), only);
});

test('distribution roughly follows weights', () => {
  const items = [{ id: 'common', weight: 90 }, { id: 'rare', weight: 10 }];
  let rare = 0;
  const n = 20_000;
  for (let i = 0; i < n; i++) {
    if (pickWeighted(items).id === 'rare') rare++;
  }
  const share = rare / n; // ожидаем ~0.10
  assert.ok(share > 0.07 && share < 0.13, `rare share ${share} out of bounds`);
});
