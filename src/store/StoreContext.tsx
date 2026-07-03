import { createContext, useCallback, useContext, useMemo, useState, type ReactNode } from "react";
import type { InventoryItem, Item } from "../types";

interface StoreState {
  balance: number;
  inventory: InventoryItem[];
  casesOpened: number;
  upgradesPlayed: number;
  addFunds: (amount: number) => void;
  spend: (amount: number) => boolean;
  addToInventory: (item: Item) => InventoryItem;
  removeFromInventory: (uid: string) => void;
  sellItem: (uid: string) => void;
  sellAll: () => void;
  incrementCasesOpened: () => void;
  incrementUpgradesPlayed: () => void;
}

const StoreContext = createContext<StoreState | null>(null);

const STARTING_BALANCE = 2854.0;

export function StoreProvider({ children }: { children: ReactNode }) {
  const [balance, setBalance] = useState(STARTING_BALANCE);
  const [inventory, setInventory] = useState<InventoryItem[]>([]);
  const [casesOpened, setCasesOpened] = useState(0);
  const [upgradesPlayed, setUpgradesPlayed] = useState(0);

  const addFunds = useCallback((amount: number) => {
    setBalance((b) => b + amount);
  }, []);

  const spend = useCallback(
    (amount: number) => {
      let ok = false;
      setBalance((b) => {
        if (b >= amount) {
          ok = true;
          return b - amount;
        }
        return b;
      });
      return ok;
    },
    []
  );

  const addToInventory = useCallback((item: Item) => {
    const entry: InventoryItem = {
      ...item,
      uid: `${item.id}-${Date.now()}-${Math.random().toString(36).slice(2, 8)}`,
      obtainedAt: Date.now(),
    };
    setInventory((inv) => [entry, ...inv]);
    return entry;
  }, []);

  const removeFromInventory = useCallback((uid: string) => {
    setInventory((inv) => inv.filter((i) => i.uid !== uid));
  }, []);

  const sellItem = useCallback((uid: string) => {
    setInventory((inv) => {
      const target = inv.find((i) => i.uid === uid);
      if (target) setBalance((b) => b + target.price);
      return inv.filter((i) => i.uid !== uid);
    });
  }, []);

  const sellAll = useCallback(() => {
    setInventory((inv) => {
      const total = inv.reduce((sum, i) => sum + i.price, 0);
      setBalance((b) => b + total);
      return [];
    });
  }, []);

  const incrementCasesOpened = useCallback(() => setCasesOpened((c) => c + 1), []);
  const incrementUpgradesPlayed = useCallback(() => setUpgradesPlayed((c) => c + 1), []);

  const value = useMemo(
    () => ({
      balance,
      inventory,
      casesOpened,
      upgradesPlayed,
      addFunds,
      spend,
      addToInventory,
      removeFromInventory,
      sellItem,
      sellAll,
      incrementCasesOpened,
      incrementUpgradesPlayed,
    }),
    [
      balance,
      inventory,
      casesOpened,
      upgradesPlayed,
      addFunds,
      spend,
      addToInventory,
      removeFromInventory,
      sellItem,
      sellAll,
      incrementCasesOpened,
      incrementUpgradesPlayed,
    ]
  );

  return <StoreContext.Provider value={value}>{children}</StoreContext.Provider>;
}

export function useStore() {
  const ctx = useContext(StoreContext);
  if (!ctx) throw new Error("useStore must be used within StoreProvider");
  return ctx;
}
