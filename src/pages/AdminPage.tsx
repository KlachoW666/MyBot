import { useState } from "react";
import { Link } from "react-router-dom";
import { useCases } from "../store/CasesContext";
import { RARITIES, RARITY_ORDER } from "../data/rarities";
import { ItemCard } from "../components/ItemCard";
import { dropChance } from "../utils/roulette";
import type { Rarity, WeaponCategory } from "../types";

const CATEGORIES: { id: WeaponCategory; label: string }[] = [
  { id: "rifle", label: "Автомат" },
  { id: "sniper", label: "Снайперская винтовка" },
  { id: "smg", label: "ПП" },
  { id: "pistol", label: "Пистолет" },
  { id: "shotgun", label: "Дробовик" },
  { id: "knife", label: "Нож" },
  { id: "gloves", label: "Перчатки" },
];

export function AdminPage() {
  const { cases, itemsCatalog, addCase, updateCase, deleteCase, addPoolEntry, updatePoolWeight, removePoolEntry, createCustomItem } =
    useCases();

  const [selectedCaseId, setSelectedCaseId] = useState<string | null>(cases[0]?.id ?? null);
  const selectedCase = cases.find((c) => c.id === selectedCaseId) ?? null;

  const [newCase, setNewCase] = useState({ name: "", price: 1, accent: "restricted" as Rarity, featured: false });
  const [pickItemId, setPickItemId] = useState("");
  const [pickWeight, setPickWeight] = useState(1000);

  const [customItem, setCustomItem] = useState({
    weapon: "",
    skin: "",
    category: "rifle" as WeaponCategory,
    rarity: "milspec" as Rarity,
    price: 5,
  });

  function handleCreateCase() {
    if (!newCase.name.trim()) return;
    const created = addCase({ ...newCase, price: Number(newCase.price) || 0.5 });
    setSelectedCaseId(created.id);
    setNewCase({ name: "", price: 1, accent: "restricted", featured: false });
  }

  function handleAddExistingItem() {
    if (!selectedCase || !pickItemId) return;
    const item = itemsCatalog.find((i) => i.id === pickItemId);
    if (!item) return;
    addPoolEntry(selectedCase.id, item, Number(pickWeight) || 1);
    setPickItemId("");
  }

  function handleCreateItem() {
    if (!selectedCase || !customItem.weapon.trim() || !customItem.skin.trim()) return;
    const item = createCustomItem({ ...customItem, price: Number(customItem.price) || 1 });
    addPoolEntry(selectedCase.id, item, Number(pickWeight) || 1);
    setCustomItem({ weapon: "", skin: "", category: "rifle", rarity: "milspec", price: 5 });
  }

  const poolTotalWeight = selectedCase?.pool.reduce((s, p) => s + p.weight, 0) ?? 0;

  return (
    <div className="py-8">
      <h1 className="mb-1 text-2xl font-extrabold text-white">Админ-панель</h1>
      <p className="mb-8 text-sm text-text-muted">
        Данные хранятся локально в браузере (localStorage). Бэкенда и реальных выплат нет — это песочница для
        настройки кейсов и проверки шансов.
      </p>

      <div className="grid grid-cols-1 gap-8 lg:grid-cols-[320px_1fr]">
        <div>
          <div className="rounded-2xl border border-white/5 bg-surface p-5">
            <h2 className="mb-3 text-sm font-bold text-white">Новый кейс</h2>
            <div className="flex flex-col gap-2">
              <input
                value={newCase.name}
                onChange={(e) => setNewCase((s) => ({ ...s, name: e.target.value }))}
                placeholder="Название кейса"
                className="rounded-lg border border-white/10 bg-surface-2 px-3 py-2 text-sm text-white outline-none focus:border-accent"
              />
              <input
                type="number"
                step="0.01"
                min="0.1"
                value={newCase.price}
                onChange={(e) => setNewCase((s) => ({ ...s, price: Number(e.target.value) }))}
                placeholder="Цена, $"
                className="rounded-lg border border-white/10 bg-surface-2 px-3 py-2 text-sm text-white outline-none focus:border-accent"
              />
              <select
                value={newCase.accent}
                onChange={(e) => setNewCase((s) => ({ ...s, accent: e.target.value as Rarity }))}
                className="rounded-lg border border-white/10 bg-surface-2 px-3 py-2 text-sm text-white outline-none focus:border-accent"
              >
                {RARITY_ORDER.map((r) => (
                  <option key={r} value={r}>
                    {RARITIES[r].label}
                  </option>
                ))}
              </select>
              <label className="flex items-center gap-2 text-xs text-text-muted">
                <input
                  type="checkbox"
                  checked={newCase.featured}
                  onChange={(e) => setNewCase((s) => ({ ...s, featured: e.target.checked }))}
                  className="h-4 w-4 accent-[#ff3d5a]"
                />
                Показывать в «Популярных»
              </label>
              <button
                onClick={handleCreateCase}
                className="mt-1 rounded-lg bg-accent py-2 text-sm font-bold text-white hover:brightness-110"
              >
                Создать кейс
              </button>
            </div>
          </div>

          <div className="mt-4 flex flex-col gap-2">
            {cases.map((c) => (
              <button
                key={c.id}
                onClick={() => setSelectedCaseId(c.id)}
                className={`flex items-center justify-between rounded-xl border px-3 py-2.5 text-left text-sm transition ${
                  selectedCaseId === c.id
                    ? "border-accent bg-accent/10 text-white"
                    : "border-white/5 bg-surface text-text-muted hover:text-white"
                }`}
              >
                <span className="truncate font-semibold">{c.name}</span>
                <span className="ml-2 shrink-0 text-xs">{c.pool.length} шт.</span>
              </button>
            ))}
          </div>
        </div>

        <div>
          {!selectedCase ? (
            <p className="text-sm text-text-faint">Создайте или выберите кейс слева.</p>
          ) : (
            <div className="flex flex-col gap-6">
              <div className="rounded-2xl border border-white/5 bg-surface p-5">
                <div className="flex flex-wrap items-center justify-between gap-3">
                  <div className="flex flex-wrap items-center gap-2">
                    <input
                      value={selectedCase.name}
                      onChange={(e) => updateCase(selectedCase.id, { name: e.target.value })}
                      className="rounded-lg border border-white/10 bg-surface-2 px-3 py-1.5 text-sm font-bold text-white outline-none focus:border-accent"
                    />
                    <input
                      type="number"
                      step="0.01"
                      value={selectedCase.price}
                      onChange={(e) => updateCase(selectedCase.id, { price: Number(e.target.value) })}
                      className="w-24 rounded-lg border border-white/10 bg-surface-2 px-3 py-1.5 text-sm text-white outline-none focus:border-accent"
                    />
                    <select
                      value={selectedCase.accent}
                      onChange={(e) => updateCase(selectedCase.id, { accent: e.target.value as Rarity })}
                      className="rounded-lg border border-white/10 bg-surface-2 px-3 py-1.5 text-sm text-white outline-none focus:border-accent"
                    >
                      {RARITY_ORDER.map((r) => (
                        <option key={r} value={r}>
                          {RARITIES[r].label}
                        </option>
                      ))}
                    </select>
                  </div>
                  <div className="flex gap-2">
                    <Link
                      to={`/case/${selectedCase.id}`}
                      className="rounded-lg bg-accent px-4 py-2 text-sm font-bold text-white hover:brightness-110"
                    >
                      Открыть кейс (тест)
                    </Link>
                    <button
                      onClick={() => {
                        deleteCase(selectedCase.id);
                        setSelectedCaseId(cases.find((c) => c.id !== selectedCase.id)?.id ?? null);
                      }}
                      className="rounded-lg border border-white/10 px-4 py-2 text-sm font-bold text-text-muted hover:border-accent hover:text-accent"
                    >
                      Удалить
                    </button>
                  </div>
                </div>
              </div>

              <div className="rounded-2xl border border-white/5 bg-surface p-5">
                <h3 className="mb-3 text-sm font-bold text-white">Добавить существующий предмет</h3>
                <div className="flex flex-wrap items-center gap-2">
                  <select
                    value={pickItemId}
                    onChange={(e) => setPickItemId(e.target.value)}
                    className="min-w-[220px] flex-1 rounded-lg border border-white/10 bg-surface-2 px-3 py-2 text-sm text-white outline-none focus:border-accent"
                  >
                    <option value="">Выберите предмет...</option>
                    {itemsCatalog
                      .filter((i) => !selectedCase.pool.some((p) => p.item.id === i.id))
                      .map((i) => (
                        <option key={i.id} value={i.id}>
                          {i.weapon} | {i.skin} — ${i.price.toFixed(2)}
                        </option>
                      ))}
                  </select>
                  <input
                    type="number"
                    value={pickWeight}
                    onChange={(e) => setPickWeight(Number(e.target.value))}
                    className="w-28 rounded-lg border border-white/10 bg-surface-2 px-3 py-2 text-sm text-white outline-none focus:border-accent"
                    placeholder="Вес"
                  />
                  <button
                    onClick={handleAddExistingItem}
                    disabled={!pickItemId}
                    className="rounded-lg bg-accent px-4 py-2 text-sm font-bold text-white hover:brightness-110 disabled:opacity-40"
                  >
                    Добавить
                  </button>
                </div>

                <h3 className="mb-3 mt-6 text-sm font-bold text-white">Или создать новый предмет</h3>
                <div className="flex flex-wrap items-center gap-2">
                  <input
                    value={customItem.weapon}
                    onChange={(e) => setCustomItem((s) => ({ ...s, weapon: e.target.value }))}
                    placeholder="Оружие (AK-47)"
                    className="w-40 rounded-lg border border-white/10 bg-surface-2 px-3 py-2 text-sm text-white outline-none focus:border-accent"
                  />
                  <input
                    value={customItem.skin}
                    onChange={(e) => setCustomItem((s) => ({ ...s, skin: e.target.value }))}
                    placeholder="Название скина"
                    className="w-40 rounded-lg border border-white/10 bg-surface-2 px-3 py-2 text-sm text-white outline-none focus:border-accent"
                  />
                  <select
                    value={customItem.category}
                    onChange={(e) => setCustomItem((s) => ({ ...s, category: e.target.value as WeaponCategory }))}
                    className="rounded-lg border border-white/10 bg-surface-2 px-3 py-2 text-sm text-white outline-none focus:border-accent"
                  >
                    {CATEGORIES.map((c) => (
                      <option key={c.id} value={c.id}>
                        {c.label}
                      </option>
                    ))}
                  </select>
                  <select
                    value={customItem.rarity}
                    onChange={(e) => setCustomItem((s) => ({ ...s, rarity: e.target.value as Rarity }))}
                    className="rounded-lg border border-white/10 bg-surface-2 px-3 py-2 text-sm text-white outline-none focus:border-accent"
                  >
                    {RARITY_ORDER.map((r) => (
                      <option key={r} value={r}>
                        {RARITIES[r].label}
                      </option>
                    ))}
                  </select>
                  <input
                    type="number"
                    step="0.01"
                    value={customItem.price}
                    onChange={(e) => setCustomItem((s) => ({ ...s, price: Number(e.target.value) }))}
                    className="w-24 rounded-lg border border-white/10 bg-surface-2 px-3 py-2 text-sm text-white outline-none focus:border-accent"
                    placeholder="Цена"
                  />
                  <input
                    type="number"
                    value={pickWeight}
                    onChange={(e) => setPickWeight(Number(e.target.value))}
                    className="w-24 rounded-lg border border-white/10 bg-surface-2 px-3 py-2 text-sm text-white outline-none focus:border-accent"
                    placeholder="Вес"
                  />
                  <button
                    onClick={handleCreateItem}
                    className="rounded-lg bg-success/90 px-4 py-2 text-sm font-bold text-black hover:bg-success"
                  >
                    Создать и добавить
                  </button>
                </div>
              </div>

              <div>
                <div className="mb-3 flex items-center justify-between">
                  <h3 className="text-sm font-bold text-white">
                    Содержимое кейса ({selectedCase.pool.length}, суммарный вес {poolTotalWeight})
                  </h3>
                </div>
                {selectedCase.pool.length === 0 ? (
                  <p className="text-sm text-text-faint">Пул пуст — добавьте предметы выше.</p>
                ) : (
                  <div className="flex flex-col gap-2">
                    {selectedCase.pool.map(({ item, weight }) => (
                      <div
                        key={item.id}
                        className="flex flex-wrap items-center gap-3 rounded-xl border border-white/5 bg-surface p-3"
                      >
                        <ItemCard item={item} size="sm" />
                        <div className="flex flex-1 items-center gap-3">
                          <label className="flex items-center gap-2 text-xs text-text-muted">
                            Вес
                            <input
                              type="number"
                              value={weight}
                              onChange={(e) => updatePoolWeight(selectedCase.id, item.id, Number(e.target.value))}
                              className="w-24 rounded-lg border border-white/10 bg-surface-2 px-2 py-1 text-sm text-white outline-none focus:border-accent"
                            />
                          </label>
                          <span className="text-xs font-semibold text-text-muted">
                            Шанс: {dropChance(selectedCase.pool, item.id).toFixed(3)}%
                          </span>
                        </div>
                        <button
                          onClick={() => removePoolEntry(selectedCase.id, item.id)}
                          className="rounded-lg border border-white/10 px-3 py-1.5 text-xs font-bold text-text-muted hover:border-accent hover:text-accent"
                        >
                          Удалить
                        </button>
                      </div>
                    ))}
                  </div>
                )}
              </div>
            </div>
          )}
        </div>
      </div>
    </div>
  );
}
