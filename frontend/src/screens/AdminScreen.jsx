import React, { useEffect, useState, useCallback } from 'react';
import {
  adminStats, adminCases, adminCatalog, adminToggleCase,
  adminRefreshCatalog, adminAdjustBalance,
} from '../api.js';
import { CaseBuilder } from '../components/CaseBuilder.jsx';
import { GiftImage } from '../components/GiftImage.jsx';
import { haptic } from '../telegram.js';

/**
 * Админка: статистика, конструктор кейсов из живого каталога подарков
 * Telegram, вкл/выкл кейсов, обновление каталога, начисление Stars.
 */
export function AdminScreen({ user, onRefresh }) {
  const [stats, setStats] = useState(null);
  const [cases, setCases] = useState([]);
  const [catalog, setCatalog] = useState([]);
  const [maxActive, setMaxActive] = useState(5);
  const [builder, setBuilder] = useState(null); // null | { editing?: case }
  const [notice, setNotice] = useState(null);
  const [busy, setBusy] = useState(false);
  const [amount, setAmount] = useState('500');
  const [targetId, setTargetId] = useState(String(user.telegram_id));

  const load = useCallback(async () => {
    const [statsData, casesData, catalogData] = await Promise.all([
      adminStats(), adminCases(), adminCatalog(),
    ]);
    setStats(statsData);
    setCases(casesData.cases);
    setMaxActive(casesData.max_active);
    setCatalog(catalogData.gifts);
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

  if (builder) {
    return (
      <div className="admin">
        <CaseBuilder
          catalog={catalog}
          editing={builder.editing}
          onCancel={() => setBuilder(null)}
          onDone={async () => {
            setBuilder(null);
            setNotice({ kind: 'ok', text: 'Кейс сохранён' });
            await load();
            await onRefresh();
          }}
        />
      </div>
    );
  }

  const activeCount = cases.filter((caseRow) => caseRow.is_active).length;

  return (
    <div className="admin">
      {notice && <div className={`notice notice-${notice.kind}`}>{notice.text}</div>}

      <div className="admin-stats">
        <div className="stat"><span className="stat-num">{stats.users}</span><span className="stat-label">юзеров</span></div>
        <div className="stat"><span className="stat-num">{stats.opens}</span><span className="stat-label">открытий</span></div>
        <div className="stat"><span className="stat-num">{stats.deposits_stars.toLocaleString('ru-RU')} ⭐</span><span className="stat-label">депозиты</span></div>
        <div className="stat"><span className="stat-num">{stats.withdrawals?.sent ?? 0}</span><span className="stat-label">выводов</span></div>
      </div>

      <h3 className="section-title">
        Кейсы <span className="muted">({activeCount}/{maxActive} активных)</span>
      </h3>
      <div className="admin-cases">
        {cases.map((caseRow) => (
          <div key={caseRow.id} className={`admin-case ${caseRow.is_active ? '' : 'admin-case-off'}`}>
            <div className="admin-case-gifts">
              {caseRow.items.slice(0, 3).map((item) => (
                <GiftImage key={item.gift_id} giftId={item.gift_id} emoji={item.emoji} size={30} />
              ))}
            </div>
            <div className="admin-case-info">
              <span className="admin-case-title">{caseRow.title}</span>
              <span className="admin-case-sub">
                {caseRow.price_stars} ⭐ · {caseRow.items.length} подарков
              </span>
            </div>
            <div className="admin-case-actions">
              <button className="btn-sm btn-ghost" disabled={busy}
                onClick={() => setBuilder({ editing: caseRow })}>
                ✏️
              </button>
              <button
                className={`btn-sm ${caseRow.is_active ? 'btn-ghost' : 'btn-primary'}`}
                disabled={busy}
                onClick={() => run(() => adminToggleCase(caseRow.id))}
              >
                {caseRow.is_active ? 'Выкл' : 'Вкл'}
              </button>
            </div>
          </div>
        ))}
      </div>
      <button className="btn-primary btn-big" style={{ marginTop: 10 }}
        onClick={() => { haptic.tap(); setBuilder({}); }}>
        + Создать кейс
      </button>

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

      <h3 className="section-title">
        Каталог подарков <span className="muted">({catalog.filter((g) => g.is_available).length} доступно)</span>
      </h3>
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
