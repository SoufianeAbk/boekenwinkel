import { useState, type FormEvent } from 'react'
import { Link, Navigate, useLocation, useNavigate } from 'react-router-dom'
import { Bericht } from '../components/Bericht'
import { useAuth } from '../context/AuthContext'

export function LoginPagina() {
  const { login, gebruiker } = useAuth()
  const navigate = useNavigate()
  const van = (useLocation().state as { van?: string } | null)?.van ?? '/'
  const [gebruikersnaam, setGebruikersnaam] = useState('')
  const [wachtwoord, setWachtwoord] = useState('')
  const [fout, setFout] = useState<string | null>(null)
  const [bezig, setBezig] = useState(false)

  if (gebruiker) return <Navigate to={van} replace />

  async function verstuur(e: FormEvent) {
    e.preventDefault()
    setBezig(true)
    setFout(null)
    try {
      await login(gebruikersnaam, wachtwoord)
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
        <h1>Inloggen</h1>
        <p className="gedempt">Log in om boeken te reserveren en je profiel te bekijken.</p>
        {fout && <Bericht soort="fout">{fout}</Bericht>}
        <form onSubmit={verstuur}>
          <div className="veld">
            <label htmlFor="gebruikersnaam">Gebruikersnaam</label>
            <input
              id="gebruikersnaam"
              autoComplete="username"
              required
              value={gebruikersnaam}
              onChange={(e) => setGebruikersnaam(e.target.value)}
            />
          </div>
          <div className="veld">
            <label htmlFor="wachtwoord">Wachtwoord</label>
            <input
              id="wachtwoord"
              type="password"
              autoComplete="current-password"
              required
              value={wachtwoord}
              onChange={(e) => setWachtwoord(e.target.value)}
            />
          </div>
          <button type="submit" className="knop knop--vol knop--breed" disabled={bezig}>
            {bezig ? 'Bezig…' : 'Inloggen'}
          </button>
        </form>
        <p className="auth__wissel">
          Nog geen account?{' '}
          <Link to="/registreer" state={{ van }}>
            Registreer je
          </Link>
        </p>
      </div>
    </div>
  )
}
