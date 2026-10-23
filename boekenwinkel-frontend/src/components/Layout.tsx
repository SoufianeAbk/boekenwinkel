import { Link, NavLink, Outlet, useNavigate } from 'react-router-dom'
import { useAuth } from '../context/AuthContext'
import { useMandje } from '../context/MandjeContext'
import { useMeldingen } from '../context/MeldingenContext'

function Logo() {
  return (
    <svg viewBox="0 0 32 32" aria-hidden="true">
      <rect width="32" height="32" rx="8" fill="var(--accent)" />
      <path d="M8 9.5c2.8-.9 5.4-.6 8 1v13c-2.6-1.6-5.2-1.9-8-1z" fill="#f6efe3" />
      <path d="M24 9.5c-2.8-.9-5.4-.6-8 1v13c2.6-1.6 5.2-1.9 8-1z" fill="#e3d5bb" />
    </svg>
  )
}

export function Layout() {
  const { gebruiker, isAdmin, logout } = useAuth()
  const { aantalBoeken } = useMandje()
  const { ongelezen } = useMeldingen()
  const navigate = useNavigate()

  return (
    <>
      <header className="kop">
        <div className="kop__binnen container">
          <Link to="/" className="logo">
            <Logo />
            De Leeshoek
          </Link>
          <nav className="nav" aria-label="Hoofdnavigatie">
            <NavLink to="/" end>
              Catalogus
            </NavLink>
            {isAdmin && <NavLink to="/beheer">Beheer</NavLink>}
            <NavLink to="/mandje">
              Reservatie
              {aantalBoeken > 0 && (
                <span className="teller" aria-label={`${aantalBoeken} boeken`}>
                  {aantalBoeken}
                </span>
              )}
            </NavLink>
            {gebruiker ? (
              <>
                <NavLink to="/profiel">
                  Mijn profiel
                  {ongelezen > 0 && (
                    <span className="teller teller--accent" aria-label={`${ongelezen} nieuwe meldingen`}>
                      {ongelezen}
                    </span>
                  )}
                </NavLink>
                <button
                  type="button"
                  className="knop knop--tekst"
                  onClick={() => {
                    logout()
                    navigate('/')
                  }}
                >
                  Uitloggen
                </button>
              </>
            ) : (
              <Link to="/login" className="knop knop--vol knop--klein">
                Inloggen
              </Link>
            )}
          </nav>
        </div>
      </header>
      <main className="container inhoud">
        <Outlet />
      </main>
      <footer className="voet container">
        De Leeshoek · fictieve boekenwinkel · reserveer online, haal af en betaal in de winkel
      </footer>
    </>
  )
}
