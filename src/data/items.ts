import type { Item, WeaponCategory, Rarity } from "../types";

let uidCounter = 0;
function makeItem(
  weapon: string,
  skin: string,
  category: WeaponCategory,
  rarity: Rarity,
  price: number,
  extra?: Partial<Item>
): Item {
  uidCounter += 1;
  return {
    id: `item-${uidCounter}`,
    weapon,
    skin,
    category,
    rarity,
    price,
    wear: "Слегка поношенное",
    ...extra,
  };
}

export const ITEMS: Item[] = [
  makeItem("AK-47", "Азимов", "rifle", "covert", 412.5),
  makeItem("AK-47", "Огненный змей", "rifle", "covert", 1180.0),
  makeItem("AK-47", "Красная линия", "rifle", "classified", 68.2),
  makeItem("AK-47", "Пустынный шторм", "rifle", "restricted", 24.9),
  makeItem("AK-47", "Гвардеец", "rifle", "milspec", 6.4),
  makeItem("M4A4", "Голограмма", "rifle", "classified", 145.0),
  makeItem("M4A4", "Император", "rifle", "classified", 210.0),
  makeItem("M4A1-S", "Дежавю", "rifle", "classified", 92.5),
  makeItem("M4A1-S", "Ночной охотник", "rifle", "covert", 320.0),
  makeItem("AWP", "Дракон Лор", "sniper", "covert", 2850.0),
  makeItem("AWP", "Кровопуск", "sniper", "covert", 620.0),
  makeItem("AWP", "Астро зона", "sniper", "classified", 88.0),
  makeItem("AWP", "Man-o-war", "sniper", "restricted", 45.0),
  makeItem("Desert Eagle", "Печать императора", "pistol", "classified", 178.0),
  makeItem("Desert Eagle", "Кобальтовая волна", "pistol", "restricted", 52.0),
  makeItem("Glock-18", "Градиент", "pistol", "restricted", 18.5),
  makeItem("Glock-18", "Веер", "pistol", "milspec", 9.2),
  makeItem("USP-S", "Убийца-неон", "pistol", "covert", 145.0),
  makeItem("USP-S", "Кодекс", "pistol", "classified", 41.0),
  makeItem("P250", "Азимов", "pistol", "restricted", 15.0),
  makeItem("Five-SeveN", "Мираж", "pistol", "milspec", 6.8),
  makeItem("MP7", "Скорость", "smg", "milspec", 4.2),
  makeItem("MP9", "Хайдра", "smg", "restricted", 12.4),
  makeItem("P90", "Азимов", "smg", "classified", 38.0),
  makeItem("UMP-45", "Праймер", "smg", "milspec", 5.6),
  makeItem("Nova", "Антрацит", "shotgun", "industrial", 2.1),
  makeItem("XM1014", "Огненная буря", "shotgun", "milspec", 7.4),
  makeItem("MAC-10", "Неоновый шторм", "smg", "restricted", 14.8),
  makeItem("Karambit", "Doppler", "knife", "gold", 1450.0, {
    statTrak: false,
  }),
  makeItem("Karambit", "Убийство Тигра", "knife", "gold", 2100.0),
  makeItem("Butterfly Knife", "Градиент", "knife", "gold", 1890.0),
  makeItem("Bayonet", "Мраморный градиент", "knife", "gold", 780.0),
  makeItem("M9 Bayonet", "Автотроника", "knife", "gold", 990.0),
  makeItem("Talon Knife", "Кровавая паутина", "knife", "gold", 1320.0),
  makeItem("Specialist Gloves", "Пандора", "gloves", "gold", 640.0),
  makeItem("Sport Gloves", "Пандора", "gloves", "gold", 720.0),
  makeItem("Driver Gloves", "Королевский синий", "gloves", "gold", 410.0),
  makeItem("P2000", "Аметист", "pistol", "milspec", 3.4),
  makeItem("Tec-9", "Пустынный ДВС", "pistol", "restricted", 11.2),
  makeItem("Galil AR", "Пустынный шторм", "rifle", "milspec", 3.9),
  makeItem("FAMAS", "Гелекс", "rifle", "restricted", 9.8),
  makeItem("SSG 08", "Крыло дракона", "sniper", "restricted", 16.5),
  makeItem("SG 553", "Пульс", "rifle", "classified", 34.0),
  makeItem("AUG", "Хаос", "rifle", "classified", 41.5),
  makeItem("CZ75-Auto", "Тигр", "pistol", "restricted", 8.9),
];

export function findItemsByRarity(rarity: Rarity): Item[] {
  return ITEMS.filter((i) => i.rarity === rarity);
}
