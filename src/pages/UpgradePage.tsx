import { useMemo, useState } from "react";
import { useStore } from "../store/StoreContext";
import { UpgradeDial } from "../components/UpgradeDial";
import { ItemCard } from "../components/ItemCard";
import { WeaponIcon } from "../components/WeaponIcon";
import { RARITIES } from "../data/rarities";
import { rarityForPrice } from "../utils/rarity";

const MULTIPLIERS = [1.6, 2, 3, 5, 10];

export function UpgradePage() {
  const { inventory, addToInventory, removeFromInventory, incrementUpgradesPlayed } = useStore();

  const [selectedUid, setSelectedUid] = useState<string | null>(null);
  const [multiplier, setMultiplier] = useState(2);
  const [rotation, setRotation] = useState(0);
  const [spinning, setSpinning] = useState(false);
  const [result, setResult] = useState<"idle" | "win" | "lose">("idle");

  const selected = inventory.find((i) => i.uid === selectedUid) ?? null;
  const chance = Math.min(95, 95 / multiplier);
  const rarity = selected ? RARITIES[selected.rarity] : null;

  const targetPreview = useMemo(() => {
    if (!selected) return null;
    const price = selected.price * multiplier;
    return { price, rarity: RARITIES[rarityForPrice(price)] };
  }, [selected, multiplier]);

  function handleUpgrade() {
    if (!selected || spinning) return;
    setSpinning(true);
    setResult("idle");

    const win = Math.random() * 100 < chance;
    const winDeg = (chance / 100) * 360;
    const landing = win ? Math.random() * winDeg : winDeg + Math.random() * (360 - winDeg);
    const spins = 4 + Math.floor(Math.random() * 3);
    const currentMod = ((rotation % 360) + 360) % 360;
    const delta = ((landing - currentMod + 360) % 360) + spins * 360;
    const nextRotation = rotation + delta;
    setRotation(nextRotation);

    const uidToUpgrade = selected.uid;
    const baseItem = selected;

    setTimeout(() => {
      setResult(win ? "win" : "lose");
      incrementUpgradesPlayed();
      removeFromInventory(uidToUpgrade);
      if (win) {
        const newPrice = baseItem.price * multiplier;
        const upgraded = addToInventory({
          ...baseItem,
          price: newPrice,
          rarity: rarityForPrice(newPrice),
        });
        setSelectedUid(upgraded.uid);
      } else {
        setSelectedUid(null);
      }
      setTimeout(() => {
        setSpinning(false);
      }, 900);
    }, 3200);
  }

  return (
    <div className="py-8">
      <h1 className="mb-1 text-2xl font-extrabold text-white">Апгрейд</h1>
      <p className="mb-8 text-sm text-text-muted">
        Выберите предмет из инвентаря и попробуйте увеличить его стоимость.
      </p>

      <div className="grid grid-cols-1 gap-8 lg:grid-cols-2">
        <div className="flex flex-col items-center justify-center rounded-2xl border border-white/5 bg-surface p-8">
          {selected ? (
            <div
              className="relative flex h-48 w-48 items-center justify-center rounded-2xl border border-white/10"
              style={{ background: rarity?.gradient, boxShadow: `0 0 50px -12px ${rarity?.glow}` }}
            >
              <WeaponIcon category={selected.category} className="h-24 w-36 text-white" />
              <div className="absolute -bottom-3 left-1/2 h-3 w-32 -translate-x-1/2 rounded-full bg-black/40 blur-md" />
            </div>
          ) : (
            <div className="flex h-48 w-48 items-center justify-center rounded-2xl border border-dashed border-white/10 text-center text-sm text-text-faint">
              Выберите предмет ниже
            </div>
          )}
          {selected && (
            <div className="mt-4 text-center">
              <p className="text-sm text-text-muted">{selected.weapon}</p>
              <p className="text-base font-bold text-white">{selected.skin}</p>
              <p className="text-sm font-bold" style={{ color: rarity?.color }}>
                ${selected.price.toFixed(2)}
              </p>
            </div>
          )}
        </div>

        <div className="flex flex-col items-center rounded-2xl border border-white/5 bg-surface p-8">
          <UpgradeDial chance={chance} rotation={rotation} spinning={spinning} result={result} />

          <div className="mt-6 flex flex-wrap justify-center gap-2">
            {MULTIPLIERS.map((m) => (
              <button
                key={m}
                onClick={() => setMultiplier(m)}
                disabled={spinning}
                className={`rounded-lg border px-3 py-1.5 text-sm font-bold transition ${
                  multiplier === m
                    ? "border-accent bg-accent/15 text-accent"
                    : "border-white/10 text-text-muted hover:text-white"
                } disabled:opacity-40`}
              >
                x{m}
              </button>
            ))}
          </div>

          {targetPreview && (
            <p className="mt-3 text-xs text-text-faint">
              При успехе: <span className="font-bold text-white">${targetPreview.price.toFixed(2)}</span>{" "}
              <span style={{ color: targetPreview.rarity.color }}>({targetPreview.rarity.label})</span>
            </p>
          )}

          <button
            onClick={handleUpgrade}
            disabled={!selected || spinning}
            className="mt-6 w-full max-w-xs rounded-lg bg-accent py-3 text-sm font-bold text-white shadow-[0_0_24px_rgba(255,61,90,0.4)] transition hover:brightness-110 disabled:cursor-not-allowed disabled:opacity-40"
          >
            {spinning ? "Крутим..." : "Апгрейд"}
          </button>
        </div>
      </div>

      <section className="mt-10">
        <h2 className="mb-4 text-lg font-bold text-white">Ваш инвентарь</h2>
        {inventory.length === 0 ? (
          <p className="text-sm text-text-faint">
            Инвентарь пуст. Откройте кейс, чтобы получить предметы для апгрейда.
          </p>
        ) : (
          <div className="grid grid-cols-3 gap-3 sm:grid-cols-4 md:grid-cols-6 lg:grid-cols-8">
            {inventory.map((item) => (
              <ItemCard
                key={item.uid}
                item={item}
                size="sm"
                selected={item.uid === selectedUid}
                onClick={() => !spinning && setSelectedUid(item.uid)}
              />
            ))}
          </div>
        )}
      </section>
    </div>
  );
}
