import { useState, type FormEvent } from 'react'
import { Link, useSearchParams } from 'react-router-dom'
import { catalogusApi } from '../api/catalogus'
import { beheerApi } from '../api/gebruikers'
import type { Boek, Reservatie, ReservatieStatus } from '../api/types'
import { Bericht } from '../components/Bericht'
import { ReservatieKaart } from '../components/ReservatieKaart'
import { StockBadge } from '../components/StockBadge'
import { auteursTekst, datumVanafVandaag, formatDatum, formatPrijs } from '../util/formaat'
import { useData } from '../util/useData'

export function BeheerPagina() {
  const [params, setParams] = useSearchParams()
  const tab = params.get('tab') === 'catalogus' ? 'catalogus' : 'reservaties'

  return (
    <>
      <h1>Winkelbeheer</h1>
      <p className="gedempt">Bevestig reservaties van klanten en houd de stock van de catalogus bij.</p>
      <div className="tabs" role="tablist">
        <button type="button" role="tab" aria-selected={tab === 'reservaties'} onClick={() => setParams({}, { replace: true })}>
          Reservaties
        </button>
        <button
          type="button"
          role="tab"
          aria-selected={tab === 'catalogus'}
          onClick={() => setParams({ tab: 'catalogus' }, { replace: true })}
        >
          Catalogus en stock
        </button>
      </div>
      {tab === 'reservaties' ? <ReservatiesBeheer /> : <CatalogusBeheer />}
    </>
  )
}

const FILTERS: { waarde: ReservatieStatus | ''; label: string }[] = [
  { waarde: 'GERESERVEERD', label: 'Te bevestigen' },
  { waarde: 'BEVESTIGD', label: 'Bevestigd' },
  { waarde: 'GEANNULEERD', label: 'Geannuleerd' },
  { waarde: '', label: 'Alle' },
]

function ReservatiesBeheer() {
  const [status, setStatus] = useState<ReservatieStatus | ''>('GERESERVEERD')
  const { data: reservaties, setData, fout, laden } = useData(() => beheerApi.reservaties(status || undefined), [status])
  const [succes, setSucces] = useState<string | null>(null)

  function bijBevestigd(bevestigd: Reservatie) {
    setSucces(
      `Reservatie #${bevestigd.id} is bevestigd. ${bevestigd.klant.naam} kreeg een melding om de boeken vanaf ` +
        `${formatDatum(bevestigd.afhaalDatum!)} af te halen, en de stock in de catalogus is verminderd.`,
    )
    setData((huidig) =>
      status === 'GERESERVEERD'
        ? (huidig?.filter((r) => r.id !== bevestigd.id) ?? null)
        : (huidig?.map((r) => (r.id === bevestigd.id ? bevestigd : r)) ?? null),
    )
  }

  return (
    <section>
      <div className="sectiekop">
        <h2>Reservaties</h2>
        <div className="veld" style={{ margin: 0 }}>
          <label htmlFor="status-filter" className="visueel-verborgen">
            Toon reservaties
          </label>
          <select id="status-filter" value={status} onChange={(e) => setStatus(e.target.value as ReservatieStatus | '')}>
            {FILTERS.map((f) => (
              <option key={f.label} value={f.waarde}>
                {f.label}
              </option>
            ))}
          </select>
        </div>
      </div>
      {succes && <Bericht soort="succes">{succes}</Bericht>}
      {fout && <Bericht soort="fout">{fout}</Bericht>}
      {laden && !reservaties && <p className="gedempt">Laden…</p>}
      {reservaties && reservaties.length === 0 && (
        <div className="kaart paneel gedempt">
          {status === 'GERESERVEERD' ? 'Er zijn geen reservaties die op bevestiging wachten.' : 'Geen reservaties gevonden.'}
        </div>
      )}
      <div className="lijst">
        {reservaties?.map((reservatie) => (
          <ReservatieKaart key={reservatie.id} reservatie={reservatie} toonKlant>
            {reservatie.status === 'GERESERVEERD' && (
              <BevestigFormulier reservatie={reservatie} onBevestigd={bijBevestigd} />
            )}
          </ReservatieKaart>
        ))}
      </div>
    </section>
  )
}

