import React, { useEffect, useRef, useState } from 'react';
import { openCase } from '../api.js';
import { haptic } from '../telegram.js';
import { GiftImage } from './GiftImage.jsx';

const CELL = 84;        // ширина ячейки рулетки + gap, px (см. styles.css)
const TARGET = 36;      // индекс ячейки, на которой останавливаемся
const SPIN_MS = 3600;

/**
 * Полноэкранное открытие кейса. Приз определяет СЕРВЕР до старта анимации —
 * лента строится так, чтобы докрутиться ровно до выпавшего подарка.
 */
export function OpenOverlay({ caseData, balance, onBalanceChange, onTopUp, onClose }) {
  const [phase, setPhase] = useState('preview'); // preview | spin | reveal | error
  const [result, setResult] = useState(null);
  const [message, setMessage] = useState(null);
  const [strip, setStrip] = useState([]);
  const stripRef = useRef(null);

  const canAfford = balance >= caseData.price_stars;

  async function spin() {
    haptic.tap();
    try {
      const res = await openCase(caseData.id, crypto.randomUUID());
      // Лента: случайные предметы кейса, приз — на позиции TARGET.
      const cells = Array.from({ length: TARGET + 6 }, (_, i) =>
        i === TARGET
          ? res.gift
          : caseData.items[Math.floor(Math.random() * caseData.items.length)]);
      setStrip(cells);
      setResult(res);
      setPhase('spin');
    } catch (err) {
      setMessage(err.message);
      setPhase('error');
      haptic.error();
    }
  }

  // Запуск прокрутки после отрисовки ленты + звонок в reveal по окончании.
  useEffect(() => {
    if (phase !== 'spin' || !stripRef.current) return;
    const el = stripRef.current;
    // Сдвиг: центр TARGET-ячейки под маркер по центру экрана.
    const offset = TARGET * CELL + CELL / 2 - el.parentElement.clientWidth / 2;
    requestAnimationFrame(() => {
      el.style.transition = `transform ${SPIN_MS}ms cubic-bezier(0.12, 0.75, 0.14, 1)`;
      el.style.transform = `translateX(-${offset}px)`;
    });
    const timer = setTimeout(() => {
      onBalanceChange(result.balance);
      setPhase('reveal');
      haptic.success();
    }, SPIN_MS + 150);
    return () => clearTimeout(timer);
  }, [phase]);

  return (
    <div className="overlay">
      <div className="overlay-head">
        <span className="overlay-title">{caseData.title}</span>
        {phase !== 'spin' && (
          <button className="overlay-close" onClick={onClose}>✕</button>
        )}
      </div>

      {phase === 'preview' && (
        <div className="overlay-body">
          <div className="drops">
            {caseData.items.map((item) => (
              <div key={item.gift_id} className="drop">
                <GiftImage giftId={item.gift_id} emoji={item.emoji} size={46} />
                <span className="drop-price">{item.star_count} ⭐</span>
              </div>
            ))}
          </div>
          {canAfford ? (
            <button className="btn-primary btn-big" onClick={spin}>
              Открыть за {caseData.price_stars} ⭐
            </button>
          ) : (
            <button className="btn-primary btn-big" onClick={onTopUp}>
              Пополнить (не хватает {caseData.price_stars - balance} ⭐)
            </button>
          )}
        </div>
      )}

      {phase === 'spin' && (
        <div className="overlay-body">
          <div className="roulette">
            <div className="roulette-marker" />
            <div className="roulette-strip" ref={stripRef}>
              {strip.map((item, index) => (
                <div key={index} className="roulette-cell">
                  <GiftImage giftId={item.gift_id} emoji={item.emoji} size={58} />
                </div>
              ))}
            </div>
            <div className="roulette-fade roulette-fade-l" />
            <div className="roulette-fade roulette-fade-r" />
          </div>
          <p className="spin-hint">Крутим…</p>
        </div>
      )}

      {phase === 'reveal' && result && (
        <div className="overlay-body reveal">
          <div className="prize-burst">
            <GiftImage giftId={result.gift.gift_id} emoji={result.gift.emoji} size={110} className="prize-img" />
          </div>
          <div className="prize-name">{result.gift.star_count} ⭐</div>
          <p className="prize-sub">Подарок добавлен в инвентарь</p>
          <div className="reveal-actions">
            {result.balance >= caseData.price_stars && (
              <button className="btn-primary" onClick={() => setPhase('preview')}>
                Ещё раз · {caseData.price_stars} ⭐
              </button>
            )}
            <button className="btn-ghost" onClick={onClose}>В инвентарь</button>
          </div>
        </div>
      )}

      {phase === 'error' && (
        <div className="overlay-body reveal">
          <span className="prize-emoji">😕</span>
          <p className="prize-sub">{message}</p>
          <button className="btn-ghost" onClick={onClose}>Закрыть</button>
        </div>
      )}
    </div>
  );
}
