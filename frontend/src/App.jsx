import React, { useEffect, useState, useCallback } from 'react';
import { auth, getCases, getMe } from './api.js';
import { CaseCard } from './components/CaseCard.jsx';
import { CaseOpenModal } from './components/CaseOpenModal.jsx';
import { Inventory } from './components/Inventory.jsx';
import { TopUp } from './components/TopUp.jsx';

export function App() {
  const [user, setUser] = useState(null);
  const [cases, setCases] = useState([]);
  const [tab, setTab] = useState('cases');
  const [openingCase, setOpeningCase] = useState(null);
  const [error, setError] = useState(null);

  const refreshBalance = useCallback(async () => {
    const me = await getMe();
    setUser(me);
  }, []);

  useEffect(() => {
    (async () => {
      try {
        const me = await auth();               // initData → JWT
        setUser(me);
        const { cases } = await getCases();
        setCases(cases);
      } catch (err) {
        setError(err.message);
      }
    })();
  }, []);

  if (error) return <div className="screen-msg">⚠️ {error}</div>;
  if (!user) return <div className="screen-msg">Загрузка…</div>;

  return (
    <div className="app">
      <header className="header">
        <span className="hello">👋 {user.first_name ?? user.username ?? user.telegram_id}</span>
        <span className="balance">{user.balance} ⭐</span>
      </header>

      {tab === 'cases' && (
        <main className="grid">
          {cases.map((c) => (
            <CaseCard key={c.id} caseData={c} onOpen={() => setOpeningCase(c)} />
          ))}
          {cases.length === 0 && <div className="screen-msg">Кейсы скоро появятся</div>}
        </main>
      )}
      {tab === 'inventory' && <Inventory onBalanceChange={refreshBalance} />}
      {tab === 'topup' && <TopUp onPaid={refreshBalance} />}

      {openingCase && (
        <CaseOpenModal
          caseData={openingCase}
          balance={user.balance}
          onClose={() => setOpeningCase(null)}
          onResult={(result) => setUser((u) => ({ ...u, balance: result.balance }))}
        />
      )}

      <nav className="tabs">
        <button className={tab === 'cases' ? 'active' : ''} onClick={() => setTab('cases')}>🎁 Кейсы</button>
        <button className={tab === 'inventory' ? 'active' : ''} onClick={() => setTab('inventory')}>📦 Инвентарь</button>
        <button className={tab === 'topup' ? 'active' : ''} onClick={() => setTab('topup')}>⭐ Пополнить</button>
      </nav>
    </div>
  );
}