function BevestigFormulier({ reservatie, onBevestigd }: { reservatie: Reservatie; onBevestigd: (r: Reservatie) => void }) {
  const [afhaalDatum, setAfhaalDatum] = useState(datumVanafVandaag(1))
  const [fout, setFout] = useState<string | null>(null)
  const [bezig, setBezig] = useState(false)

  async function bevestig(e: FormEvent) {
    e.preventDefault()
    setBezig(true)
    setFout(null)
    try {
      onBevestigd(await beheerApi.bevestig(reservatie.id, afhaalDatum))
    } catch (err) {
      setFout((err as Error).message)
      setBezig(false)
    }
  }

  return (
    <form onSubmit={bevestig} style={{ display: 'contents' }}>
      {fout && (
        <div style={{ flexBasis: '100%' }}>
          <Bericht soort="fout">{fout}</Bericht>
        </div>
      )}
      <div className="veld">
        <label htmlFor={`afhaal-${reservatie.id}`}>Afhalen vanaf</label>
        <input
          id={`afhaal-${reservatie.id}`}
          type="date"
          required
          min={datumVanafVandaag(0)}
          value={afhaalDatum}
          onChange={(e) => setAfhaalDatum(e.target.value)}
        />
      </div>
      <button type="submit" className="knop knop--vol" disabled={bezig}>
        {bezig ? 'Bezig…' : 'Bevestigen en klant verwittigen'}
      </button>
    </form>
  )
}

