import { useStore } from "../store/StoreContext";

function StatPill({ icon, value, label }: { icon: string; value: string; label: string }) {
  return (
    <div className="flex shrink-0 items-center gap-2 rounded-lg border border-white/5 bg-surface px-3 py-2">
      <span className="text-base leading-none">{icon}</span>
      <div className="leading-tight">
        <p className="text-sm font-bold text-white">{value}</p>
        <p className="text-[10px] text-text-faint">{label}</p>
      </div>
    </div>
  );
}

export function StatsBar() {
  const { casesOpened, upgradesPlayed, inventory } = useStore();
  const inventoryValue = inventory.reduce((sum, i) => sum + i.price, 0);

  return (
    <div className="no-scrollbar flex gap-2 overflow-x-auto px-4 py-3 lg:px-6">
      <StatPill icon="🟢" value="12,654" label="Онлайн" />
      <StatPill icon="🎁" value={casesOpened.toLocaleString()} label="Кейсов открыто" />
      <StatPill icon="📜" value={upgradesPlayed.toLocaleString()} label="Апгрейдов" />
      <StatPill icon="🎒" value={inventory.length.toLocaleString()} label="В инвентаре" />
      <StatPill
        icon="💰"
        value={`$${inventoryValue.toLocaleString("en-US", { maximumFractionDigits: 0 })}`}
        label="Стоимость инвентаря"
      />
      <StatPill icon="🏆" value="$12,854,032" label="Всего выведено" />
    </div>
  );
}
