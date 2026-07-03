import { useState } from "react";
import { useStore } from "../store/StoreContext";
import { ItemCard } from "../components/ItemCard";

const XP_LEVEL = 47;
const XP_CURRENT = 2480;
const XP_NEEDED = 5000;

export function ProfilePage() {
  const { balance, addFunds, inventory, sellItem, sellAll, casesOpened, upgradesPlayed } = useStore();
  const [streamerMode, setStreamerMode] = useState(false);
  const [tab, setTab] = useState<"items" | "upgrades">("items");

  const inventoryValue = inventory.reduce((sum, i) => sum + i.price, 0);
  const displayName = streamerMode ? "Player_" + "*".repeat(5) : "twitch_wodstream";

  return (
    <div className="py-8">
      <div className="flex flex-col gap-6 rounded-2xl border border-white/5 bg-surface p-6 sm:flex-row sm:items-center sm:justify-between">
        <div className="flex items-center gap-4">
          <div className="flex h-16 w-16 items-center justify-center rounded-xl bg-surface-2 text-2xl font-extrabold text-white ring-2 ring-accent/40">
            {streamerMode ? "?" : displayName.slice(0, 1).toUpperCase()}
          </div>
          <div>
            <p className="flex items-center gap-1.5 text-lg font-bold text-white">
              {displayName}
              <span className="text-accent" title="Верифицирован">
                ✔
              </span>
            </p>
            <p className="text-sm text-text-muted">
              Баланс: <span className="font-bold text-white">${balance.toFixed(2)}</span>
            </p>
          </div>
        </div>
        <div className="flex gap-2">
          <button
            onClick={() => addFunds(100)}
            className="rounded-lg bg-success/90 px-4 py-2.5 text-sm font-bold text-black transition hover:bg-success"
          >
            + Пополнить $100
          </button>
          <button className="rounded-lg bg-accent px-4 py-2.5 text-sm font-bold text-white transition hover:brightness-110">
            Trade-URL
          </button>
        </div>
      </div>

      <div className="mt-4 rounded-2xl border border-white/5 bg-surface p-6">
        <div className="flex items-center justify-between text-xs text-text-muted">
          <span className="rounded-md bg-surface-2 px-2 py-1 font-bold text-white">LVL {XP_LEVEL}</span>
          <span>
            {XP_CURRENT} / {XP_NEEDED} XP
          </span>
        </div>
        <div className="mt-2 h-2 w-full overflow-hidden rounded-full bg-surface-2">
          <div
            className="h-full rounded-full bg-success shadow-[0_0_10px_#3ecf5e]"
            style={{ width: `${(XP_CURRENT / XP_NEEDED) * 100}%` }}
          />
        </div>

        <div className="mt-6 flex items-center justify-between border-t border-white/5 pt-4">
          <div>
            <p className="text-sm font-bold text-white">Режим стримера</p>
            <p className="text-xs text-text-faint">Скрывает никнейм и аватар от других пользователей</p>
          </div>
          <button
            onClick={() => setStreamerMode((v) => !v)}
            className={`h-6 w-11 rounded-full transition ${streamerMode ? "bg-accent" : "bg-surface-2"}`}
          >
            <span
              className={`block h-5 w-5 translate-y-0.5 rounded-full bg-white transition-transform ${
                streamerMode ? "translate-x-5" : "translate-x-0.5"
              }`}
            />
          </button>
        </div>
      </div>

      <div className="mt-4 grid grid-cols-2 gap-3 sm:grid-cols-4">
        {[
          ["Кейсы открыто", casesOpened],
          ["Апгрейдов", upgradesPlayed],
          ["Предметов", inventory.length],
          ["Стоимость инвентаря", `$${inventoryValue.toFixed(2)}`],
        ].map(([label, value]) => (
          <div key={label} className="rounded-xl border border-white/5 bg-surface p-4 text-center">
            <p className="text-lg font-extrabold text-white">{value}</p>
            <p className="text-[11px] text-text-faint">{label}</p>
          </div>
        ))}
      </div>

      <section className="mt-8">
        <div className="mb-4 flex flex-wrap items-center justify-between gap-3">
          <div className="flex gap-2">
            <button
              onClick={() => setTab("items")}
              className={`rounded-lg px-3 py-2 text-sm font-bold ${
                tab === "items" ? "bg-accent text-white" : "bg-surface-2 text-text-muted"
              }`}
            >
              Предметы
            </button>
            <button
              onClick={() => setTab("upgrades")}
              className={`rounded-lg px-3 py-2 text-sm font-bold ${
                tab === "upgrades" ? "bg-accent text-white" : "bg-surface-2 text-text-muted"
              }`}
            >
              История
            </button>
          </div>
          {tab === "items" && inventory.length > 0 && (
            <button
              onClick={sellAll}
              className="rounded-lg bg-success/90 px-4 py-2 text-sm font-bold text-black hover:bg-success"
            >
              Продать всё (${inventoryValue.toFixed(2)})
            </button>
          )}
        </div>

        {tab === "items" ? (
          inventory.length === 0 ? (
            <p className="text-sm text-text-faint">Инвентарь пуст. Откройте кейс на главной странице.</p>
          ) : (
            <div className="grid grid-cols-3 gap-3 sm:grid-cols-4 md:grid-cols-6 lg:grid-cols-8">
              {inventory.map((item) => (
                <ItemCard
                  key={item.uid}
                  item={item}
                  size="sm"
                  onClick={() => sellItem(item.uid)}
                  footer={
                    <p className="mt-0.5 text-[10px] font-semibold text-success">${item.price.toFixed(2)} · продать</p>
                  }
                />
              ))}
            </div>
          )
        ) : (
          <p className="text-sm text-text-faint">История апгрейдов появится здесь.</p>
        )}
      </section>
    </div>
  );
}
