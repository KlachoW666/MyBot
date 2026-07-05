/** Обёртка window.Telegram.WebApp. Вне Telegram отдаёт заглушку для dev. */
export const tg = window.Telegram?.WebApp ?? {
  initData: '',
  ready() {},
  expand() {},
  openInvoice(_url, cb) { cb?.('failed'); },
  HapticFeedback: { impactOccurred() {}, notificationOccurred() {} },
};

export function initTelegram() {
  tg.ready();
  tg.expand?.();
}

/** Сырой initData — единственное, что фронт отправляет для авторизации. */
export const getInitData = () => tg.initData;

/**
 * Открыть инвойс Stars. Резолвится статусом: 'paid' | 'cancelled' | 'failed'.
 */
export function openInvoice(link) {
  return new Promise((resolve) => tg.openInvoice(link, resolve));
}

export const haptic = {
  tap: () => tg.HapticFeedback?.impactOccurred?.('medium'),
  success: () => tg.HapticFeedback?.notificationOccurred?.('success'),
  error: () => tg.HapticFeedback?.notificationOccurred?.('error'),
};
