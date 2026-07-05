import React from 'react';
import { haptic } from '../telegram.js';
import { GiftImage } from './GiftImage.jsx';

/** Палитра тиров: от дешёвого к дорогому. Индекс = позиция в списке. */
const TIERS = ['tier-cyan', 'tier-bronze', 'tier-silver', 'tier-gold', 'tier-legend'];

export function CaseCard({ caseData, tier, hero = false, onOpen }) {
  const preview = caseData.items.slice(0, 3);
  const top = caseData.items[caseData.items.length - 1]; // самый дорогой дроп

  return (
    <button
      className={`case-card ${TIERS[tier % TIERS.length]} ${hero ? 'case-hero' : ''}`}
      onClick={() => { haptic.tap(); onOpen(); }}
    >
      <div className="case-glow" />
      <div className="case-gifts">
        {preview.map((item) => (
          <GiftImage key={item.gift_id} giftId={item.gift_id} emoji={item.emoji}
            size={hero ? 50 : 40} />
        ))}
      </div>
      <div className="case-meta">
        <span className="case-title">{caseData.title}</span>
        {top && <span className="case-top">до {top.star_count} ⭐</span>}
      </div>
      <span className="case-price">{caseData.price_stars} ⭐</span>
    </button>
  );
}
