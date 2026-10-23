import { useEffect, useState, type FormEvent } from 'react'
import { Link, useSearchParams } from 'react-router-dom'
import { profielApi, reservatieApi } from '../api/gebruikers'
import type { GekochtBoek, Profiel } from '../api/types'
import { Bericht } from '../components/Bericht'
import { BoekCover } from '../components/BoekCover'
import { ReservatieKaart } from '../components/ReservatieKaart'
import { Sterren, SterrenInvoer } from '../components/Sterren'
import { useAuth } from '../context/AuthContext'
import { useMeldingen } from '../context/MeldingenContext'
import { formatDatum, formatDatumTijd } from '../util/formaat'
import { useData } from '../util/useData'

const TABS = [
  { id: 'meldingen', label: 'Meldingen' },
  { id: 'reservaties', label: 'Reservaties' },
  { id: 'boeken', label: 'Gekochte boeken' },
  { id: 'gegevens', label: 'Mijn gegevens' },
] as const

type TabId = (typeof TABS)[number]['id']

export function ProfielPagina() {
  const [params, setParams] = useSearchParams()
  const gevraagd = params.get('tab')
  const tab: TabId = TABS.some((t) => t.id === gevraagd) ? (gevraagd as TabId) : 'meldingen'
  const { data: profiel, setData: setProfiel, fout } = useData(() => profielApi.haal(), [])
  const { ongelezen } = useMeldingen()

  return (
    <>
      <div className="profielkop">
        <span className="avatar avatar--groot">
          {profiel ? `${profiel.voornaam[0] ?? ''}${profiel.achternaam[0] ?? ''}` : ''}
        </span>
        <div>
          <h1>{profiel ? `${profiel.voornaam} ${profiel.achternaam}` : 'Mijn profiel'}</h1>
          {profiel && (
            <div className="gedempt">
              @{profiel.gebruikersnaam} · {profiel.email}
            </div>
          )}
        </div>
      </div>

      {fout && <Bericht soort="fout">{fout}</Bericht>}

      <div className="tabs" role="tablist">
        {TABS.map((t) => (
          <button
            key={t.id}
            type="button"
            role="tab"
            aria-selected={tab === t.id}
            onClick={() => setParams({ tab: t.id }, { replace: true })}
          >
            {t.label}
            {t.id === 'meldingen' && ongelezen > 0 && <span className="teller teller--accent">{ongelezen}</span>}
            {t.id === 'boeken' && profiel && <span className="teller">{profiel.gekochteBoeken.length}</span>}
          </button>
        ))}
      </div>

      {tab === 'meldingen' && <MeldingenTab />}
      {tab === 'reservaties' && <ReservatiesTab />}
      {tab === 'boeken' && profiel && <GekochteBoekenTab profiel={profiel} onWijziging={setProfiel} />}
      {tab === 'gegevens' && profiel && <GegevensTab profiel={profiel} onOpgeslagen={setProfiel} />}
    </>
  )
}

function MeldingenTab() {
  const { meldingen, ongelezen, ververs, markeerGelezen } = useMeldingen()
  const [fout, setFout] = useState<string | null>(null)

  useEffect(() => {
    void ververs()
  }, [ververs])

  return (
    <section>
      <div className="sectiekop">
        <h2>Meldingen</h2>
        {ongelezen > 0 && (
          <button
            type="button"
            className="knop knop--klein"
            onClick={() => markeerGelezen().catch((e: Error) => setFout(e.message))}
          >
            Alles als gelezen markeren
          </button>
        )}
      </div>
      {fout && <Bericht soort="fout">{fout}</Bericht>}
      {meldingen.length === 0 ? (
        <div className="kaart paneel gedempt">
          Nog geen meldingen. Wanneer de winkel een reservatie bevestigt, verschijnt hier wanneer je je boeken mag
          afhalen en betalen.
        </div>
      ) : (
        <div className="lijst">
          {meldingen.map((melding) => (
            <article key={melding.id} className={`kaart melding ${melding.gelezen ? '' : 'melding--nieuw'}`}>
              <span className="melding__icoon" aria-hidden="true">
                <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
                  <path d="M20 6 9 17l-5-5" />
                </svg>
              </span>
              <div>
                <p>{melding.tekst}</p>
                <div className="gedempt klein">
                  {formatDatumTijd(melding.aangemaaktOp)}
                  {!melding.gelezen && ' · nieuw'}
                </div>
              </div>
            </article>
          ))}
        </div>
      )}
    </section>
  )
}