function CatalogusBeheer() {
  const { data: boeken, setData, fout, laden } = useData(() => catalogusApi.zoekBoeken(), [])
  const [succes, setSucces] = useState<string | null>(null)
  const [formulierOpen, setFormulierOpen] = useState(false)

  const werkBij = (boek: Boek) => setData((huidig) => huidig?.map((b) => (b.isbn === boek.isbn ? boek : b)) ?? null)

  return (
    <section>
      <div className="sectiekop">
        <h2>Catalogus en stock</h2>
        <button type="button" className="knop knop--klein" onClick={() => setFormulierOpen((open) => !open)}>
          {formulierOpen ? 'Formulier sluiten' : 'Nieuw boek toevoegen'}
        </button>
      </div>
      {succes && <Bericht soort="succes">{succes}</Bericht>}
      {fout && <Bericht soort="fout">{fout}</Bericht>}

      {formulierOpen && (
        <NieuwBoekFormulier
          onToegevoegd={(boek) => {
            setData((huidig) => [...(huidig ?? []), boek].sort((a, b) => a.titel.localeCompare(b.titel)))
            setSucces(`'${boek.titel}' is toegevoegd aan de catalogus.`)
            setFormulierOpen(false)
          }}
        />
      )}

      {laden && !boeken && <p className="gedempt">Laden…</p>}
      {boeken && (
        <div className="kaart tabel-wrapper">
          <table className="tabel tabel--stapel">
            <thead>
              <tr>
                <th>Boek</th>
                <th>Prijs</th>
                <th>Stock</th>
                <th>Levering registreren</th>
              </tr>
            </thead>
            <tbody>
              {boeken.map((boek) => (
                <tr key={boek.isbn}>
                  <td>
                    <Link to={`/boeken/${boek.isbn}`}>
                      <strong>{boek.titel}</strong>
                    </Link>
                    <div className="gedempt klein">
                      {auteursTekst(boek.auteurs)} · {boek.isbn}
                    </div>
                  </td>
                  <td>{formatPrijs(boek.prijs)}</td>
                  <td>
                    <StockBadge stock={boek.stock} />
                  </td>
                  <td>
                    <LeveringFormulier
                      boek={boek}
                      onGeregistreerd={(bijgewerkt, aantal) => {
                        werkBij(bijgewerkt)
                        setSucces(`Levering van ${aantal} × '${bijgewerkt.titel}' geregistreerd. Nieuwe stock: ${bijgewerkt.stock}.`)
                      }}
                    />
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      )}
    </section>
  )
}

function LeveringFormulier({ boek, onGeregistreerd }: { boek: Boek; onGeregistreerd: (b: Boek, aantal: number) => void }) {
  const [aantal, setAantal] = useState(5)
  const [fout, setFout] = useState<string | null>(null)
  const [bezig, setBezig] = useState(false)

  async function registreer(e: FormEvent) {
    e.preventDefault()
    setBezig(true)
    setFout(null)
    try {
      onGeregistreerd(await catalogusApi.registreerLevering(boek.isbn, aantal), aantal)
    } catch (err) {
      setFout((err as Error).message)
    } finally {
      setBezig(false)
    }
  }

  return (
    <form onSubmit={registreer}>
      <div className="levering">
        <label htmlFor={`levering-${boek.isbn}`} className="visueel-verborgen">
          Aantal exemplaren voor {boek.titel}
        </label>
        <input
          id={`levering-${boek.isbn}`}
          type="number"
          min={1}
          max={10000}
          required
          value={aantal}
          onChange={(e) => setAantal(Number(e.target.value))}
        />
        <button type="submit" className="knop knop--klein" disabled={bezig}>
          + Toevoegen
        </button>
      </div>
      {fout && <div className="klein" style={{ color: 'var(--danger)', marginTop: 4 }}>{fout}</div>}
    </form>
  )
}

function NieuwBoekFormulier({ onToegevoegd }: { onToegevoegd: (boek: Boek) => void }) {
  const [isbn, setIsbn] = useState('')
  const [titel, setTitel] = useState('')
  const [auteurs, setAuteurs] = useState('')
  const [omschrijving, setOmschrijving] = useState('')
  const [prijs, setPrijs] = useState('')
  const [stock, setStock] = useState('0')
  const [fout, setFout] = useState<string | null>(null)
  const [bezig, setBezig] = useState(false)

  async function verstuur(e: FormEvent) {
    e.preventDefault()
    setBezig(true)
    setFout(null)
    try {
      onToegevoegd(
        await catalogusApi.voegBoekToe({
          isbn,
          titel,
          auteurs: auteurs.split(',').map((a) => a.trim()).filter(Boolean),
          omschrijving,
          prijs: Number(prijs.replace(',', '.')),
          stock: Number(stock),
        }),
      )
    } catch (err) {
      setFout((err as Error).message)
    } finally {
      setBezig(false)
    }
  }

  return (
    <form className="kaart paneel" style={{ marginBottom: 18 }} onSubmit={verstuur}>
      <h3>Nieuw boek</h3>
      {fout && <Bericht soort="fout">{fout}</Bericht>}
      <div className="rij rij--2">
        <div className="veld">
          <label htmlFor="nb-isbn">ISBN</label>
          <input id="nb-isbn" required placeholder="978-90-..." value={isbn} onChange={(e) => setIsbn(e.target.value)} />
        </div>
        <div className="veld">
          <label htmlFor="nb-titel">Titel</label>
          <input id="nb-titel" required value={titel} onChange={(e) => setTitel(e.target.value)} />
        </div>
      </div>
      <div className="veld">
        <label htmlFor="nb-auteurs">Auteur(s)</label>
        <input id="nb-auteurs" required value={auteurs} onChange={(e) => setAuteurs(e.target.value)} />
        <span className="veld__hulp">Meerdere auteurs scheiden met een komma</span>
      </div>
      <div className="veld">
        <label htmlFor="nb-omschrijving">Omschrijving</label>
        <textarea id="nb-omschrijving" maxLength={2000} value={omschrijving} onChange={(e) => setOmschrijving(e.target.value)} />
      </div>
      <div className="rij rij--2">
        <div className="veld">
          <label htmlFor="nb-prijs">Prijs (€)</label>
          <input id="nb-prijs" required inputMode="decimal" placeholder="19,99" value={prijs} onChange={(e) => setPrijs(e.target.value)} />
        </div>
        <div className="veld">
          <label htmlFor="nb-stock">Beginstock</label>
          <input id="nb-stock" type="number" min={0} required value={stock} onChange={(e) => setStock(e.target.value)} />
        </div>
      </div>
      <button type="submit" className="knop knop--vol" disabled={bezig}>
        {bezig ? 'Bezig…' : 'Boek toevoegen'}
      </button>
    </form>
  )
}
