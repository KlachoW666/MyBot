import { Link } from "react-router-dom";
import { useCases } from "../store/CasesContext";
import { CaseCard } from "./CaseCard";

export function TopCasesStrip() {
  const { cases } = useCases();

  return (
    <div className="no-scrollbar flex gap-3 overflow-x-auto px-4 pb-4 lg:px-6">
      {cases.map((c) => (
        <Link key={c.id} to={`/case/${c.id}`} className="shrink-0">
          <CaseCard data={c} size="sm" />
        </Link>
      ))}
    </div>
  );
}
