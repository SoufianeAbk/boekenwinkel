import { useEffect, useState } from 'react'
import { Link, useSearchParams } from 'react-router-dom'
import { catalogusApi } from '../api/catalogus'
import type { Boek } from '../api/types'
import { Bericht } from '../components/Bericht'
import { BoekCover } from '../components/BoekCover'
import { Sterren } from '../components/Sterren'
import { StockBadge } from '../components/StockBadge'
import { useMandje } from '../context/MandjeContext'
import { auteursTekst, formatPrijs } from '../util/formaat'
import { useData } from '../util/useData'

export function CatalogusPagina() {
  const [params, setParams] = useSearchParams()
  const zoekterm = params.get('zoek') ?? ''
  const [invoer, setInvoer] = useState(zoekterm)

  // pas zoeken wanneer de gebruiker even stopt met typen
  useEffect(() => {
    const timer = window.setTimeout(() => {
      const nieuw = invoer.trim()
      if (nieuw !== zoekterm) setParams(nieuw ? { zoek: nieuw } : {}, { replace: true })
    }, 300)
    return () => window.clearTimeout(timer)
  }, [invoer, zoekterm, setParams])

  const { data: boeken, fout, laden } = useData(() => catalogusApi.zoekBoeken(zoekterm || undefined), [zoekterm])

  return (
    <>
      <section className="hero">
        <div>
          <h1>Boeken voor elke leeshoek</h1>
          <p className="gedempt">
            Blader door onze catalogus en reserveer online. Zodra je reservatie bevestigd is, haal je je boeken af in de
            winkel en betaal je ter plaatse.
          </p>
        </div>
        <div className="zoekveld">
          <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" aria-hidden="true">
            <circle cx="11" cy="11" r="7" />
            <path d="m20 20-3.5-3.5" />
          </svg>
          <label htmlFor="zoek" className="visueel-verborgen">
            Zoek een boek
          </label>
          <input
            id="zoek"
            type="search"
            placeholder="Zoek op titel, auteur of ISBN"
            value={invoer}
            onChange={(e) => setInvoer(e.target.value)}
          />
        </div>
      </section>

      {fout && <Bericht soort="fout">{fout}</Bericht>}

      {laden && !boeken && (
        <div className="raster" aria-busy="true">
          {Array.from({ length: 8 }, (_, i) => (
            <div key={i} className="skelet" />
          ))}
        </div>
      )}

      {boeken && boeken.length === 0 && (
        <div className="leeg">
          <h2>Geen boeken gevonden</h2>
          <p>Probeer een andere zoekterm.</p>
        </div>
      )}

      {boeken && boeken.length > 0 && (
        <>
          <p className="resultaat-info">
            {zoekterm ? `${boeken.length} resultaten voor “${zoekterm}”` : `${boeken.length} titels`} ·{' '}
            {boeken.filter((b) => b.stock > 0).length} op voorraad
          </p>
          <div className="raster">
            {boeken.map((boek) => (
              <BoekKaart key={boek.isbn} boek={boek} />
            ))}
          </div>
        </>
      )}
    </>
  )
}

function BoekKaart({ boek }: { boek: Boek }) {
  const { voegToe, aantalVan } = useMandje()
  const inMandje = aantalVan(boek.isbn)
  const kanToevoegen = boek.stock > inMandje

  let label = 'Reserveren'
  if (boek.stock === 0) label = 'Uitverkocht'
  else if (inMandje > 0) label = kanToevoegen ? `Gereserveerd (${inMandje}) · +1` : `Gereserveerd (${inMandje})`

  return (
    <article className="kaart boekkaart">
      <Link to={`/boeken/${boek.isbn}`} className="boekkaart__link">
        <BoekCover isbn={boek.isbn} titel={boek.titel} auteurs={boek.auteurs} />
        <div>
          <h3>{boek.titel}</h3>
          <p className="boekkaart__auteur">{auteursTekst(boek.auteurs)}</p>
        </div>
      </Link>
      <div className="boekkaart__score">
        {boek.gemiddeldeScore !== null ? (
          <>
            <Sterren score={boek.gemiddeldeScore} />
            <span>({boek.aantalReviews})</span>
          </>
        ) : (
          <span>Nog geen reviews</span>
        )}
      </div>
      <div className="boekkaart__voet">
        <span className="prijs">{formatPrijs(boek.prijs)}</span>
        <StockBadge stock={boek.stock} />
      </div>
      <button
        type="button"
        className={`knop ${inMandje > 0 ? '' : 'knop--vol'}`}
        disabled={!kanToevoegen}
        onClick={() => voegToe(boek)}
      >
        {label}
      </button>
    </article>
  )
}
