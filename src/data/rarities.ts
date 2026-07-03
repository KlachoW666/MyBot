import type { Rarity, RarityInfo } from "../types";

export const RARITIES: Record<Rarity, RarityInfo> = {
  consumer: {
    id: "consumer",
    label: "Ширпотреб",
    color: "#b0c3d9",
    glow: "rgba(176,195,217,0.35)",
    gradient: "linear-gradient(160deg, #2b3038 0%, #171a1f 70%)",
  },
  industrial: {
    id: "industrial",
    label: "Промышленное",
    color: "#5e98d9",
    glow: "rgba(94,152,217,0.35)",
    gradient: "linear-gradient(160deg, #1e3a56 0%, #161c24 70%)",
  },
  milspec: {
    id: "milspec",
    label: "Армейское",
    color: "#4b69ff",
    glow: "rgba(75,105,255,0.4)",
    gradient: "linear-gradient(160deg, #1f2a63 0%, #161826 70%)",
  },
  restricted: {
    id: "restricted",
    label: "Запрещённое",
    color: "#8847ff",
    glow: "rgba(136,71,255,0.45)",
    gradient: "linear-gradient(160deg, #3a1f6d 0%, #1c1626 70%)",
  },
  classified: {
    id: "classified",
    label: "Засекреченное",
    color: "#d32ce6",
    glow: "rgba(211,44,230,0.45)",
    gradient: "linear-gradient(160deg, #5c1a63 0%, #201421 70%)",
  },
  covert: {
    id: "covert",
    label: "Тайное",
    color: "#eb4b4b",
    glow: "rgba(235,75,75,0.5)",
    gradient: "linear-gradient(160deg, #6e1a1a 0%, #241414 70%)",
  },
  gold: {
    id: "gold",
    label: "Редкое спец. предмет",
    color: "#d4af37",
    glow: "rgba(212,175,55,0.55)",
    gradient: "linear-gradient(160deg, #6b551a 0%, #241f14 70%)",
  },
};

export const RARITY_ORDER: Rarity[] = [
  "consumer",
  "industrial",
  "milspec",
  "restricted",
  "classified",
  "covert",
  "gold",
];
