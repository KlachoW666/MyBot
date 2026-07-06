import { getInitData } from './telegram.js';

const BASE = '/api';
let token = null;

export class ApiError extends Error {
  constructor(status, code, message) {
    super(message);
    this.status = status;
    this.code = code;
  }
}

async function request(path, { method = 'GET', body } = {}) {
  const response = await fetch(`${BASE}${path}`, {
    method,
    headers: {
      'content-type': 'application/json',
      ...(token ? { authorization: `Bearer ${token}` } : {}),
    },
    body: body ? JSON.stringify(body) : undefined,
  });
  const data = await response.json().catch(() => ({}));
  if (!response.ok) {
    // Протухший JWT → одна попытка тихой переавторизации через initData.
    if (response.status === 401 && token) {
      token = null;
      await auth();
      return request(path, { method, body });
    }
    throw new ApiError(response.status, data.error ?? 'UNKNOWN', data.message ?? 'Request failed');
  }
  return data;
}

/** POST /auth: initData → JWT. Токен живёт в памяти, не в localStorage. */
export async function auth() {
  // Пустой initData: сервер пустит только в dev-режиме (DEV_USER_ID).
  const data = await request('/auth', { method: 'POST', body: { initData: getInitData() ?? '' } });
  token = data.token;
  return data.user;
}

export const getMe = () => request('/me');
export const getInventory = () => request('/me/inventory');
export const getCatalog = () => request('/catalog');
export const getCases = () => request('/cases');

export const openCase = (caseId, idempotencyKey) =>
  request(`/cases/${caseId}/open`, { method: 'POST', body: { idempotencyKey } });

export const createInvoice = (amountStars) =>
  request('/pay/invoice', { method: 'POST', body: { amountStars } });

export const withdraw = (inventoryId) =>
  request('/withdraw', { method: 'POST', body: { inventoryId } });

export const getUpgradeOptions = (inventoryId) =>
  request(`/upgrade/options/${inventoryId}`);

export const postUpgrade = (inventoryId, targetGiftId) =>
  request('/upgrade', { method: 'POST', body: { inventoryId, targetGiftId } });

// --- Админка (доступна только ID из ADMIN_IDS, сервер проверяет сам) ---
export const adminStats = () => request('/admin/stats');
export const adminCases = () => request('/admin/cases');
export const adminCatalog = () => request('/admin/catalog');
export const adminSaveCase = (payload) => request('/admin/cases', { method: 'POST', body: payload });
export const adminToggleCase = (id) => request(`/admin/cases/${id}/toggle`, { method: 'POST' });
export const adminRefreshCatalog = () => request('/admin/catalog/refresh', { method: 'POST' });
export const adminWithdrawals = () => request('/admin/withdrawals');
export const adminAdjustBalance = (telegramId, amount) =>
  request('/admin/balance', { method: 'POST', body: { telegram_id: telegramId, amount } });
