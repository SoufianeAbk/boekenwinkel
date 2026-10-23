import type { CSSProperties } from 'react'

interface Props {
  isbn: string
  titel: string
  auteurs: string[]
  className?: string
}

function hash(tekst: string): number {
  let h = 0
  for (const teken of tekst) h = (h * 31 + teken.charCodeAt(0)) >>> 0
  return h
}

/** We hebben geen afbeeldingen van de boeken: daarom tekenen we per ISBN een vaste, unieke cover. */
export function BoekCover({ isbn, titel, auteurs, className = '' }: Props) {
  const h = hash(isbn)
  const stijl = { '--tint': h % 360 } as CSSProperties
  return (
    <div className={`cover cover--v${h % 3} ${className}`} style={stijl} aria-hidden="true">
      <div className="cover__rug" />
      <div className="cover__inhoud">
        <div>
          <span className="cover__titel">{titel}</span>
          <div className="cover__lijn" />
        </div>
        <span className="cover__auteur">{auteurs[0]}</span>
      </div>
    </div>
  )
}
