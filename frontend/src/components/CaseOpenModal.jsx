import React, { useState } from 'react';
import { openCase } from '../api.js';
import { haptic } from '../telegram.js';

/**
 * Анимация открытия. Результат приходит С СЕРВЕРА до старта «рулетки» —
 * клиент только крутит ленту и останавливает её на выпавшем подарке.
 */
export function CaseOpenModal({ caseData, balance, onClose, onResult }) {
  const [phase, setPhase] = useState('idle'); // idle | spinning | done | error
  const [prize, setPrize] = useState(null);
  const [message, setMessage] = useState(null);

  const canAfford = balance >= caseData.price_stars;

  async function handleOpen() {
    setPhase('spinning');
    haptic.tap();
    try {
      const result = await openCase(caseData.id, crypto.randomUUID());
      // Пока сервер отвечал — лента уже крутится; даём ей докрутиться.
      setTimeout(() => {
        setPrize(result.gift);
        onResult(result);
        setPhase('done');
        haptic.success();
      }, 1800);
    } catch (err) {
      setMessage(err.message);
      setPhase('error');
      haptic.error();
    }
  }

  return (
    <div className="modal-backdrop" onClick={phase === 'spinning' ? undefined : onClose}>
      <div className="modal" onClick={(e) => e.stopPropagation()}>
        <h2>{caseData.title}</h2>

        {phase === 'idle' && (
          <>
            <div className="case-items">
              {caseData.items.map((item) => (
                <div key={item.gift_id} className="mini-gift">
                  <span>{item.emoji ?? '🎁'}</span>
                  <small>{item.star_count} ⭐</small>
                </div>
              ))}
            </div>
            <button className="primary" disabled={!canAfford} onClick={handleOpen}>
              {canAfford
                ? `Открыть за ${caseData.price_stars} ⭐`
                : `Не хватает ${caseData.price_stars - balance} ⭐`}
            </button>
          </>
        )}

        {phase === 'spinning' && (
          <div className="roulette">
            <div className="roulette-strip">
              {[...caseData.items, ...caseData.items, ...caseData.items].map((item, i) => (
                <span key={i} className="roulette-cell">{item.emoji ?? '🎁'}</span>
              ))}
            </div>
            <div className="roulette-pointer" />
          </div>
        )}

        {phase === 'done' && prize && (
          <div className="prize">
            <div className="prize-emoji">{prize.emoji ?? '🎁'}</div>
            <div className="prize-value">{prize.star_count} ⭐</div>
            <p>Подарок в инвентаре — можно вывести в Telegram!</p>
            <button className="primary" onClick={onClose}>Забрать</button>
          </div>
        )}

        {phase === 'error' && (
          <div className="prize">
            <p>⚠️ {message}</p>
            <button className="primary" onClick={onClose}>Закрыть</button>
          </div>
        )}
      </div>
    </div>
  );
}
