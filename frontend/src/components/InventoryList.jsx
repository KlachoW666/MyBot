import React, { useEffect, useState, useCallback } from 'react';
import { getInventory, withdraw } from '../api.js';
import { haptic } from '../telegram.js';
import { GiftImage } from './GiftImage.jsx';

const STATUS = {
  withdraw_pending: { label: 'отправляется', className: 'chip-pending' },
  withdrawn: { label: 'выведен', className: 'chip-done' },
  refunded: { label: 'возврат ⭐', className: 'chip-refund' },
  lost: { label: 'сгорел в апгрейде', className: 'chip-lost' },
};

export function InventoryList({ onBalanceChange }) {
  const [items, setItems] = useState(null);
  const [busyId, setBusyId] = useState(null);
  const [notice, setNotice] = useState(null);

  const load = useCallback(async () => {
    const data = await getInventory();
    setItems(data.items);
  }, []);

  useEffect(() => { load().catch(() => setItems([])); }, [load]);

  async function handleWithdraw(item) {
    setBusyId(item.id);
    setNotice(null);
    haptic.tap();
    try {
      const result = await withdraw(item.id);
      if (result.status === 'withdrawn') {
        setNotice({ kind: 'ok', text: 'Подарок отправлен вам в Telegram 🎉' });
        haptic.success();
      } else if (result.status === 'refunded') {
        setNotice({ kind: 'ok', text: `Подарок распродан — вернули ${result.refundedStars} ⭐` });
        await onBalanceChange();
      }
    } catch (err) {
      setNotice({ kind: 'err', text: err.message });
      haptic.error();
    } finally {
      setBusyId(null);
      await load().catch(() => {});
    }
  }

  if (items === null) return <div className="empty">Загрузка…</div>;
  if (items.length === 0) return <div className="empty">Пока пусто — откройте кейс 🎁</div>;

  return (
    <div className="inventory">
      {notice && <div className={`notice notice-${notice.kind}`}>{notice.text}</div>}
      {items.map((item) => (
        <div key={item.id} className="inv-item">
          <GiftImage giftId={item.gift_id} emoji={item.emoji} size={42} />
          <div className="inv-mid">
            <span className="inv-value">{item.star_value} ⭐</span>
            <span className="inv-date">
              {new Date(item.created_at).toLocaleDateString('ru-RU')}
            </span>
          </div>
          {item.status === 'won' ? (
            <button
              className="btn-primary btn-sm"
              disabled={busyId === item.id}
              onClick={() => handleWithdraw(item)}
            >
              {busyId === item.id ? '⏳' : 'Вывести'}
            </button>
          ) : (
            <span className={`chip ${STATUS[item.status]?.className ?? ''}`}>
              {STATUS[item.status]?.label ?? item.status}
            </span>
          )}
        </div>
      ))}
    </div>
  );
}
