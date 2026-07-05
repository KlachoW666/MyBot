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
  const initData = getInitData();
  if (!initData) throw new ApiError(0, 'NO_TELEGRAM', 'Откройте приложение внутри Telegram');
  const data = await request('/auth', { method: 'POST', body: { initData } });
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
