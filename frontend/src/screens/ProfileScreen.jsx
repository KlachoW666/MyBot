import React from 'react';
import { InventoryList } from '../components/InventoryList.jsx';

export function ProfileScreen({ user, onRefresh }) {
  const name = user.first_name ?? user.username ?? 'Игрок';
  return (
    <div className="profile">
      <div className="profile-card">
        <div className="avatar">{name[0].toUpperCase()}</div>
        <div className="profile-info">
          <div className="profile-name">
            {name}
            {user.is_admin && <span className="badge-admin">ADMIN</span>}
          </div>
          <div className="profile-id">
            {user.username ? `@${user.username} · ` : ''}ID {user.telegram_id}
          </div>
        </div>
      </div>

      <div className="stats-row">
        <div className="stat">
          <span className="stat-num">{user.stats?.opened ?? 0}</span>
          <span className="stat-label">открыто</span>
        </div>
        <div className="stat">
          <span className="stat-num">{(user.stats?.won_stars ?? 0).toLocaleString('ru-RU')} ⭐</span>
          <span className="stat-label">выиграно</span>
        </div>
        <div className="stat">
          <span className="stat-num">{user.stats?.withdrawn ?? 0}</span>
          <span className="stat-label">выведено</span>
        </div>
      </div>

      <h3 className="section-title">Инвентарь</h3>
      <InventoryList onBalanceChange={onRefresh} />
    </div>
  );
}
