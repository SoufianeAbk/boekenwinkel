import type { ReactNode } from 'react'
import type { Reservatie, ReservatieStatus } from '../api/types'
import { formatDatum, formatDatumTijd, formatPrijs } from '../util/formaat'

const STATUS: Record<ReservatieStatus, { label: string; klasse: string }> = {
  GERESERVEERD: { label: 'Wacht op bevestiging', klasse: 'badge--warn' },
  BEVESTIGD: { label: 'Klaar om af te halen', klasse: 'badge--ok' },
  GEANNULEERD: { label: 'Geannuleerd', klasse: 'badge--neutraal' },
}

interface Props {
  reservatie: Reservatie
  toonKlant?: boolean
  children?: ReactNode
}

export function ReservatieKaart({ reservatie, toonKlant = false, children }: Props) {
  const status = STATUS[reservatie.status]
  return (
    <article className="kaart reservatie">
      <div className="reservatie__kop">
        <div>
          <h3>Reservatie #{reservatie.id}</h3>
          <div className="gedempt klein">
            {toonKlant && (
              <>
                {reservatie.klant.naam} ({reservatie.klant.gebruikersnaam}) ·{' '}
              </>
            )}
            geplaatst op {formatDatumTijd(reservatie.aangemaaktOp)}
          </div>
        </div>
        <span className={`badge ${status.klasse}`}>{status.label}</span>
      </div>

      <table className="reservatie__lijnen">
        <tbody>
          {reservatie.lijnen.map((lijn) => (
            <tr key={lijn.isbn}>
              <td>
                {lijn.aantal} × {lijn.titel}
                <div className="gedempt klein">{formatPrijs(lijn.prijs)} per stuk</div>
              </td>
              <td>{formatPrijs(lijn.subtotaal)}</td>
            </tr>
          ))}
          <tr className="reservatie__totaal">
            <td>Te betalen bij afhaling</td>
            <td>{formatPrijs(reservatie.totaal)}</td>
          </tr>
        </tbody>
      </table>

      {reservatie.status === 'BEVESTIGD' && reservatie.afhaalDatum && (
        <div className="reservatie__afhalen">
          Afhalen en betalen in de winkel vanaf {formatDatum(reservatie.afhaalDatum)}
        </div>
      )}

      {children && <div className="reservatie__acties">{children}</div>}
    </article>
  )
}
