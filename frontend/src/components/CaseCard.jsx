import React from 'react';

export function CaseCard({ caseData, onOpen }) {
  const preview = caseData.items.slice(0, 4);
  return (
    <div className="case-card" onClick={onOpen}>
      <div className="case-emoji">
        {preview.map((item) => (
          <span key={item.gift_id}>{item.emoji ?? '🎁'}</span>
        ))}
      </div>
      <div className="case-title">{caseData.title}</div>
      <div className="case-price">{caseData.price_stars} ⭐</div>
    </div>
  );
}
