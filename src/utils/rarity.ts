import type { Rarity } from "../types";

export function rarityForPrice(price: number): Rarity {
  if (price < 8) return "milspec";
  if (price < 40) return "restricted";
  if (price < 150) return "classified";
  if (price < 500) return "covert";
  return "gold";
}
