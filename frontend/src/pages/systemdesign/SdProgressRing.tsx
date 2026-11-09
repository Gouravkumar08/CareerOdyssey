type SdProgressRingProps = {
  completed: number
  total: number
}

const SIZE = 160
const CENTER = SIZE / 2
const RADIUS = 62
const STROKE = 13

function polarToCartesian(cx: number, cy: number, r: number, angleDeg: number) {
  const rad = ((angleDeg - 90) * Math.PI) / 180
  return { x: cx + r * Math.cos(rad), y: cy + r * Math.sin(rad) }
}

function describeArc(cx: number, cy: number, r: number, startAngle: number, endAngle: number) {
  const clampedEnd = endAngle - startAngle >= 359.99 ? startAngle + 359.99 : endAngle
  const start = polarToCartesian(cx, cy, r, clampedEnd)
  const end = polarToCartesian(cx, cy, r, startAngle)
  const largeArcFlag = clampedEnd - startAngle <= 180 ? 0 : 1
  return `M ${start.x} ${start.y} A ${r} ${r} 0 ${largeArcFlag} 0 ${end.x} ${end.y}`
}

/**
 * A single-arc ring showing overall completed/total progress for a System Design sheet — the
 * simpler counterpart to DsaProgressRing, which has no difficulty-style breakdown to segment on.
 */
export function SdProgressRing({ completed, total }: SdProgressRingProps) {
  const color = '#6d8cf0'
  const completedEnd = total > 0 ? (completed / total) * 359.99 : 0

  return (
    <div className="dsa-progress-ring-wrap">
      <svg width={SIZE} height={SIZE} viewBox={`0 0 ${SIZE} ${SIZE}`}>
        <path
          d={describeArc(CENTER, CENTER, RADIUS, 0, 359.99)}
          stroke={color}
          strokeOpacity={0.22}
          strokeWidth={STROKE}
          strokeLinecap="round"
          fill="none"
        />
        {completedEnd > 0 && (
          <path
            d={describeArc(CENTER, CENTER, RADIUS, 0, completedEnd)}
            stroke={color}
            strokeWidth={STROKE}
            strokeLinecap="round"
            fill="none"
            style={{ filter: `drop-shadow(0 0 4px ${color})` }}
          />
        )}
      </svg>
      <div className="dsa-progress-ring-value">
        <span className="big">{completed}</span>
        <span className="small">{total}</span>
      </div>
    </div>
  )
}
