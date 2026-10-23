import { useState } from 'react'

interface SterrenProps {
  score: number
  groot?: boolean
}

/** Toont een score van 0 tot 5 als sterren (ook halve sterren). */
export function Sterren({ score, groot = false }: SterrenProps) {
  const procent = Math.max(0, Math.min(100, (score / 5) * 100))
  const label = `${score.toLocaleString('nl-BE', { maximumFractionDigits: 1 })} van 5 sterren`
  return (
    <span className={`sterren ${groot ? 'sterren--groot' : ''}`} role="img" aria-label={label}>
      <span className="sterren__leeg">★★★★★</span>
      <span className="sterren__vol" style={{ width: `${procent}%` }}>
        ★★★★★
      </span>
    </span>
  )
}

interface InvoerProps {
  waarde: number
  onChange: (score: number) => void
}

const OMSCHRIJVING = ['', 'Slecht', 'Matig', 'Goed', 'Heel goed', 'Uitstekend']

export function SterrenInvoer({ waarde, onChange }: InvoerProps) {
  const [hover, setHover] = useState(0)
  const getoond = hover || waarde
  return (
    <div style={{ display: 'flex', alignItems: 'center', gap: 10 }}>
      <div className="sterren-invoer" role="radiogroup" aria-label="Score" onMouseLeave={() => setHover(0)}>
        {[1, 2, 3, 4, 5].map((score) => (
          <button
            key={score}
            type="button"
            role="radio"
            aria-checked={waarde === score}
            aria-label={`${score} ${score === 1 ? 'ster' : 'sterren'}`}
            className={score <= getoond ? 'aan' : ''}
            onMouseEnter={() => setHover(score)}
            onClick={() => onChange(score)}
          >
            ★
          </button>
        ))}
      </div>
      <span className="gedempt klein">{OMSCHRIJVING[getoond]}</span>
    </div>
  )
}
