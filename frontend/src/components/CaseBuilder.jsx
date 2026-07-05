import React, { useMemo, useState } from 'react';
import { adminSaveCase } from '../api.js';
import { GiftImage } from './GiftImage.jsx';
import { haptic } from '../telegram.js';

const slugify = (text) =>
  text.toLowerCase().replace(/[^a-z0-9]+/g, '-').replace(/^-+|-+$/g, '')
  || `case-${Date.now().toString(36)}`;

/** Рекомендованный вес: дорогое падает реже (~10000 / цена). */
const suggestedWeight = (starCount) => Math.max(1, Math.round(10_000 / starCount));

/**
 * Конструктор кейса: подарки из ЖИВОГО каталога Telegram с ценами и
 * лимитированностью. Вес > 0 включает подарок в кейс; тут же считаются
 * шанс каждого дропа и матожидание против цены кейса.
 */
export function CaseBuilder({ catalog, editing, onDone, onCancel }) {
  const [title, setTitle] = useState(editing?.title ?? '');
  const [price, setPrice] = useState(editing ? String(editing.price_stars) : '');
  const [weights, setWeights] = useState(() => {
    const initial = {};
    for (const item of editing?.items ?? []) initial[item.gift_id] = item.weight;
    return initial;
  });
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState(null);

  const picked = catalog.filter((gift) => (weights[gift.gift_id] ?? 0) > 0);
  const totalWeight = picked.reduce((sum, gift) => sum + weights[gift.gift_id], 0);
  const expectedValue = useMemo(
    () => picked.reduce(
      (sum, gift) => sum + gift.star_count * (weights[gift.gift_id] / totalWeight), 0),
    [picked, weights, totalWeight],
  );

  const toggleGift = (gift) => {
    haptic.tap();
    setWeights((prev) => {
      const next = { ...prev };
      if (next[gift.gift_id] > 0) delete next[gift.gift_id];
      else next[gift.gift_id] = suggestedWeight(gift.star_count);
      return next;
    });
  };

  const setWeight = (giftId, value) => {
    const weight = Math.max(0, Math.floor(Number(value) || 0));
    setWeights((prev) => ({ ...prev, [giftId]: weight }));
  };

  async function save() {
    setBusy(true);
    setError(null);
    try {
      await adminSaveCase({
        slug: editing?.slug ?? slugify(title),
        title: title.trim(),
        price_stars: Number(price),
        is_active: editing?.is_active ?? true,
        items: picked.map((gift) => ({ gift_id: gift.gift_id, weight: weights[gift.gift_id] })),
      });
      haptic.success();
      onDone();
    } catch (err) {
      setError(err.message);
      haptic.error();
    } finally {
      setBusy(false);
    }
  }

  const canSave = title.trim().length > 0 && Number(price) > 0 && picked.length > 0 && !busy;

  return (
    <div className="builder">
      <div className="builder-head">
        <button className="overlay-close" onClick={onCancel}>←</button>
        <h3 className="section-title" style={{ margin: 0 }}>
          {editing ? `Кейс «${editing.title}»` : 'Новый кейс'}
        </h3>
      </div>

      {error && <div className="notice notice-err">{error}</div>}

      <div className="admin-form">
        <input placeholder="Название кейса" value={title}
          onChange={(e) => setTitle(e.target.value)} maxLength={64} />
        <input placeholder="Цена открытия, ⭐" inputMode="numeric" value={price}
          onChange={(e) => setPrice(e.target.value)} />
      </div>

      {picked.length > 0 && (
        <div className="builder-summary">
          <span>{picked.length} подарков</span>
          <span>среднее ≈ {Math.round(expectedValue)} ⭐</span>
          <span className={Number(price) > expectedValue ? 'ok-text' : 'warn-text'}>
            маржа {Number(price) > 0
              ? `${Math.round((1 - expectedValue / Number(price)) * 100)}%`
              : '—'}
          </span>
        </div>
      )}

      <h3 className="section-title">Подарки из каталога Telegram</h3>
      <div className="builder-gifts">
        {catalog.map((gift) => {
          const weight = weights[gift.gift_id] ?? 0;
          const isPicked = weight > 0;
          const limited = gift.total_count != null;
          return (
            <div key={gift.gift_id}
              className={`builder-gift ${isPicked ? 'builder-gift-on' : ''} ${gift.is_available ? '' : 'builder-gift-off'}`}>
              <button className="builder-gift-main" disabled={!gift.is_available}
                onClick={() => toggleGift(gift)}>
                <GiftImage giftId={gift.gift_id} emoji={gift.emoji} size={44} />
                <div className="builder-gift-info">
                  <span className="builder-gift-price">{gift.star_count} ⭐</span>
                  <span className="builder-gift-sub">
                    {!gift.is_available ? 'распродан'
                      : limited ? `лимит: ${gift.remaining_count ?? '?'} из ${gift.total_count}`
                      : 'без лимита'}
                    {gift.upgrade_star_count ? ` · апгрейд ${gift.upgrade_star_count} ⭐` : ''}
                  </span>
                </div>
              </button>
              {isPicked && (
                <div className="builder-weight">
                  <input inputMode="numeric" value={weight}
                    onChange={(e) => setWeight(gift.gift_id, e.target.value)} />
                  <span className="builder-chance">
                    {totalWeight > 0 ? `${((weight / totalWeight) * 100).toFixed(1)}%` : ''}
                  </span>
                </div>
              )}
            </div>
          );
        })}
      </div>

      <button className="btn-primary btn-big" disabled={!canSave} onClick={save}>
        {busy ? 'Сохраняю…' : editing ? 'Сохранить кейс' : 'Создать кейс'}
      </button>
    </div>
  );
}
