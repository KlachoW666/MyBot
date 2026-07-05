import React, { useState } from 'react';
import { CaseCard } from '../components/CaseCard.jsx';
import { OpenOverlay } from '../components/OpenOverlay.jsx';

/**
 * Главный экран: до 5 кейсов, все помещаются в один вьюпорт без скролла.
 * Первый (самый дешёвый) — широкая hero-карта, остальные — сетка 2×2.
 */
export function CasesScreen({ cases, user, onBalanceChange, onRefresh, onTopUp }) {
  const [opening, setOpening] = useState(null);
  const [hero, ...rest] = cases;

  if (cases.length === 0) {
    return <div className="empty">Кейсы скоро появятся ✨</div>;
  }

  return (
    <div className="cases">
      {hero && (
        <CaseCard caseData={hero} tier={0} hero onOpen={() => setOpening(hero)} />
      )}
      <div className="cases-grid">
        {rest.map((caseData, index) => (
          <CaseCard
            key={caseData.id}
            caseData={caseData}
            tier={index + 1}
            onOpen={() => setOpening(caseData)}
          />
        ))}
      </div>

      {opening && (
        <OpenOverlay
          caseData={opening}
          balance={user.balance}
          onBalanceChange={onBalanceChange}
          onTopUp={() => { setOpening(null); onTopUp(); }}
          onClose={() => { setOpening(null); onRefresh(); }}
        />
      )}
    </div>
  );
}
