import { NavLink } from "react-router-dom";
import clsx from "clsx";
import { useStore } from "../store/StoreContext";

const NAV_LINKS = [
  { to: "/", label: "Кейсы" },
  { to: "/upgrade", label: "Апгрейд" },
  { to: "/contracts", label: "Контракты" },
  { to: "/bonuses", label: "Бонусы" },
  { to: "/wheel", label: "Колесо" },
  { to: "/missions", label: "Миссии" },
];

export function Navbar() {
  const { balance } = useStore();

  return (
    <header className="border-b border-white/5 bg-[#0d0d12]/90 backdrop-blur">
      <div className="mx-auto flex max-w-7xl items-center gap-6 px-4 py-3 lg:px-6">
        <a href="/" className="flex shrink-0 items-center gap-2">
          <span className="flex h-8 w-8 items-center justify-center rounded-lg bg-accent shadow-[0_0_18px_rgba(255,61,90,0.5)]">
            <svg viewBox="0 0 32 32" className="h-5 w-5">
              <path d="M16 4 L27 10 V22 L16 28 L5 22 V10 Z" fill="none" stroke="#0a0a0e" strokeWidth="2.4" strokeLinejoin="round" />
              <circle cx="16" cy="16" r="3.4" fill="#0a0a0e" />
            </svg>
          </span>
          <span className="text-lg font-extrabold tracking-tight text-white">
            RAGE<span className="text-accent">DROP</span>
          </span>
        </a>

        <nav className="no-scrollbar hidden flex-1 items-center gap-1 overflow-x-auto lg:flex">
          {NAV_LINKS.map((link) => (
            <NavLink
              key={link.to}
              to={link.to}
              className={({ isActive }) =>
                clsx(
                  "shrink-0 rounded-lg px-3 py-2 text-sm font-semibold transition-colors",
                  isActive ? "bg-white/5 text-white" : "text-text-muted hover:text-white"
                )
              }
            >
              {link.label}
            </NavLink>
          ))}
        </nav>

        <div className="ml-auto flex shrink-0 items-center gap-3">
          <NavLink
            to="/admin"
            title="Админ-панель"
            className="hidden h-9 w-9 items-center justify-center rounded-lg bg-surface-2 text-base text-text-muted ring-1 ring-white/5 hover:text-white hover:ring-accent/60 sm:flex"
          >
            ⚙
          </NavLink>
          <div className="flex items-center gap-2 rounded-lg border border-white/5 bg-surface px-3 py-1.5">
            <span className="h-2 w-2 rounded-full bg-success shadow-[0_0_8px_#3ecf5e]" />
            <span className="text-sm font-bold text-white">
              ${balance.toLocaleString("en-US", { minimumFractionDigits: 2, maximumFractionDigits: 2 })}
            </span>
          </div>
          <NavLink
            to="/profile"
            className="flex h-9 w-9 items-center justify-center rounded-lg bg-surface-2 text-sm font-bold text-white ring-1 ring-white/5 hover:ring-accent/60"
          >
            Я
          </NavLink>
        </div>
      </div>
    </header>
  );
}
