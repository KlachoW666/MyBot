import type { WeaponCategory } from "../types";

interface Props {
  category: WeaponCategory;
  className?: string;
}

/**
 * Stylised line-art silhouettes standing in for real skin renders.
 * Kept as simple geometric shapes so every category reads at a glance.
 */
export function WeaponIcon({ category, className }: Props) {
  const common = {
    className,
    viewBox: "0 0 200 90",
    xmlns: "http://www.w3.org/2000/svg",
    fill: "none",
    stroke: "currentColor",
    strokeWidth: 3,
    strokeLinejoin: "round" as const,
    strokeLinecap: "round" as const,
  };

  switch (category) {
    case "rifle":
      return (
        <svg {...common}>
          <path d="M20 55 h30 l8 -14 h60 l10 10 h30 v14 h-14 v10 h-10 v-10 h-84 l-6 10 h-16 z" />
          <path d="M58 41 v-16 h34 v16" />
          <path d="M100 55 v18" />
          <rect x="94" y="59" width="14" height="22" rx="3" />
        </svg>
      );
    case "sniper":
      return (
        <svg {...common}>
          <path d="M10 58 h50 l10 -10 h100 l16 8 v10 h-16 v8 h-10 v-8 h-96 l-8 8 h-10 l-8 -8 h-28 z" />
          <path d="M70 48 v-20 h30 v20" />
          <path d="M120 40 h30" />
          <rect x="66" y="58" width="12" height="20" rx="3" />
        </svg>
      );
    case "smg":
      return (
        <svg {...common}>
          <path d="M24 52 h40 l6 -10 h56 l14 8 v12 h-12 v8 h-10 v-8 h-70 l-8 8 h-14 z" />
          <rect x="60" y="52" width="12" height="24" rx="3" />
          <path d="M120 44 l16 -10" />
        </svg>
      );
    case "pistol":
      return (
        <svg {...common}>
          <path d="M30 45 h70 l14 -8 h30 v16 h-14 v6 h-10 v-6 h-64 l-6 6 h-10 z" />
          <rect x="46" y="45" width="14" height="28" rx="3" />
        </svg>
      );
    case "shotgun":
      return (
        <svg {...common}>
          <path d="M14 55 h150 l16 -6 v14 h-14 v6 h-10 v-6 h-118 l-8 8 h-16 z" />
          <path d="M60 55 v-14 h60 v14" />
        </svg>
      );
    case "knife":
      return (
        <svg {...common}>
          <path d="M150 20 L60 55 L96 63 L165 40 Z" />
          <path d="M96 63 L48 78 L38 68 L84 55" />
        </svg>
      );
    case "gloves":
      return (
        <svg {...common}>
          <path d="M60 70 V38 q0 -8 8 -8 t8 8 v14 M76 44 V28 q0 -8 8 -8 t8 8 v22 M92 44 V26 q0 -8 8 -8 t8 8 v24 M108 46 V32 q0 -7 7 -7 t7 7 v18" />
          <path d="M60 70 q-12 0 -12 14 v0 q0 10 12 10 h64 q14 0 14 -14 v-16" />
        </svg>
      );
    default:
      return <svg {...common} />;
  }
}
