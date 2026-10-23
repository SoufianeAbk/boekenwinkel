import { useState } from 'react'
import { Link, useNavigate } from 'react-router-dom'
import { reservatieApi } from '../api/gebruikers'
import type { Reservatie } from '../api/types'
import { Bericht } from '../components/Bericht'
import { BoekCover } from '../components/BoekCover'
import { useAuth } from '../context/AuthContext'
import { useMandje } from '../context/MandjeContext'
import { auteursTekst, formatPrijs } from '../util/formaat'

export function MandjePagina() {
  const { items, totaal, aantalBoeken, wijzigAantal, verwijder, leegmaken } = useMandje()
  const { gebruiker } = useAuth()
  const navigate = useNavigate()
  const [bezig, setBezig] = useState(false)
  const [fout, setFout] = useState<string | null>(null)
  const [geplaatst, setGeplaatst] = useState<Reservatie | null>(null)

  async function plaatsReservatie() {
    setBezig(true)
    setFout(null)
    try {
      const reservatie = await reservatieApi.reserveer(items.map(({ isbn, aantal }) => ({ isbn, aantal })))
      setGeplaatst(reservatie)
      leegmaken()
    } catch (e) {
      setFout((e as Error).message)
    } finally {
      setBezig(false)
    }
  }

  if (geplaatst) {
    return (
      <div className="kaart paneel" style={{ maxWidth: 640 }}>
        <h1>Reservatie #{geplaatst.id} is geplaatst</h1>
        <p>
          Bedankt! We leggen je boeken opzij. Zodra de winkel je reservatie bevestigt, krijg je een melding in je
          profiel met de datum waarop je ze kan afhalen en betalen.
        </p>
        <p className="gedempt">Te betalen bij afhaling: {formatPrijs(geplaatst.totaal)}</p>
        <div className="knoppenrij">
          <Link to="/profiel?tab=reservaties" className="knop knop--vol">
            Bekijk mijn reservaties
          </Link>
          <Link to="/" className="knop">
            Verder winkelen
          </Link>
        </div>
      </div>
    )
  }

  if (items.length === 0) {
    return (
      <div className="leeg">
        <h2>Je reservatie is nog leeg</h2>
        <p>Voeg boeken toe vanuit de catalogus om ze te reserveren.</p>
        <Link to="/" className="knop knop--vol">
          Naar de catalogus
        </Link>
      </div>
    )
  }

  return (
    <>
      <h1>Mijn reservatie</h1>
      {fout && <Bericht soort="fout">{fout}</Bericht>}
      <div className="twee-kolommen">
        <div className="kaart mandjelijst">
          {items.map((item) => (
            <div key={item.isbn} className="mandjelijn">
              <BoekCover isbn={item.isbn} titel={item.titel} auteurs={item.auteurs} />
              <div>
                <h3>
                  <Link to={`/boeken/${item.isbn}`}>{item.titel}</Link>
                </h3>
                <div className="gedempt klein">{auteursTekst(item.auteurs)}</div>
                <div className="mandjelijn__acties">
                  <div className="stapper" aria-label={`Aantal van ${item.titel}`}>
                    <button
                      type="button"
                      aria-label="Minder"
                      onClick={() => wijzigAantal(item.isbn, item.aantal - 1)}
                    >
                      −
                    </button>
                    <span>{item.aantal}</span>
                    <button
                      type="button"
                      aria-label="Meer"
                      disabled={item.aantal >= item.stock}
                      onClick={() => wijzigAantal(item.isbn, item.aantal + 1)}
                    >
                      +
                    </button>
                  </div>
                  <strong>{formatPrijs(item.prijs * item.aantal)}</strong>
                  <button type="button" className="knop knop--tekst knop--gevaar knop--klein" onClick={() => verwijder(item.isbn)}>
                    Verwijderen
                  </button>
                </div>
              </div>
            </div>
          ))}
        </div>

        <aside className="kaart samenvatting">
          <h2>Overzicht</h2>
          <div className="samenvatting__rij">
            <span>{aantalBoeken} {aantalBoeken === 1 ? 'boek' : 'boeken'}</span>
            <span>{formatPrijs(totaal)}</span>
          </div>
          <div className="samenvatting__rij samenvatting__totaal">
            <span>Te betalen bij afhaling</span>
            <span>{formatPrijs(totaal)}</span>
          </div>
          <ol className="stappen">
            <li>Je plaatst je reservatie online.</li>
            <li>De winkel bevestigt en je krijgt een melding met de afhaaldatum.</li>
            <li>Je haalt je boeken af en betaalt in de winkel.</li>
          </ol>
          {gebruiker ? (
            <button type="button" className="knop knop--vol knop--breed" disabled={bezig} onClick={plaatsReservatie}>
              {bezig ? 'Bezig met reserveren…' : 'Reservatie plaatsen'}
            </button>
          ) : (
            <button
              type="button"
              className="knop knop--vol knop--breed"
              onClick={() => navigate('/login', { state: { van: '/mandje' } })}
            >
              Log in om te reserveren
            </button>
          )}
        </aside>
      </div>
    </>
  )
}
