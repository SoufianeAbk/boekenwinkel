import { createContext, useCallback, useContext, useEffect, useMemo, useState, type ReactNode } from 'react'
import { SESSIE_VERLOPEN, stelTokenIn } from '../api/client'
import { authApi } from '../api/gebruikers'
import type { AuthAntwoord, Gebruiker, Registratie } from '../api/types'

interface Sessie {
  token: string
  gebruiker: Gebruiker
}

interface AuthWaarde {
  gebruiker: Gebruiker | null
  isAdmin: boolean
  login: (gebruikersnaam: string, wachtwoord: string) => Promise<void>
  registreer: (gegevens: Registratie) => Promise<void>
  logout: () => void
  werkGebruikerBij: (gebruiker: Gebruiker) => void
}

const OPSLAG = 'leeshoek.sessie'
const AuthContext = createContext<AuthWaarde | null>(null)

function isVerlopen(token: string): boolean {
  try {
    const payload = JSON.parse(atob(token.split('.')[1].replace(/-/g, '+').replace(/_/g, '/')))
    return typeof payload.exp === 'number' && payload.exp * 1000 < Date.now()
  } catch {
    return true
  }
}

function leesSessie(): Sessie | null {
  try {
    const ruw = localStorage.getItem(OPSLAG)
    if (!ruw) return null
    const sessie = JSON.parse(ruw) as Sessie
    return isVerlopen(sessie.token) ? null : sessie
  } catch {
    return null
  }
}

function schrijfSessie(sessie: Sessie | null) {
  try {
    if (sessie) localStorage.setItem(OPSLAG, JSON.stringify(sessie))
    else localStorage.removeItem(OPSLAG)
  } catch {
    // opslag niet beschikbaar (bv. privévenster): de sessie leeft dan enkel in het geheugen
  }
}

export function AuthProvider({ children }: { children: ReactNode }) {
  const [sessie, setSessie] = useState<Sessie | null>(() => {
    const opgeslagen = leesSessie()
    stelTokenIn(opgeslagen?.token ?? null)
    return opgeslagen
  })

  const bewaar = useCallback((nieuw: Sessie | null) => {
    stelTokenIn(nieuw?.token ?? null)
    schrijfSessie(nieuw)
    setSessie(nieuw)
  }, [])

  useEffect(() => {
    const bijVerlopen = () => bewaar(null)
    window.addEventListener(SESSIE_VERLOPEN, bijVerlopen)
    return () => window.removeEventListener(SESSIE_VERLOPEN, bijVerlopen)
  }, [bewaar])

  const waarde = useMemo<AuthWaarde>(() => {
    const start = (antwoord: AuthAntwoord) => bewaar({ token: antwoord.token, gebruiker: antwoord.gebruiker })
    return {
      gebruiker: sessie?.gebruiker ?? null,
      isAdmin: sessie?.gebruiker.rol === 'ADMIN',
      login: async (gebruikersnaam, wachtwoord) => start(await authApi.login(gebruikersnaam, wachtwoord)),
      registreer: async (gegevens) => start(await authApi.registreer(gegevens)),
      logout: () => bewaar(null),
      werkGebruikerBij: (gebruiker) => sessie && bewaar({ ...sessie, gebruiker }),
    }
  }, [sessie, bewaar])

  return <AuthContext.Provider value={waarde}>{children}</AuthContext.Provider>
}

export function useAuth(): AuthWaarde {
  const waarde = useContext(AuthContext)
  if (!waarde) throw new Error('useAuth moet binnen een AuthProvider gebruikt worden')
  return waarde
}
