import test from 'node:test';
import assert from 'node:assert/strict';
import crypto from 'node:crypto';
import { validateInitData } from '../src/lib/validate-init-data.js';

const BOT_TOKEN = '123456:TEST_TOKEN';

/** Собирает валидный initData так же, как это делает Telegram. */
function makeInitData(fields) {
  const params = new URLSearchParams(fields);
  const dataCheckString = [...params.entries()]
    .map(([k, v]) => `${k}=${v}`)
    .sort()
    .join('\n');
  const secretKey = crypto.createHmac('sha256', 'WebAppData').update(BOT_TOKEN).digest();
  const hash = crypto.createHmac('sha256', secretKey).update(dataCheckString).digest('hex');
  params.set('hash', hash);
  return params.toString();
}

const freshFields = () => ({
  auth_date: String(Math.floor(Date.now() / 1000)),
  query_id: 'AAF03Qc0AAAAAPTdBzRZq',
  user: JSON.stringify({ id: 42, first_name: 'Test', username: 'tester' }),
});

test('accepts valid initData and extracts user id', () => {
  const { user } = validateInitData(makeInitData(freshFields()), BOT_TOKEN);
  assert.equal(user.id, 42);
  assert.equal(user.username, 'tester');
});

test('rejects tampered user field', () => {
  const fields = freshFields();
  const initData = makeInitData(fields);
  const tampered = initData.replace(
    encodeURIComponent(fields.user),
    encodeURIComponent(JSON.stringify({ id: 999, first_name: 'Hacker' })),
  );
  assert.throws(() => validateInitData(tampered, BOT_TOKEN), /signature mismatch/);
});

test('rejects initData signed with another bot token', () => {
  const initData = makeInitData(freshFields());
  assert.throws(() => validateInitData(initData, 'other:TOKEN'), /signature mismatch/);
});

test('rejects expired auth_date (older than 1h)', () => {
  const fields = freshFields();
  fields.auth_date = String(Math.floor(Date.now() / 1000) - 7200);
  assert.throws(() => validateInitData(makeInitData(fields), BOT_TOKEN), /too old/);
});

test('rejects missing hash / empty input', () => {
  assert.throws(() => validateInitData('', BOT_TOKEN));
  assert.throws(() => validateInitData('auth_date=1&user=%7B%7D', BOT_TOKEN));
});

test('rejects initData without user', () => {
  const fields = freshFields();
  delete fields.user;
  assert.throws(() => validateInitData(makeInitData(fields), BOT_TOKEN), /user field/);
});
