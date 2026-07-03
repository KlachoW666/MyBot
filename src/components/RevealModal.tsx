import { AnimatePresence, motion } from "framer-motion";
import type { Item } from "../types";
import { RARITIES } from "../data/rarities";
import { WeaponIcon } from "./WeaponIcon";

interface Props {
  item: Item | null;
  onClose: () => void;
  onSell: () => void;
  onOpenAgain: () => void;
}

export function RevealModal({ item, onClose, onSell, onOpenAgain }: Props) {
  const rarity = item ? RARITIES[item.rarity] : null;

  return (
    <AnimatePresence>
      {item && rarity && (
        <motion.div
          className="fixed inset-0 z-50 flex items-center justify-center bg-black/70 backdrop-blur-sm"
          initial={{ opacity: 0 }}
          animate={{ opacity: 1 }}
          exit={{ opacity: 0 }}
          onClick={onClose}
        >
          <motion.div
            className="relative w-[90vw] max-w-sm overflow-hidden rounded-2xl border border-white/10 p-6 text-center"
            style={{ background: rarity.gradient }}
            initial={{ scale: 0.85, y: 20, opacity: 0 }}
            animate={{ scale: 1, y: 0, opacity: 1 }}
            exit={{ scale: 0.9, opacity: 0 }}
            transition={{ type: "spring", stiffness: 260, damping: 20 }}
            onClick={(e) => e.stopPropagation()}
          >
            <div
              className="absolute inset-0 opacity-40"
              style={{ background: `radial-gradient(circle at 50% 0%, ${rarity.glow}, transparent 65%)` }}
            />
            <p className="relative text-xs font-bold uppercase tracking-widest text-white/60">
              Вы получили
            </p>
            <div className="relative mx-auto mt-4 flex h-32 w-32 items-center justify-center rounded-xl border border-white/10 bg-black/20">
              <WeaponIcon category={item.category} className="h-16 w-24 text-white" />
            </div>
            <p className="relative mt-4 text-sm font-semibold text-white/70">{item.weapon}</p>
            <p className="relative text-xl font-extrabold text-white">{item.skin}</p>
            <p className="relative mt-1 text-sm font-bold" style={{ color: rarity.color }}>
              {rarity.label} · ${item.price.toFixed(2)}
            </p>

            <div className="relative mt-6 flex gap-3">
              <button
                onClick={onSell}
                className="flex-1 rounded-lg bg-success/90 py-2.5 text-sm font-bold text-black transition hover:bg-success"
              >
                Продать ${item.price.toFixed(2)}
              </button>
              <button
                onClick={onOpenAgain}
                className="flex-1 rounded-lg bg-accent py-2.5 text-sm font-bold text-white transition hover:brightness-110"
              >
                Открыть ещё
              </button>
            </div>
            <button
              onClick={onClose}
              className="relative mt-3 text-xs font-semibold text-white/50 hover:text-white"
            >
              Забрать в инвентарь
            </button>
          </motion.div>
        </motion.div>
      )}
    </AnimatePresence>
  );
}
