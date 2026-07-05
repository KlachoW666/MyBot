import React from 'react';
import { haptic } from '../telegram.js';

export function BalancePill({ balance, onClick }) {
  return (
    <button className="balance-pill" onClick={() => { haptic.tap(); onClick(); }}>
      <span className="balance-star">⭐</span>
      <span className="balance-num">{balance.toLocaleString('ru-RU')}</span>
      <span className="balance-plus">+</span>
    </button>
  );
}
