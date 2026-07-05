import React, { useEffect, useState, useCallback } from 'react';
import {
  adminStats, adminCases, adminToggleCase, adminSaveCase,
  adminRefreshCatalog, adminAdjustBalance,
} from '../api.js';
import { haptic } from '../telegram.js';

/** Админка: статистика, управление кейсами, каталог, начисление Stars. */
export function AdminScreen({ user, onRefresh }) {
  const [stats, setStats] = useState(null);
  const [cases, setCases] = useState([]);
  const [notice, setNotice] = useState(null);
  const [busy, setBusy] = useState(false);
  const [amount, setAmount] = useState('500');
  const [targetId, setTargetId] = useState(String(user.telegram_id));

  const load = useCallback(async () => {
    const [statsData, casesData] = await Promise.all([adminStats(), adminCases()]);
    setStats(statsData);
    setCases(casesData.cases);
  }, []);

  useEffect(() => { load().catch((err) => setNotice({ kind: 'err', text: err.message })); }, [load]);

  const run = async (fn, okText) => {
    setBusy(true);
    setNotice(null);
    haptic.tap();
    try {
      await fn();
      if (okText) setNotice({ kind: 'ok', text: okText });
      await load();
      await onRefresh();
    } catch (err) {
      setNotice({ kind: 'err', text: err.message });
      haptic.error();
    } finally {
      setBusy(false);
    }
  };

  if (!stats) return <div className="empty">Загрузка…</div>;

  return (
    <div className="admin">
      {notice && <div className={`notice notice-${notice.kind}`}>{notice.text}</div>}

      <div className="admin-stats">
        <div className="stat"><span className="stat-num">{stats.users}</span><span className="stat-label">юзеров</span></div>
        <div className="stat"><span className="stat-num">{stats.opens}</span><span className="stat-label">открытий</span></div>
        <div className="stat"><span className="stat-num">{stats.deposits_stars.toLocaleString('ru-RU')} ⭐</span><span className="stat-label">депозиты</span></div>
        <div className="stat"><span className="stat-num">{stats.withdrawals?.sent ?? 0}</span><span className="stat-label">выводов</span></div>
      </div>

      <h3 className="section-title">Кейсы <span className="muted">(активных максимум 5)</span></h3>
      <div className="admin-cases">
        {cases.map((caseRow) => (
          <div key={caseRow.id} className={`admin-case ${caseRow.is_active ? '' : 'admin-case-off'}`}>
            <div className="admin-case-info">
              <span className="admin-case-title">{caseRow.title}</span>
              <span className="admin-case-sub">
                {caseRow.price_stars} ⭐ · {caseRow.items.length} подарков
              </span>
            </div>
            <button
              className={`btn-sm ${caseRow.is_active ? 'btn-ghost' : 'btn-primary'}`}
              disabled={busy}
              onClick={() => run(() => adminToggleCase(caseRow.id))}
            >
              {caseRow.is_active ? 'Выключить' : 'Включить'}
            </button>
          </div>
        ))}
      </div>

      <h3 className="section-title">Начислить Stars</h3>
      <div className="admin-form">
        <input value={targetId} onChange={(e) => setTargetId(e.target.value)}
          inputMode="numeric" placeholder="Telegram ID" />
        <input value={amount} onChange={(e) => setAmount(e.target.value)}
          inputMode="numeric" placeholder="Сумма ⭐" />
        <button
          className="btn-primary"
          disabled={busy || !targetId || !amount}
          onClick={() => run(
            () => adminAdjustBalance(Number(targetId), Number(amount)),
            `Начислено ${amount} ⭐ пользователю ${targetId}`)}
        >
          Начислить
        </button>
      </div>

      <h3 className="section-title">Каталог подарков</h3>
      <button
        className="btn-ghost"
        disabled={busy}
        onClick={() => run(() => adminRefreshCatalog(), 'Каталог обновлён из Bot API')}
      >
        🔄 Обновить из getAvailableGifts
      </button>
    </div>
  );
}
