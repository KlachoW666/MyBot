import React from 'react';
import { haptic } from '../telegram.js';

/** Палитра тиров: от дешёвого к дорогому. Индекс = позиция в списке. */
const TIERS = ['tier-cyan', 'tier-bronze', 'tier-silver', 'tier-gold', 'tier-legend'];

export function CaseCard({ caseData, tier, hero = false, onOpen }) {
  const preview = caseData.items.slice(0, hero ? 5 : 3);
  const top = caseData.items[caseData.items.length - 1]; // самый дорогой дроп

  return (
    <button
      className={`case-card ${TIERS[tier % TIERS.length]} ${hero ? 'case-hero' : ''}`}
      onClick={() => { haptic.tap(); onOpen(); }}
    >
      <div className="case-glow" />
      <div className="case-emojis">
        {preview.map((item) => (
          <span key={item.gift_id} className="case-emoji">{item.emoji ?? '🎁'}</span>
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
