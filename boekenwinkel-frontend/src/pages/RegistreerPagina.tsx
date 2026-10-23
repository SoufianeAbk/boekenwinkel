import { useState, type FormEvent } from 'react'
import { Link, Navigate, useLocation, useNavigate } from 'react-router-dom'
import type { Registratie } from '../api/types'
import { Bericht } from '../components/Bericht'
import { useAuth } from '../context/AuthContext'

const LEEG: Registratie = { gebruikersnaam: '', email: '', voornaam: '', achternaam: '', wachtwoord: '' }

export function RegistreerPagina() {
  const { registreer, gebruiker } = useAuth()
  const navigate = useNavigate()
  const van = (useLocation().state as { van?: string } | null)?.van ?? '/'
  const [gegevens, setGegevens] = useState<Registratie>(LEEG)
  const [herhaling, setHerhaling] = useState('')
  const [fout, setFout] = useState<string | null>(null)
  const [bezig, setBezig] = useState(false)

  if (gebruiker) return <Navigate to={van} replace />

  const wijzig = (veld: keyof Registratie) => (e: { target: { value: string } }) =>
    setGegevens((huidig) => ({ ...huidig, [veld]: e.target.value }))

  async function verstuur(e: FormEvent) {
    e.preventDefault()
    if (gegevens.wachtwoord !== herhaling) {
      setFout('De twee wachtwoorden zijn niet gelijk.')
      return
    }
    setBezig(true)
    setFout(null)
    try {
      await registreer(gegevens)
      navigate(van, { replace: true })
    } catch (err) {
      setFout((err as Error).message)
    } finally {
      setBezig(false)
    }
  }

  return (
    <div className="auth">
      <div className="kaart paneel">
        <h1>Account aanmaken</h1>
        <p className="gedempt">Met een account reserveer je boeken en schrijf je reviews over boeken die je kocht.</p>
        {fout && <Bericht soort="fout">{fout}</Bericht>}
        <form onSubmit={verstuur}>
          <div className="rij rij--2">
            <div className="veld">
              <label htmlFor="voornaam">Voornaam</label>
              <input id="voornaam" autoComplete="given-name" required value={gegevens.voornaam} onChange={wijzig('voornaam')} />
            </div>
            <div className="veld">
              <label htmlFor="achternaam">Achternaam</label>
              <input id="achternaam" autoComplete="family-name" required value={gegevens.achternaam} onChange={wijzig('achternaam')} />
            </div>
          </div>
          <div className="veld">
            <label htmlFor="email">E-mailadres</label>
            <input id="email" type="email" autoComplete="email" required value={gegevens.email} onChange={wijzig('email')} />
          </div>
          <div className="veld">
            <label htmlFor="gebruikersnaam">Gebruikersnaam</label>
            <input
              id="gebruikersnaam"
              autoComplete="username"
              required
              minLength={3}
              maxLength={50}
              pattern="[A-Za-z0-9._\-]+"
              value={gegevens.gebruikersnaam}
              onChange={wijzig('gebruikersnaam')}
            />
            <span className="veld__hulp">Letters, cijfers, punt, - en _</span>
          </div>
          <div className="rij rij--2">
            <div className="veld">
              <label htmlFor="wachtwoord">Wachtwoord</label>
              <input
                id="wachtwoord"
                type="password"
                autoComplete="new-password"
                required
                minLength={8}
                value={gegevens.wachtwoord}
                onChange={wijzig('wachtwoord')}
              />
              <span className="veld__hulp">Minstens 8 tekens</span>
            </div>
            <div className="veld">
              <label htmlFor="herhaling">Herhaal wachtwoord</label>
              <input
                id="herhaling"
                type="password"
                autoComplete="new-password"
                required
                value={herhaling}
                onChange={(e) => setHerhaling(e.target.value)}
              />
            </div>
          </div>
          <button type="submit" className="knop knop--vol knop--breed" disabled={bezig}>
            {bezig ? 'Bezig…' : 'Registreren'}
          </button>
        </form>
        <p className="auth__wissel">
          Al een account?{' '}
          <Link to="/login" state={{ van }}>
            Log in
          </Link>
        </p>
      </div>
    </div>
  )
}
