import clsx from "clsx";
import type { Item } from "../types";
import { RARITIES } from "../data/rarities";
import { WeaponIcon } from "./WeaponIcon";

interface Props {
  item: Item;
  size?: "sm" | "md" | "lg";
  badge?: string;
  selected?: boolean;
  onClick?: () => void;
  footer?: React.ReactNode;
}

const SIZE_MAP = {
  sm: "h-28 w-24",
  md: "h-36 w-32",
  lg: "h-48 w-40",
};

export const ITEM_CARD_WIDTH = { sm: 96, md: 128, lg: 160 } as const;

export function ItemCard({ item, size = "md", badge, selected, onClick, footer }: Props) {
  const rarity = RARITIES[item.rarity];

  return (
    <div
      onClick={onClick}
      className={clsx(
        "group relative flex shrink-0 flex-col overflow-hidden rounded-xl border transition-all",
        SIZE_MAP[size],
        onClick && "cursor-pointer hover:-translate-y-0.5",
        selected ? "border-2" : "border-white/5"
      )}
      style={{
        background: rarity.gradient,
        borderColor: selected ? rarity.color : undefined,
        boxShadow: selected ? `0 0 0 1px ${rarity.color}, 0 8px 24px -8px ${rarity.glow}` : undefined,
      }}
    >
      <div
        className="absolute inset-x-0 bottom-0 h-1"
        style={{ background: rarity.color, boxShadow: `0 0 12px ${rarity.glow}` }}
      />
      {badge && (
        <span
          className="absolute left-1.5 top-1.5 z-10 rounded-md px-1.5 py-0.5 text-[9px] font-bold uppercase tracking-wide text-black"
          style={{ background: rarity.color }}
        >
          {badge}
        </span>
      )}
      <div className="flex flex-1 items-center justify-center px-3 pt-2 text-white/90">
        <WeaponIcon category={item.category} className="h-10 w-full -rotate-6 opacity-90 transition-transform group-hover:-rotate-3 group-hover:scale-105" />
      </div>
      <div className="px-2.5 pb-2">
        <p className="truncate text-[10px] font-medium text-white/50">{item.weapon}</p>
        <p className="truncate text-xs font-bold text-white">{item.skin}</p>
        {footer ?? (
          <p className="mt-0.5 text-xs font-bold" style={{ color: rarity.color }}>
            ${item.price.toFixed(2)}
          </p>
        )}
      </div>
    </div>
  );
}
