import type { CasePool, Item } from "../types";

export function weightedRandomItem(pool: CasePool[]): Item {
  const total = pool.reduce((sum, p) => sum + p.weight, 0);
  let roll = Math.random() * total;
  for (const p of pool) {
    if (roll < p.weight) return p.item;
    roll -= p.weight;
  }
  return pool[pool.length - 1].item;
}

export const REEL_LENGTH = 60;
export const WINNER_INDEX = 52;

export function buildReel(pool: CasePool[], winner: Item): Item[] {
  const reel: Item[] = [];
  for (let i = 0; i < REEL_LENGTH; i++) {
    if (i === WINNER_INDEX) {
      reel.push(winner);
    } else {
      reel.push(pool[Math.floor(Math.random() * pool.length)].item);
    }
  }
  return reel;
}

export function dropChance(pool: CasePool[], itemId: string): number {
  const total = pool.reduce((sum, p) => sum + p.weight, 0);
  const entry = pool.find((p) => p.item.id === itemId);
  if (!entry || total === 0) return 0;
  return (entry.weight / total) * 100;
}
