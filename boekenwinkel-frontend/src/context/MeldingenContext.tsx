import { createContext, useCallback, useContext, useEffect, useMemo, useState, type ReactNode } from 'react'
import { profielApi } from '../api/gebruikers'
import type { Melding } from '../api/types'
import { useAuth } from './AuthContext'

interface MeldingenWaarde {
  meldingen: Melding[]
  ongelezen: number
  ververs: () => Promise<void>
  markeerGelezen: () => Promise<void>
}

const VERVERS_INTERVAL_MS = 30_000
const MeldingenContext = createContext<MeldingenWaarde | null>(null)

/** Houdt de meldingen van de ingelogde gebruiker bij, zodat een bevestiging meteen zichtbaar wordt. */
export function MeldingenProvider({ children }: { children: ReactNode }) {
  const { gebruiker } = useAuth()
  const [meldingen, setMeldingen] = useState<Melding[]>([])

  const ververs = useCallback(async () => {
    if (!gebruiker) {
      setMeldingen([])
      return
    }
    try {
      setMeldingen(await profielApi.meldingen())
    } catch {
      // niet kritiek: bij de volgende poging opnieuw proberen
    }
  }, [gebruiker])

  useEffect(() => {
    void ververs()
    if (!gebruiker) return
    const timer = window.setInterval(() => void ververs(), VERVERS_INTERVAL_MS)
    return () => window.clearInterval(timer)
  }, [gebruiker, ververs])

  const waarde = useMemo<MeldingenWaarde>(
    () => ({
      meldingen,
      ongelezen: meldingen.filter((melding) => !melding.gelezen).length,
      ververs,
      markeerGelezen: async () => {
        if (!meldingen.some((melding) => !melding.gelezen)) return
        await profielApi.markeerMeldingenGelezen()
        setMeldingen((huidig) => huidig.map((melding) => ({ ...melding, gelezen: true })))
      },
    }),
    [meldingen, ververs],
  )

  return <MeldingenContext.Provider value={waarde}>{children}</MeldingenContext.Provider>
}

export function useMeldingen(): MeldingenWaarde {
  const waarde = useContext(MeldingenContext)
  if (!waarde) throw new Error('useMeldingen moet binnen een MeldingenProvider gebruikt worden')
  return waarde
}
