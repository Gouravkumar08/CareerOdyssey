type DiffStat = { total: number; solved: number }

type DsaProgressRingProps = {
  solved: number
  total: number
  easy: DiffStat
  medium: DiffStat
  hard: DiffStat
}

const SIZE = 160
const CENTER = SIZE / 2
const RADIUS = 62
const STROKE = 13
const GAP_DEG = 5

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
 * A ring split into an Easy/Medium/Hard segment per difficulty (arc length proportional to how
 * many questions of that difficulty exist), with the solved fraction of each segment drawn as a
 * brighter, glowing overlay on top of a dim full-segment track.
 */
export function DsaProgressRing({ solved, total, easy, medium, hard }: DsaProgressRingProps) {
  const segments = [
    { key: 'easy', color: '#22c55e', ...easy },
    { key: 'medium', color: '#f59e0b', ...medium },
    { key: 'hard', color: '#ef4444', ...hard },
  ]
  const segTotal = segments.reduce((sum, seg) => sum + seg.total, 0)

  let cursor = 0
  const gapTotal = GAP_DEG * segments.filter((s) => s.total > 0).length
  const arcs = segments.map((seg) => {
    if (seg.total === 0 || segTotal === 0) {
      return { ...seg, start: cursor, end: cursor, solvedEnd: cursor }
    }
    const span = (seg.total / segTotal) * (360 - gapTotal)
    const start = cursor
    const end = cursor + span
    cursor = end + GAP_DEG
    const solvedSpan = (seg.solved / seg.total) * span
    return { ...seg, start, end, solvedEnd: start + solvedSpan }
  })

  return (
    <div className="dsa-progress-ring-wrap">
      <svg width={SIZE} height={SIZE} viewBox={`0 0 ${SIZE} ${SIZE}`}>
        {arcs.map((arc) => (
          <g key={arc.key}>
            {arc.end > arc.start && (
              <path
                d={describeArc(CENTER, CENTER, RADIUS, arc.start, arc.end)}
                stroke={arc.color}
                strokeOpacity={0.22}
                strokeWidth={STROKE}
                strokeLinecap="round"
                fill="none"
              />
            )}
            {arc.solvedEnd > arc.start && (
              <path
                d={describeArc(CENTER, CENTER, RADIUS, arc.start, arc.solvedEnd)}
                stroke={arc.color}
                strokeWidth={STROKE}
                strokeLinecap="round"
                fill="none"
                style={{ filter: `drop-shadow(0 0 4px ${arc.color})` }}
              />
            )}
          </g>
        ))}
      </svg>
      <div className="dsa-progress-ring-value">
        <span className="big">{solved}</span>
        <span className="small">{total}</span>
      </div>
    </div>
  )
}
