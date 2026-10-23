import { useState } from 'react'
import { Link, useParams } from 'react-router-dom'
import { catalogusApi } from '../api/catalogus'
import type { BoekDetail } from '../api/types'
import { Bericht } from '../components/Bericht'
import { BoekCover } from '../components/BoekCover'
import { Sterren } from '../components/Sterren'
import { StockBadge } from '../components/StockBadge'
import { useMandje } from '../context/MandjeContext'
import { auteursTekst, formatDatum, formatPrijs } from '../util/formaat'
import { useData } from '../util/useData'

export function BoekDetailPagina() {
  const { isbn = '' } = useParams()
  const { data: boek, fout, laden } = useData(() => catalogusApi.haalBoek(isbn), [isbn])

  return (
    <>
      <Link to="/" className="terug">
        ← Terug naar de catalogus
      </Link>
      {fout && <Bericht soort="fout">{fout}</Bericht>}
      {laden && !boek && <div className="skelet" style={{ maxWidth: 280 }} />}
      {boek && (
        <>
          <BoekInfo boek={boek} />
          <Reviews boek={boek} />
        </>
      )}
    </>
  )
}

function BoekInfo({ boek }: { boek: BoekDetail }) {
  const { voegToe, aantalVan } = useMandje()
  const inMandje = aantalVan(boek.isbn)
  const maxExtra = Math.max(0, boek.stock - inMandje)
  const [aantal, setAantal] = useState(1)
  const [toegevoegd, setToegevoegd] = useState(false)

  const gekozen = Math.min(aantal, Math.max(1, maxExtra))

  return (
    <div className="detail">
      <BoekCover className="detail__cover" isbn={boek.isbn} titel={boek.titel} auteurs={boek.auteurs} />
      <div>
        <h1>{boek.titel}</h1>
        <div className="detail__auteurs">{auteursTekst(boek.auteurs)}</div>
        <div className="detail__score">
          {boek.gemiddeldeScore !== null ? (
            <>
              <Sterren score={boek.gemiddeldeScore} />
              <span>
                {boek.gemiddeldeScore.toLocaleString('nl-BE')} · {boek.aantalReviews}{' '}
                {boek.aantalReviews === 1 ? 'review' : 'reviews'}
              </span>
            </>
          ) : (
            <span>Nog geen reviews</span>
          )}
        </div>
        {boek.omschrijving && <p className="detail__omschrijving">{boek.omschrijving}</p>}

        <div className="kaart reserveerblok">
          <div className="reserveerblok__boven">
            <span className="prijs" style={{ fontSize: '1.6rem' }}>
              {formatPrijs(boek.prijs)}
            </span>
            <StockBadge stock={boek.stock} />
          </div>

          {maxExtra > 0 ? (
            <div className="reserveerblok__acties">
              <div className="stapper" aria-label="Aantal">
                <button type="button" aria-label="Minder" disabled={gekozen <= 1} onClick={() => setAantal(gekozen - 1)}>
                  −
                </button>
                <span aria-live="polite">{gekozen}</span>
                <button
                  type="button"
                  aria-label="Meer"
                  disabled={gekozen >= maxExtra}
                  onClick={() => setAantal(gekozen + 1)}
                >
                  +
                </button>
              </div>
              <button
                type="button"
                className="knop knop--vol"
                onClick={() => {
                  voegToe(boek, gekozen)
                  setAantal(1)
                  setToegevoegd(true)
                }}
              >
                Toevoegen aan reservatie
              </button>
            </div>
          ) : (
            <p className="gedempt" style={{ margin: 0 }}>
              {boek.stock === 0
                ? 'Dit boek is momenteel uitverkocht. Kom later terug: de catalogus wordt bijgewerkt zodra er een levering binnenkomt.'
                : 'Je hebt alle beschikbare exemplaren al in je reservatie.'}
            </p>
          )}

          {(toegevoegd || inMandje > 0) && (
            <div className="klein">
              {inMandje} {inMandje === 1 ? 'exemplaar' : 'exemplaren'} in je reservatie ·{' '}
              <Link to="/mandje">Bekijk reservatie →</Link>
            </div>
          )}
          <p className="gedempt klein" style={{ margin: 0 }}>
            Je betaalt pas bij het afhalen in de winkel.
          </p>
        </div>

        <dl className="eigenschappen">
          <dt>ISBN</dt>
          <dd>{boek.isbn}</dd>
          <dt>{boek.auteurs.length === 1 ? 'Auteur' : 'Auteurs'}</dt>
          <dd>{boek.auteurs.join(', ')}</dd>
        </dl>
      </div>
    </div>
  )
}

function Reviews({ boek }: { boek: BoekDetail }) {
  return (
    <section className="reviews" aria-labelledby="reviews-titel">
      <h2 id="reviews-titel">Reviews van lezers</h2>
      {boek.reviews.length === 0 ? (
        <div className="kaart paneel gedempt">
          Nog geen reviews voor dit boek. Kocht je het? Dan kan je vanuit je profiel een review schrijven.
        </div>
      ) : (
        <>
          <div className="kaart reviews__samenvatting">
            <span className="reviews__gemiddelde">{boek.gemiddeldeScore?.toLocaleString('nl-BE')}</span>
            <div>
              <Sterren score={boek.gemiddeldeScore ?? 0} groot />
              <div className="gedempt klein">
                Gemiddelde van {boek.aantalReviews} {boek.aantalReviews === 1 ? 'review' : 'reviews'}
              </div>
            </div>
          </div>
          {boek.reviews.map((review) => (
            <article key={review.id} className="kaart review">
              <div className="review__kop">
                <span className="review__naam">
                  <span className="avatar">{review.gebruikersnaam.slice(0, 2)}</span>
                  {review.gebruikersnaam}
                </span>
                <span className="gedempt klein">{formatDatum(review.aangemaaktOp)}</span>
              </div>
              <Sterren score={review.score} />
              {review.tekst && <p style={{ marginTop: 8 }}>{review.tekst}</p>}
            </article>
          ))}
        </>
      )}
    </section>
  )
}
