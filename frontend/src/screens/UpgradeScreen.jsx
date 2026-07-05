import React, { useEffect, useState, useCallback } from 'react';
import { getInventory, getUpgradeOptions, postUpgrade } from '../api.js';
import { GiftImage } from '../components/GiftImage.jsx';
import { haptic } from '../telegram.js';

const pct = (bp) => `${(bp / 100).toFixed(bp % 100 === 0 ? 0 : 1)}%`;

/**
 * Апгрейд: свой подарок против более дорогого из каталога.
 * Шанс = цена своего / цена цели (сервер клампит 1%..75%).
 * Исход решает только сервер — тут лишь выбор и анимация.
 */
export function UpgradeScreen({ onRefresh }) {
  const [items, setItems] = useState(null);      // инвентарь (только won)
  const [selected, setSelected] = useState(null);
  const [options, setOptions] = useState(null);  // { targets, min_bp, max_bp }
  const [target, setTarget] = useState(null);
  const [phase, setPhase] = useState('pick');    // pick | rolling | won | lost
  const [result, setResult] = useState(null);
  const [error, setError] = useState(null);

  const loadInventory = useCallback(async () => {
    const data = await getInventory();
    setItems(data.items.filter((item) => item.status === 'won'));
  }, []);

  useEffect(() => { loadInventory().catch(() => setItems([])); }, [loadInventory]);

  async function selectItem(item) {
    haptic.tap();
    setSelected(item);
    setTarget(null);
    setOptions(null);
    setError(null);
    try {
      setOptions(await getUpgradeOptions(item.id));
    } catch (err) {
      setError(err.message);
      setSelected(null);
    }
  }

  async function run() {
    haptic.tap();
    setPhase('rolling');
    setError(null);
    try {
      const res = await postUpgrade(selected.id, target.gift_id);
      // Пауза на «барабан» — исход уже известен серверу.
      setTimeout(() => {
        setResult(res);
        setPhase(res.won ? 'won' : 'lost');
        res.won ? haptic.success() : haptic.error();
      }, 1600);
    } catch (err) {
      setError(err.message);
      setPhase('pick');
      haptic.error();
    }
  }

  async function reset() {
    haptic.tap();
    setSelected(null);
    setOptions(null);
    setTarget(null);
    setResult(null);
    setPhase('pick');
    await loadInventory().catch(() => {});
    await onRefresh();
  }

  if (items === null) return <div className="empty">Загрузка…</div>;
  if (items.length === 0 && phase === 'pick') {
    return <div className="empty">Нет предметов для апгрейда — откройте кейс 🎁</div>;
  }

  const chanceBp = target?.chance_bp ?? null;

  // -------- результат --------
  if (phase === 'won' || phase === 'lost') {
    return (
      <div className="upgrade">
        <div className="upgrade-result">
          {phase === 'won' ? (
            <>
              <div className="prize-burst">
                <GiftImage giftId={result.gift.gift_id} emoji={result.gift.emoji}
                  size={110} className="prize-img" />
              </div>
              <div className="prize-name">Апгрейд удался!</div>
              <p className="prize-sub">{result.gift.star_count} ⭐ теперь в инвентаре</p>
            </>
          ) : (
            <>
              <div className="upgrade-lost-icon">💨</div>
              <div className="prize-name lost-name">Не повезло</div>
              <p className="prize-sub">Предмет сгорел — шанс был {pct(result.chance_bp)}</p>
            </>
          )}
          <button className="btn-primary btn-big" onClick={reset}>Ещё апгрейд</button>
        </div>
      </div>
    );
  }

  return (
    <div className="upgrade">
      {error && <div className="notice notice-err">{error}</div>}

      {/* слоты: свой предмет → цель, между ними шанс */}
      <div className="upgrade-slots">
        <div className={`upgrade-slot ${selected ? 'upgrade-slot-full' : ''}`}>
          {selected
            ? <GiftImage giftId={selected.gift_id} emoji={selected.emoji} size={54} />
            : <span className="upgrade-slot-hint">ваш предмет</span>}
          {selected && <span className="upgrade-slot-price">{selected.star_value} ⭐</span>}
        </div>
        <div className="upgrade-chance">
          {phase === 'rolling'
            ? <span className="upgrade-chance-num rolling">🎲</span>
            : chanceBp !== null
              ? <span className="upgrade-chance-num">{pct(chanceBp)}</span>
              : <span className="upgrade-arrow">→</span>}
          <span className="upgrade-chance-label">
            {chanceBp !== null ? 'шанс' : ''}
          </span>
        </div>
        <div className={`upgrade-slot ${target ? 'upgrade-slot-full' : ''}`}>
          {target
            ? <GiftImage giftId={target.gift_id} emoji={target.emoji} size={54} />
            : <span className="upgrade-slot-hint">цель</span>}
          {target && <span className="upgrade-slot-price">{target.star_count} ⭐</span>}
        </div>
      </div>

      <button className="btn-primary btn-big" disabled={!selected || !target || phase === 'rolling'}
        onClick={run}>
        {phase === 'rolling' ? 'Крутим…'
          : chanceBp !== null ? `Апгрейд · шанс ${pct(chanceBp)}` : 'Выберите предмет и цель'}
      </button>

      {/* шаг 1: выбор своего предмета */}
      {!selected && (
        <>
          <h3 className="section-title">Ваши предметы</h3>
          <div className="upgrade-list">
            {items.map((item) => (
              <button key={item.id} className="upgrade-pick" onClick={() => selectItem(item)}>
                <GiftImage giftId={item.gift_id} emoji={item.emoji} size={40} />
                <span className="upgrade-pick-price">{item.star_value} ⭐</span>
              </button>
            ))}
          </div>
        </>
      )}

      {/* шаг 2: выбор цели с шансами */}
      {selected && options && (
        <>
          <h3 className="section-title">
            Цели <span className="muted">(шанс от {pct(options.min_bp)} до {pct(options.max_bp)})</span>
            <button className="upgrade-back" onClick={() => { setSelected(null); setTarget(null); }}>
              сменить предмет
            </button>
          </h3>
          {options.targets.length === 0 && (
            <div className="empty">Нет подарков дороже вашего предмета</div>
          )}
          <div className="upgrade-list">
            {options.targets.map((gift) => (
              <button key={gift.gift_id}
                className={`upgrade-pick ${target?.gift_id === gift.gift_id ? 'upgrade-pick-on' : ''}`}
                onClick={() => { haptic.tap(); setTarget(gift); }}>
                <GiftImage giftId={gift.gift_id} emoji={gift.emoji} size={40} />
                <span className="upgrade-pick-price">{gift.star_count} ⭐</span>
                <span className="upgrade-pick-chance">{pct(gift.chance_bp)}</span>
              </button>
            ))}
          </div>
        </>
      )}
    </div>
  );
}
