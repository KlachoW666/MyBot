import type { CaseData, CasePool, Item, Rarity } from "../types";
import { ITEMS } from "./items";

const RARITY_WEIGHT: Record<Rarity, number> = {
  consumer: 0,
  industrial: 7992,
  milspec: 7992,
  restricted: 1598,
  classified: 320,
  covert: 64,
  gold: 26,
};

function buildPool(items: Item[]): CasePool[] {
  return items.map((item) => ({
    item,
    weight: RARITY_WEIGHT[item.rarity] / items.filter((i) => i.rarity === item.rarity).length,
  }));
}

function byId(...ids: number[]): Item[] {
  return ids.map((i) => ITEMS[i]);
}

export const CASES: CaseData[] = [
  {
    id: "case-neon-rage",
    name: "Neon Rage",
    price: 2.5,
    featured: true,
    accent: "covert",
    pool: buildPool(byId(4, 20, 21, 3, 2, 22, 27, 1, 0, 9, 28)),
  },
  {
    id: "case-desert-storm",
    name: "Desert Storm",
    price: 1.75,
    accent: "restricted",
    pool: buildPool(byId(39, 25, 4, 3, 13, 14, 20, 12, 30, 9)),
  },
  {
    id: "case-phantom-vault",
    name: "Phantom Vault",
    price: 5.0,
    featured: true,
    accent: "gold",
    pool: buildPool(byId(38, 40, 41, 5, 6, 7, 8, 28, 29, 31)),
  },
  {
    id: "case-glacier-drop",
    name: "Glacier Drop",
    price: 3.2,
    accent: "classified",
    pool: buildPool(byId(11, 12, 15, 16, 17, 18, 19, 2, 42, 9)),
  },
  {
    id: "case-blood-hunt",
    name: "Blood Hunt",
    price: 8.9,
    featured: true,
    accent: "gold",
    pool: buildPool(byId(9, 10, 32, 33, 34, 1, 0, 29, 31, 6)),
  },
  {
    id: "case-street-ops",
    name: "Street Ops",
    price: 1.2,
    accent: "milspec",
    pool: buildPool(byId(20, 21, 25, 27, 37, 39, 22, 23, 24, 26)),
  },
  {
    id: "case-royal-palette",
    name: "Royal Palette",
    price: 4.4,
    accent: "restricted",
    pool: buildPool(byId(35, 36, 5, 6, 40, 15, 3, 13, 14, 41)),
  },
];

export function getCaseById(id: string): CaseData | undefined {
  return CASES.find((c) => c.id === id);
}
