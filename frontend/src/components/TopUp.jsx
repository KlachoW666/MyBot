import React, { useState } from 'react';
import { createInvoice } from '../api.js';
import { openInvoice, haptic } from '../telegram.js';

const PRESETS = [50, 100, 250, 500];

export function TopUp({ onPaid }) {
  const [busy, setBusy] = useState(false);
  const [notice, setNotice] = useState(null);

  async function handleTopUp(amount) {
    setBusy(true);
    setNotice(null);
    try {
      const { link } = await createInvoice(amount);
      const status = await openInvoice(link); // paid | cancelled | failed
      if (status === 'paid') {
        haptic.success();
        setNotice('Оплата прошла! Баланс обновится через пару секунд.');
        // Зачисляет вебхук бота; даём ему время и перечитываем баланс.
        setTimeout(onPaid, 2500);
      } else if (status === 'failed') {
        setNotice('⚠️ Оплата не прошла, попробуйте ещё раз.');
      }
    } catch (err) {
      setNotice(`⚠️ ${err.message}`);
    } finally {
      setBusy(false);
    }
  }

  return (
    <main className="topup">
      <h2>Пополнение баланса</h2>
      <p>Оплата в Telegram Stars — прямо внутри приложения.</p>
      {notice && <div className="notice">{notice}</div>}
      <div className="topup-grid">
        {PRESETS.map((amount) => (
          <button key={amount} className="primary" disabled={busy} onClick={() => handleTopUp(amount)}>
            {amount} ⭐
          </button>
        ))}
      </div>
    </main>
  );
}
