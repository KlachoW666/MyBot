import clsx from "clsx";
import type { CaseData } from "../types";
import { RARITIES } from "../data/rarities";

interface Props {
  data: CaseData;
  size?: "sm" | "md";
}

const SIZE_MAP = {
  sm: { box: "w-32", art: "h-16 w-16" },
  md: { box: "w-full", art: "h-24 w-24" },
};

export function CaseCard({ data, size = "md" }: Props) {
  const rarity = RARITIES[data.accent];
  const s = SIZE_MAP[size];

  return (
    <div
      className={clsx(
        "group relative flex flex-col items-center overflow-hidden rounded-xl border border-white/5 bg-surface p-3 transition-all hover:-translate-y-1 hover:border-white/10",
        s.box
      )}
      style={{ boxShadow: `inset 0 0 40px -20px ${rarity.glow}` }}
    >
      {data.featured && (
        <span className="absolute left-2 top-2 z-10 text-base" title="Популярный">
          👑
        </span>
      )}
      <div className={clsx("relative flex items-center justify-center", s.art)}>
        <div
          className="absolute inset-0 rounded-full blur-xl"
          style={{ background: rarity.glow }}
        />
        <svg viewBox="0 0 100 100" className="relative h-full w-full drop-shadow-lg">
          <defs>
            <linearGradient id={`crate-${data.id}`} x1="0" y1="0" x2="1" y2="1">
              <stop offset="0%" stopColor={rarity.color} stopOpacity="0.9" />
              <stop offset="100%" stopColor="#1a1a22" />
            </linearGradient>
          </defs>
          <rect x="18" y="30" width="64" height="50" rx="6" fill={`url(#crate-${data.id})`} stroke={rarity.color} strokeWidth="1.5" />
          <path d="M18 42 h64" stroke="#0a0a0e" strokeWidth="2" />
          <rect x="42" y="42" width="16" height="20" rx="2" fill="#0a0a0e" stroke={rarity.color} strokeWidth="1.2" />
          <path d="M22 30 L50 16 L78 30" fill="none" stroke={rarity.color} strokeWidth="2" strokeLinejoin="round" />
        </svg>
      </div>
      <p className="mt-2 truncate text-sm font-bold text-white">{data.name}</p>
      <p className="text-xs font-semibold" style={{ color: rarity.color }}>
        ${data.price.toFixed(2)}
      </p>
    </div>
  );
}
