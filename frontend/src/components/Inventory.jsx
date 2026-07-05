import React, { useEffect, useState, useCallback } from 'react';
import { getInventory, withdraw } from '../api.js';
import { haptic } from '../telegram.js';

const STATUS_LABEL = {
  won: null,
  withdraw_pending: 'Отправляется…',
  withdrawn: 'Выведен ✅',
  refunded: 'Возврат ⭐',
};

export function Inventory({ onBalanceChange }) {
  const [items, setItems] = useState(null);
  const [busyId, setBusyId] = useState(null);
  const [notice, setNotice] = useState(null);

  const load = useCallback(async () => {
    const { items } = await getInventory();
    setItems(items);
  }, []);

  useEffect(() => { load().catch(() => setItems([])); }, [load]);

  async function handleWithdraw(item) {
    setBusyId(item.id);
    setNotice(null);
    try {
      const result = await withdraw(item.id);
      if (result.status === 'withdrawn') {
        setNotice('Подарок отправлен вам в Telegram 🎉');
        haptic.success();
      } else if (result.status === 'refunded') {
        setNotice(`Подарок распродан — вернули ${result.refundedStars} ⭐ на баланс`);
        await onBalanceChange();
      }
    } catch (err) {
      setNotice(`⚠️ ${err.message}`);
      haptic.error();
    } finally {
      setBusyId(null);
      await load().catch(() => {});
    }
  }

  if (items === null) return <div className="screen-msg">Загрузка…</div>;
  if (items.length === 0) return <div className="screen-msg">Пока пусто — откройте кейс!</div>;

  return (
    <main className="inventory">
      {notice && <div className="notice">{notice}</div>}
      {items.map((item) => (
        <div key={item.id} className="inv-row">
          <span className="inv-emoji">{item.emoji ?? '🎁'}</span>
          <span className="inv-value">{item.star_value} ⭐</span>
          {item.status === 'won' ? (
            <button
              className="secondary"
              disabled={busyId === item.id}
              onClick={() => handleWithdraw(item)}
            >
              {busyId === item.id ? '…' : 'Вывести'}
            </button>
          ) : (
            <span className="inv-status">{STATUS_LABEL[item.status]}</span>
          )}
        </div>
      ))}
    </main>
  );
}
