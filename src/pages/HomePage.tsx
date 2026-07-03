import { Link } from "react-router-dom";
import { useCases } from "../store/CasesContext";
import { CaseCard } from "../components/CaseCard";

export function HomePage() {
  const { cases } = useCases();
  const featured = cases.filter((c) => c.featured);

  return (
    <div className="py-8">
      <section className="relative overflow-hidden rounded-2xl border border-white/5 bg-surface px-6 py-12 sm:px-10 sm:py-16">
        <div className="absolute -right-24 -top-24 h-72 w-72 rounded-full bg-accent/20 blur-3xl" />
        <div className="absolute -bottom-32 -left-16 h-72 w-72 rounded-full bg-[#8847ff]/15 blur-3xl" />
        <div className="relative max-w-xl">
          <p className="mb-3 inline-block rounded-full border border-accent/30 bg-accent/10 px-3 py-1 text-xs font-bold uppercase tracking-wide text-accent">
            CS2 Case Opening
          </p>
          <h1 className="text-4xl font-extrabold leading-tight text-white sm:text-5xl">
            Открывай кейсы.
            <br />
            Получай легендарные скины.
          </h1>
          <p className="mt-4 text-text-muted">
            Демо-платформа для открытия кейсов CS2 с апгрейдом предметов и инвентарём. Виртуальный баланс,
            никаких реальных ставок.
          </p>
          <div className="mt-6 flex gap-3">
            <a
              href="#all-cases"
              className="rounded-lg bg-accent px-5 py-3 text-sm font-bold text-white shadow-[0_0_24px_rgba(255,61,90,0.45)] transition-transform hover:scale-[1.03]"
            >
              Открыть кейс
            </a>
            <Link
              to="/upgrade"
              className="rounded-lg border border-white/10 bg-surface-2 px-5 py-3 text-sm font-bold text-white transition-colors hover:border-white/20"
            >
              Апгрейд скинов
            </Link>
          </div>
        </div>
      </section>

      {featured.length > 0 && (
        <section className="mt-10">
          <div className="mb-4 flex items-center gap-2">
            <span className="text-lg">👑</span>
            <h2 className="text-lg font-bold text-white">Популярные кейсы</h2>
          </div>
          <div className="grid grid-cols-2 gap-4 sm:grid-cols-3 lg:grid-cols-4">
            {featured.map((c) => (
              <Link key={c.id} to={`/case/${c.id}`}>
                <CaseCard data={c} />
              </Link>
            ))}
          </div>
        </section>
      )}

      <section id="all-cases" className="mt-10 scroll-mt-20">
        <div className="mb-4 flex items-center gap-2">
          <span className="text-lg">🎁</span>
          <h2 className="text-lg font-bold text-white">Все кейсы</h2>
        </div>
        <div className="grid grid-cols-2 gap-4 sm:grid-cols-3 lg:grid-cols-4">
          {cases.map((c) => (
            <Link key={c.id} to={`/case/${c.id}`}>
              <CaseCard data={c} />
            </Link>
          ))}
        </div>
      </section>
    </div>
  );
}