function ReservatiesTab() {
  const { data: reservaties, setData, fout, laden } = useData(() => reservatieApi.mijnReservaties(), [])
  const [actieFout, setActieFout] = useState<string | null>(null)

  async function annuleer(id: number) {
    setActieFout(null)
    try {
      const bijgewerkt = await reservatieApi.annuleer(id)
      setData((huidig) => huidig?.map((r) => (r.id === id ? bijgewerkt : r)) ?? null)
    } catch (e) {
      setActieFout((e as Error).message)
    }
  }

  return (
    <section>
      <div className="sectiekop">
        <h2>Mijn reservaties</h2>
      </div>
      {(fout || actieFout) && <Bericht soort="fout">{fout ?? actieFout}</Bericht>}
      {laden && !reservaties && <p className="gedempt">Laden…</p>}
      {reservaties && reservaties.length === 0 && (
        <div className="kaart paneel gedempt">
          Je hebt nog geen reservaties. <Link to="/">Ontdek de catalogus</Link>
        </div>
      )}
      <div className="lijst">
        {reservaties?.map((reservatie) => (
          <ReservatieKaart key={reservatie.id} reservatie={reservatie}>
            {reservatie.status === 'GERESERVEERD' && (
              <button type="button" className="knop knop--klein knop--gevaar" onClick={() => annuleer(reservatie.id)}>
                Reservatie annuleren
              </button>
            )}
          </ReservatieKaart>
        ))}
      </div>
    </section>
  )
}

function GekochteBoekenTab({ profiel, onWijziging }: { profiel: Profiel; onWijziging: (p: Profiel) => void }) {
  if (profiel.gekochteBoeken.length === 0) {
    return (
      <div className="kaart paneel gedempt">
        Je hebt nog geen boeken gekocht. Boeken uit een bevestigde reservatie verschijnen hier, en dan kan je er een
        review voor schrijven.
      </div>
    )
  }

  const werkBij = (boek: GekochtBoek) =>
    onWijziging({
      ...profiel,
      gekochteBoeken: profiel.gekochteBoeken.map((b) => (b.isbn === boek.isbn ? boek : b)),
    })

  return (
    <section>
      <div className="sectiekop">
        <h2>Gekochte boeken</h2>
      </div>
      <div className="lijst">
        {profiel.gekochteBoeken.map((boek) => (
          <GekochtBoekKaart key={boek.isbn} boek={boek} onReview={werkBij} />
        ))}
      </div>
    </section>
  )
}

