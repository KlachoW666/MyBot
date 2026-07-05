import React, { useEffect, useState, useCallback } from 'react';
import { auth, getCases, getMe } from './api.js';
import { CasesScreen } from './screens/CasesScreen.jsx';
import { UpgradeScreen } from './screens/UpgradeScreen.jsx';
import { ProfileScreen } from './screens/ProfileScreen.jsx';
import { AdminScreen } from './screens/AdminScreen.jsx';
import { TabBar } from './components/TabBar.jsx';
import { BalancePill } from './components/BalancePill.jsx';
import { TopUpSheet } from './components/TopUpSheet.jsx';

export function App() {
  const [user, setUser] = useState(null);
  const [cases, setCases] = useState([]);
  const [tab, setTab] = useState('cases');
  const [topUpOpen, setTopUpOpen] = useState(false);
  const [error, setError] = useState(null);

  const refresh = useCallback(async () => {
    const me = await getMe();
    setUser(me);
  }, []);

  useEffect(() => {
    (async () => {
      try {
        await auth();              // initData → JWT
        const [me, { cases }] = await Promise.all([getMe(), getCases()]);
        setUser(me);
        setCases(cases);
      } catch (err) {
        setError(err.message);
      }
    })();
  }, []);

  if (error) {
    return (
      <div className="boot">
        <div className="boot-logo">🎁</div>
        <p className="boot-error">{error}</p>
      </div>
    );
  }
  if (!user) {
    return (
      <div className="boot">
        <div className="boot-logo pulse">🎁</div>
        <p>Загрузка…</p>
      </div>
    );
  }

  return (
    <div className="app">
      <header className="header">
        <div className="brand">
          <span className="brand-icon">🎁</span>
          <span className="brand-name">Gift Cases</span>
        </div>
        <BalancePill balance={user.balance} onClick={() => setTopUpOpen(true)} />
      </header>

      <div className="screen">
        {tab === 'cases' && (
          <CasesScreen
            cases={cases}
            user={user}
            onBalanceChange={(balance) => setUser((u) => ({ ...u, balance }))}
            onRefresh={refresh}
            onTopUp={() => setTopUpOpen(true)}
          />
        )}
        {tab === 'upgrade' && <UpgradeScreen onRefresh={refresh} />}
        {tab === 'profile' && <ProfileScreen user={user} onRefresh={refresh} />}
        {tab === 'admin' && user.is_admin && <AdminScreen user={user} onRefresh={refresh} />}
      </div>

      <TabBar tab={tab} onChange={setTab} isAdmin={user.is_admin} />

      {topUpOpen && <TopUpSheet onClose={() => setTopUpOpen(false)} onPaid={refresh} />}
    </div>
  );
}
