import React, { useState } from 'react';
import { createInvoice } from '../api.js';
import { openInvoice, haptic } from '../telegram.js';

const PRESETS = [50, 100, 250, 500, 1000, 2500];

/** Нижний шит пополнения: инвойс Telegram Stars (XTR) → WebApp.openInvoice. */
export function TopUpSheet({ onClose, onPaid }) {
  const [busy, setBusy] = useState(false);
  const [notice, setNotice] = useState(null);
  const [custom, setCustom] = useState('');

  async function pay(amount) {
    if (!Number.isInteger(amount) || amount < 1) return;
    setBusy(true);
    setNotice(null);
    haptic.tap();
    try {
      const { link } = await createInvoice(amount);
      const status = await openInvoice(link); // paid | cancelled | failed
      if (status === 'paid') {
        haptic.success();
        setNotice({ kind: 'ok', text: 'Оплачено! Баланс обновится через пару секунд.' });
        setTimeout(async () => { await onPaid(); onClose(); }, 2500);
      } else if (status === 'failed') {
        setNotice({ kind: 'err', text: 'Оплата не прошла, попробуйте ещё раз.' });
      }
    } catch (err) {
      setNotice({ kind: 'err', text: err.message });
    } finally {
      setBusy(false);
    }
  }

  return (
    <div className="sheet-backdrop" onClick={onClose}>
      <div className="sheet" onClick={(e) => e.stopPropagation()}>
        <div className="sheet-grip" />
        <h3 className="sheet-title">Пополнить баланс</h3>
        <p className="sheet-sub">Оплата в Telegram Stars, зачисление мгновенное</p>
        {notice && <div className={`notice notice-${notice.kind}`}>{notice.text}</div>}
        <div className="topup-grid">
          {PRESETS.map((amount) => (
            <button key={amount} className="topup-btn" disabled={busy} onClick={() => pay(amount)}>
              <span className="topup-star">⭐</span> {amount}
            </button>
          ))}
        </div>
        <div className="topup-custom">
          <input
            type="number"
            inputMode="numeric"
            min="1"
            placeholder="Своя сумма"
            value={custom}
            onChange={(e) => setCustom(e.target.value)}
          />
          <button
            className="btn-primary"
            disabled={busy || !custom}
            onClick={() => pay(Number(custom))}
          >
            Оплатить
          </button>
        </div>
      </div>
    </div>
  );
}
