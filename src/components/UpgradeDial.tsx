import { motion } from "framer-motion";

interface Props {
  chance: number;
  rotation: number;
  spinning: boolean;
  result: "idle" | "win" | "lose";
}

const SIZE = 220;
const RADIUS = 92;
const CIRC = 2 * Math.PI * RADIUS;

export function UpgradeDial({ chance, rotation, spinning, result }: Props) {
  const winLen = (chance / 100) * CIRC;
  const color = result === "win" ? "#3ecf5e" : result === "lose" ? "#5c5c6b" : "#ff3d5a";

  return (
    <div className="relative flex h-56 w-56 items-center justify-center">
      <svg width={SIZE} height={SIZE} viewBox={`0 0 ${SIZE} ${SIZE}`} className="-rotate-90">
        <circle
          cx={SIZE / 2}
          cy={SIZE / 2}
          r={RADIUS}
          fill="none"
          stroke="#1a1a22"
          strokeWidth={10}
        />
        <circle
          cx={SIZE / 2}
          cy={SIZE / 2}
          r={RADIUS}
          fill="none"
          stroke={color}
          strokeWidth={10}
          strokeLinecap="round"
          strokeDasharray={`${winLen} ${CIRC - winLen}`}
          style={{ filter: `drop-shadow(0 0 8px ${color})`, transition: "stroke 0.3s" }}
        />
      </svg>

      <motion.div
        className="absolute left-1/2 top-1/2 h-[86px] w-1 origin-bottom rounded-full bg-white"
        style={{ marginLeft: -2, marginTop: -86 }}
        animate={{ rotate: rotation }}
        transition={
          spinning
            ? { duration: 3.2, ease: [0.15, 0.7, 0.2, 1] }
            : { duration: 0 }
        }
      />
      <div className="absolute h-3 w-3 rounded-full bg-white shadow" />

      <div className="absolute flex flex-col items-center text-center">
        <p className="text-xs font-bold uppercase tracking-widest text-white">Апгрейд</p>
        <p className="mt-1 max-w-[120px] text-[10px] leading-tight text-text-faint">
          Шанс на успешный апгрейд предмета
        </p>
        <p className="mt-2 text-3xl font-extrabold text-white">
          {result === "idle" ? `${chance.toFixed(1)}%` : result === "win" ? "УСПЕХ" : "МИМО"}
        </p>
      </div>
    </div>
  );
}
