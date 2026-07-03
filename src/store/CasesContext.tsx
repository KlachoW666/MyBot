import { createContext, useContext, useEffect, useState, type ReactNode } from "react";
import type { CaseData, CasePool, Item, Rarity } from "../types";
import { CASES as DEFAULT_CASES } from "../data/cases";
import { ITEMS as DEFAULT_ITEMS } from "../data/items";

const CASES_KEY = "ragedrop_cases_v1";
const ITEMS_KEY = "ragedrop_custom_items_v1";

interface CasesState {
  cases: CaseData[];
  itemsCatalog: Item[];
  addCase: (input: { name: string; price: number; accent: Rarity; featured: boolean }) => CaseData;
  updateCase: (id: string, patch: Partial<Pick<CaseData, "name" | "price" | "accent" | "featured">>) => void;
  deleteCase: (id: string) => void;
  addPoolEntry: (caseId: string, item: Item, weight: number) => void;
  updatePoolWeight: (caseId: string, itemId: string, weight: number) => void;
  removePoolEntry: (caseId: string, itemId: string) => void;
  createCustomItem: (input: Omit<Item, "id">) => Item;
}

const CasesContext = createContext<CasesState | null>(null);

function loadInitial<T>(key: string, fallback: T): T {
  try {
    const raw = localStorage.getItem(key);
    if (raw) return JSON.parse(raw) as T;
  } catch {
    // ignore malformed storage
  }
  return fallback;
}

export function CasesProvider({ children }: { children: ReactNode }) {
  const [cases, setCases] = useState<CaseData[]>(() => loadInitial(CASES_KEY, DEFAULT_CASES));
  const [customItems, setCustomItems] = useState<Item[]>(() => loadInitial(ITEMS_KEY, []));

  useEffect(() => {
    localStorage.setItem(CASES_KEY, JSON.stringify(cases));
  }, [cases]);

  useEffect(() => {
    localStorage.setItem(ITEMS_KEY, JSON.stringify(customItems));
  }, [customItems]);

  const itemsCatalog = [...DEFAULT_ITEMS, ...customItems];

  function addCase(input: { name: string; price: number; accent: Rarity; featured: boolean }) {
    const newCase: CaseData = {
      id: `case-${Date.now()}-${Math.random().toString(36).slice(2, 7)}`,
      name: input.name,
      price: input.price,
      accent: input.accent,
      featured: input.featured,
      pool: [],
    };
    setCases((prev) => [...prev, newCase]);
    return newCase;
  }

  function updateCase(id: string, patch: Partial<Pick<CaseData, "name" | "price" | "accent" | "featured">>) {
    setCases((prev) => prev.map((c) => (c.id === id ? { ...c, ...patch } : c)));
  }

  function deleteCase(id: string) {
    setCases((prev) => prev.filter((c) => c.id !== id));
  }

  function addPoolEntry(caseId: string, item: Item, weight: number) {
    setCases((prev) =>
      prev.map((c) => {
        if (c.id !== caseId) return c;
        if (c.pool.some((p) => p.item.id === item.id)) return c;
        const pool: CasePool[] = [...c.pool, { item, weight }];
        return { ...c, pool };
      })
    );
  }

  function updatePoolWeight(caseId: string, itemId: string, weight: number) {
    setCases((prev) =>
      prev.map((c) => {
        if (c.id !== caseId) return c;
        return { ...c, pool: c.pool.map((p) => (p.item.id === itemId ? { ...p, weight } : p)) };
      })
    );
  }

  function removePoolEntry(caseId: string, itemId: string) {
    setCases((prev) =>
      prev.map((c) => {
        if (c.id !== caseId) return c;
        return { ...c, pool: c.pool.filter((p) => p.item.id !== itemId) };
      })
    );
  }

  function createCustomItem(input: Omit<Item, "id">) {
    const item: Item = { ...input, id: `custom-${Date.now()}-${Math.random().toString(36).slice(2, 7)}` };
    setCustomItems((prev) => [...prev, item]);
    return item;
  }

  return (
    <CasesContext.Provider
      value={{
        cases,
        itemsCatalog,
        addCase,
        updateCase,
        deleteCase,
        addPoolEntry,
        updatePoolWeight,
        removePoolEntry,
        createCustomItem,
      }}
    >
      {children}
    </CasesContext.Provider>
  );
}

export function useCases() {
  const ctx = useContext(CasesContext);
  if (!ctx) throw new Error("useCases must be used within CasesProvider");
  return ctx;
}
