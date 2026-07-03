import { useLayoutEffect, useRef, useState } from "react";
import { motion } from "framer-motion";
import type { Item } from "../types";
import { ItemCard, ITEM_CARD_WIDTH } from "./ItemCard";
import { WINNER_INDEX } from "../utils/roulette";

interface Props {
  reel: Item[];
  spinKey: number;
  spinning: boolean;
  fastMode: boolean;
  onComplete: () => void;
}

const CARD_WIDTH = ITEM_CARD_WIDTH.md;
const GAP = 12;
const STEP = CARD_WIDTH + GAP;

export function RouletteReel({ reel, spinKey, spinning, fastMode, onComplete }: Props) {
  const containerRef = useRef<HTMLDivElement>(null);
  const [targetX, setTargetX] = useState(0);

  useLayoutEffect(() => {
    if (spinKey === 0) {
      setTargetX(0);
      return;
    }
    const width = containerRef.current?.offsetWidth ?? 0;
    const jitter = (Math.random() - 0.5) * (CARD_WIDTH * 0.55);
    const x = -(WINNER_INDEX * STEP + CARD_WIDTH / 2) + width / 2 - jitter;
    setTargetX(x);
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [spinKey]);

  return (
    <div
      ref={containerRef}
      className="relative h-44 overflow-hidden rounded-xl border border-white/5 bg-surface"
    >
      <div className="pointer-events-none absolute inset-y-0 left-1/2 z-20 w-0.5 -translate-x-1/2 bg-accent shadow-[0_0_14px_#ff3d5a]" />
      <div className="pointer-events-none absolute -left-1 top-1/2 z-20 h-3 w-3 -translate-x-1/2 -translate-y-1/2 rotate-45 bg-accent" />
      <div className="pointer-events-none absolute inset-0 z-10 bg-gradient-to-r from-surface via-transparent to-surface" />
      <motion.div
        key={spinKey}
        className="flex h-full items-center gap-3 py-3 pl-3"
        initial={false}
        animate={{ x: targetX }}
        transition={{
          duration: fastMode ? 0.5 : 5,
          ease: [0.11, 0.79, 0.15, 1],
        }}
        onAnimationComplete={() => {
          if (spinning) onComplete();
        }}
      >
        {reel.map((item, i) => (
          <ItemCard key={i} item={item} size="md" selected={!spinning && i === WINNER_INDEX} />
        ))}
      </motion.div>
    </div>
  );
}
