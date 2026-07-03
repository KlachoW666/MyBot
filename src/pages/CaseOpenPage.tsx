import { useMemo, useState } from "react";
import { Link, Navigate, useParams } from "react-router-dom";
import { useCases } from "../store/CasesContext";
import { RARITIES, RARITY_ORDER } from "../data/rarities";
import { useStore } from "../store/StoreContext";
import { RouletteReel } from "../components/RouletteReel";
import { RevealModal } from "../components/RevealModal";
import { ItemCard } from "../components/ItemCard";
import { buildReel, dropChance, weightedRandomItem } from "../utils/roulette";
import type { InventoryItem, Item } from "../types";

export function CaseOpenPage() {
  const { caseId } = useParams();
  const { cases } = useCases();
  const caseData = caseId ? cases.find((c) => c.id === caseId) : undefined;
  const { balance, spend, addToInventory, sellItem, incrementCasesOpened } = useStore();

  const [reel, setReel] = useState<Item[]>([]);
  const [spinKey, setSpinKey] = useState(0);
  const [spinning, setSpinning] = useState(false);
  const [pendingWinner, setPendingWinner] = useState<Item | null>(null);
  const [revealEntry, setRevealEntry] = useState<InventoryItem | null>(null);
  const [fastMode, setFastMode] = useState(false);

  const sortedPool = useMemo(() => {
    if (!caseData) return [];
    return [...caseData.pool].sort(
      (a, b) => RARITY_ORDER.indexOf(b.item.rarity) - RARITY_ORDER.indexOf(a.item.rarity)
    );
  }, [caseData]);

  if (!caseData) return <Navigate to="/" replace />;

  const rarity = RARITIES[caseData.accent];

  function handleOpen() {
    if (!caseData || spinning || caseData.pool.length === 0) return;
    if (!spend(caseData.price)) {
      window.alert("Недостаточно средств на балансе");
      return;
    }
    const winner = weightedRandomItem(caseData.pool);
    const newReel = buildReel(caseData.pool, winner);
    setReel(newReel);
    setPendingWinner(winner);
    setSpinning(true);
    setSpinKey((k) => k + 1);
    setRevealEntry(null);
  }

  function handleSpinComplete() {
    if (!pendingWinner) return;
    setSpinning(false);
    const entry = addToInventory(pendingWinner);
    incrementCasesOpened();
    setRevealEntry(entry);
  }

  return (
    <div className="py-8">
      <div className="mb-4 flex items-center gap-1.5 text-xs text-text-faint">
        <Link to="/" className="hover:text-white">
          Кейсы
        </Link>
        <span>/</span>
        <span className="text-white">{caseData.name}</span>
      </div>

      <div className="flex flex-col items-center gap-2 text-center">
        <div
          className="flex h-20 w-20 items-center justify-center rounded-2xl border border-white/10"
          style={{ background: rarity.gradient, boxShadow: `0 0 40px -10px ${rarity.glow}` }}
        >
          <span className="text-3xl">🎁</span>
        </div>
        <h1 className="text-2xl font-extrabold text-white">{caseData.name}</h1>
        <p className="text-sm text-text-muted">Цена открытия: ${caseData.price.toFixed(2)}</p>
      </div>

      <div className="mx-auto mt-6 max-w-3xl">
        <RouletteReel
          reel={reel.length ? reel : sortedPool.map((p) => p.item)}
          spinKey={spinKey}
          spinning={spinning}
          fastMode={fastMode}
          onComplete={handleSpinComplete}
        />

        {caseData.pool.length === 0 && (
          <p className="mt-3 text-center text-sm text-warning">
            В кейсе пока нет предметов. Добавьте их в{" "}
            <Link to="/admin" className="underline">
              админке
            </Link>
            .
          </p>
        )}

        <div className="mt-5 flex flex-col items-center gap-3 sm:flex-row sm:justify-center">
          <button
            onClick={handleOpen}
            disabled={spinning || balance < caseData.price || caseData.pool.length === 0}
            className="rounded-lg bg-accent px-8 py-3 text-sm font-bold text-white shadow-[0_0_24px_rgba(255,61,90,0.4)] transition hover:brightness-110 disabled:cursor-not-allowed disabled:opacity-40"
          >
            {spinning ? "Открываем..." : `Открыть за $${caseData.price.toFixed(2)}`}
          </button>
          <label className="flex items-center gap-2 text-xs font-semibold text-text-muted">
            <input
              type="checkbox"
              checked={fastMode}
              onChange={(e) => setFastMode(e.target.checked)}
              className="h-4 w-4 accent-[#ff3d5a]"
            />
            Быстрое открытие
          </label>
        </div>
      </div>

      <section className="mt-12">
        <h2 className="mb-4 text-lg font-bold text-white">Содержимое кейса</h2>
        {sortedPool.length === 0 && <p className="text-sm text-text-faint">Пул предметов пуст.</p>}
        <div className="grid grid-cols-3 gap-3 sm:grid-cols-4 md:grid-cols-6 lg:grid-cols-8">
          {sortedPool.map(({ item }) => (
            <ItemCard
              key={item.id}
              item={item}
              size="sm"
              footer={
                <p className="mt-0.5 text-[10px] font-semibold text-text-muted">
                  {dropChance(caseData.pool, item.id).toFixed(2)}%
                </p>
              }
            />
          ))}
        </div>
      </section>

      <RevealModal
        item={revealEntry}
        onClose={() => setRevealEntry(null)}
        onSell={() => {
          if (revealEntry) sellItem(revealEntry.uid);
          setRevealEntry(null);
        }}
        onOpenAgain={() => {
          setRevealEntry(null);
          handleOpen();
        }}
      />
    </div>
  );
}