function GekochtBoekKaart({ boek, onReview }: { boek: GekochtBoek; onReview: (boek: GekochtBoek) => void }) {
  const [bewerken, setBewerken] = useState(false)
  const [score, setScore] = useState(boek.reviewScore ?? 0)
  const [tekst, setTekst] = useState(boek.reviewTekst ?? '')
  const [fout, setFout] = useState<string | null>(null)
  const [bezig, setBezig] = useState(false)
  const [opgeslagen, setOpgeslagen] = useState(false)

  async function verstuur(e: FormEvent) {
    e.preventDefault()
    if (score === 0) {
      setFout('Kies een score van 1 tot 5 sterren.')
      return
    }
    setBezig(true)
    setFout(null)
    try {
      onReview(await profielApi.schrijfReview(boek.isbn, score, tekst))
      setBewerken(false)
      setOpgeslagen(true)
    } catch (err) {
      setFout((err as Error).message)
    } finally {
      setBezig(false)
    }
  }

  return (
    <article className="kaart gekocht">
      <BoekCover isbn={boek.isbn} titel={boek.titel} auteurs={['']} />
      <div>
        <h3>
          <Link to={`/boeken/${boek.isbn}`}>{boek.titel}</Link>
        </h3>
        <div className="gedempt klein">
          {boek.aantal} {boek.aantal === 1 ? 'exemplaar' : 'exemplaren'} · laatst gekocht op{' '}
          {formatDatum(boek.laatstGekochtOp)}
        </div>

        {opgeslagen && !bewerken && (
          <div style={{ marginTop: 12 }}>
            <Bericht soort="succes">
              Bedankt! Je review staat nu op de <Link to={`/boeken/${boek.isbn}`}>pagina van het boek</Link>.
            </Bericht>
          </div>
        )}

        {!bewerken && boek.reviewScore !== null && (
          <div className="eigen-review">
            <div className="sectiekop" style={{ margin: 0 }}>
              <span className="klein gedempt">Jouw review</span>
              <button type="button" className="knop knop--tekst knop--klein" onClick={() => setBewerken(true)}>
                Wijzigen
              </button>
            </div>
            <Sterren score={boek.reviewScore} />
            {boek.reviewTekst && <p>{boek.reviewTekst}</p>}
          </div>
        )}

        {!bewerken && boek.reviewScore === null && (
          <button type="button" className="knop knop--klein" style={{ marginTop: 12 }} onClick={() => setBewerken(true)}>
            Schrijf een review
          </button>
        )}

        {bewerken && (
          <form className="reviewformulier" onSubmit={verstuur}>
            {fout && <Bericht soort="fout">{fout}</Bericht>}
            <div className="veld">
              <span style={{ fontWeight: 600, fontSize: '0.9rem' }}>Score</span>
              <SterrenInvoer waarde={score} onChange={setScore} />
            </div>
            <div className="veld">
              <label htmlFor={`review-${boek.isbn}`}>Je review</label>
              <textarea
                id={`review-${boek.isbn}`}
                maxLength={2000}
                placeholder="Wat vond je van dit boek?"
                value={tekst}
                onChange={(e) => setTekst(e.target.value)}
              />
            </div>
            <div className="knoppenrij">
              <button type="submit" className="knop knop--vol knop--klein" disabled={bezig}>
                {bezig ? 'Bezig…' : 'Review plaatsen'}
              </button>
              <button type="button" className="knop knop--tekst knop--klein" onClick={() => setBewerken(false)}>
                Annuleren
              </button>
            </div>
          </form>
        )}
      </div>
    </article>
  )
}

function GegevensTab({ profiel, onOpgeslagen }: { profiel: Profiel; onOpgeslagen: (p: Profiel) => void }) {
  const { werkGebruikerBij } = useAuth()
  const [voornaam, setVoornaam] = useState(profiel.voornaam)
  const [achternaam, setAchternaam] = useState(profiel.achternaam)
  const [email, setEmail] = useState(profiel.email)
  const [fout, setFout] = useState<string | null>(null)
  const [succes, setSucces] = useState(false)
  const [bezig, setBezig] = useState(false)

  async function verstuur(e: FormEvent) {
    e.preventDefault()
    setBezig(true)
    setFout(null)
    setSucces(false)
    try {
      const bijgewerkt = await profielApi.wijzig({ voornaam, achternaam, email })
      onOpgeslagen(bijgewerkt)
      const { gekochteBoeken: _, ...gebruiker } = bijgewerkt
      werkGebruikerBij(gebruiker)
      setSucces(true)
    } catch (err) {
      setFout((err as Error).message)
    } finally {
      setBezig(false)
    }
  }

  return (
    <section className="kaart paneel" style={{ maxWidth: 560 }}>
      <h2>Mijn gegevens</h2>
      {fout && <Bericht soort="fout">{fout}</Bericht>}
      {succes && <Bericht soort="succes">Je gegevens zijn opgeslagen.</Bericht>}
      <form onSubmit={verstuur}>
        <div className="veld">
          <label htmlFor="profiel-gebruikersnaam">Gebruikersnaam</label>
          <input id="profiel-gebruikersnaam" value={profiel.gebruikersnaam} readOnly />
        </div>
        <div className="rij rij--2">
          <div className="veld">
            <label htmlFor="profiel-voornaam">Voornaam</label>
            <input id="profiel-voornaam" required value={voornaam} onChange={(e) => setVoornaam(e.target.value)} />
          </div>
          <div className="veld">
            <label htmlFor="profiel-achternaam">Achternaam</label>
            <input id="profiel-achternaam" required value={achternaam} onChange={(e) => setAchternaam(e.target.value)} />
          </div>
        </div>
        <div className="veld">
          <label htmlFor="profiel-email">E-mailadres</label>
          <input id="profiel-email" type="email" required value={email} onChange={(e) => setEmail(e.target.value)} />
        </div>
        <button type="submit" className="knop knop--vol" disabled={bezig}>
          {bezig ? 'Bezig…' : 'Opslaan'}
        </button>
      </form>
    </section>
  )
}
