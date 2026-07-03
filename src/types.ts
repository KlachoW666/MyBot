export type Rarity =
  | "consumer"
  | "industrial"
  | "milspec"
  | "restricted"
  | "classified"
  | "covert"
  | "gold";

export type WeaponCategory =
  | "rifle"
  | "sniper"
  | "smg"
  | "pistol"
  | "shotgun"
  | "knife"
  | "gloves";

export interface RarityInfo {
  id: Rarity;
  label: string;
  color: string;
  glow: string;
  gradient: string;
}

export interface Item {
  id: string;
  weapon: string;
  skin: string;
  category: WeaponCategory;
  rarity: Rarity;
  price: number;
  wear?: string;
  statTrak?: boolean;
}

export interface CasePool {
  item: Item;
  weight: number;
}

export interface CaseData {
  id: string;
  name: string;
  price: number;
  featured?: boolean;
  accent: Rarity;
  pool: CasePool[];
}

export interface InventoryItem extends Item {
  uid: string;
  obtainedAt: number;
}
