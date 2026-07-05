import React from 'react';
import { haptic } from '../telegram.js';

const TABS = [
  { id: 'cases', icon: '🎁', label: 'Кейсы' },
  { id: 'profile', icon: '👤', label: 'Профиль' },
  { id: 'admin', icon: '⚙️', label: 'Админка', adminOnly: true },
];

export function TabBar({ tab, onChange, isAdmin }) {
  const visible = TABS.filter((item) => !item.adminOnly || isAdmin);
  return (
    <nav className="tabbar">
      {visible.map((item) => (
        <button
          key={item.id}
          className={`tab ${tab === item.id ? 'tab-active' : ''}`}
          onClick={() => { haptic.tap(); onChange(item.id); }}
        >
          <span className="tab-icon">{item.icon}</span>
          <span className="tab-label">{item.label}</span>
        </button>
      ))}
    </nav>
  );
}
