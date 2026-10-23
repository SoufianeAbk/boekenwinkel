import { createContext, useContext, useEffect, useMemo, useState, type ReactNode } from 'react'
import type { Boek } from '../api/types'

/** Een boek dat de gebruiker wil reserveren. De stock onthouden we om het aantal te begrenzen. */
export interface MandjeItem {
  isbn: string
  titel: string
  auteurs: string[]
  prijs: number
  stock: number
  aantal: number
}

interface MandjeWaarde {
  items: MandjeItem[]
  aantalBoeken: number
  totaal: number
  aantalVan: (isbn: string) => number
  voegToe: (boek: Boek, aantal?: number) => void
  wijzigAantal: (isbn: string, aantal: number) => void
  verwijder: (isbn: string) => void
  leegmaken: () => void
}

const OPSLAG = 'leeshoek.mandje'
const MandjeContext = createContext<MandjeWaarde | null>(null)

function leesMandje(): MandjeItem[] {
  try {
    const ruw = localStorage.getItem(OPSLAG)
    return ruw ? (JSON.parse(ruw) as MandjeItem[]) : []
  } catch {
    return []
  }
}

export function MandjeProvider({ children }: { children: ReactNode }) {
  const [items, setItems] = useState<MandjeItem[]>(leesMandje)

  useEffect(() => {
    try {
      localStorage.setItem(OPSLAG, JSON.stringify(items))
    } catch {
      // opslag niet beschikbaar: het mandje blijft enkel in het geheugen
    }
  }, [items])

  const waarde = useMemo<MandjeWaarde>(() => {
    const begrens = (aantal: number, stock: number) => Math.max(0, Math.min(aantal, stock, 20))
    return {
      items,
      aantalBoeken: items.reduce((som, item) => som + item.aantal, 0),
      totaal: items.reduce((som, item) => som + item.prijs * item.aantal, 0),
      aantalVan: (isbn) => items.find((item) => item.isbn === isbn)?.aantal ?? 0,
      voegToe: (boek, aantal = 1) =>
        setItems((huidig) => {
          const bestaand = huidig.find((item) => item.isbn === boek.isbn)
          if (bestaand) {
            return huidig.map((item) =>
              item.isbn === boek.isbn
                ? { ...item, stock: boek.stock, prijs: boek.prijs, aantal: begrens(item.aantal + aantal, boek.stock) }
                : item,
            )
          }
          const nieuw: MandjeItem = {
            isbn: boek.isbn,
            titel: boek.titel,
            auteurs: boek.auteurs,
            prijs: boek.prijs,
            stock: boek.stock,
            aantal: begrens(aantal, boek.stock),
          }
          return nieuw.aantal > 0 ? [...huidig, nieuw] : huidig
        }),
      wijzigAantal: (isbn, aantal) =>
        setItems((huidig) =>
          huidig
            .map((item) => (item.isbn === isbn ? { ...item, aantal: begrens(aantal, item.stock) } : item))
            .filter((item) => item.aantal > 0),
        ),
      verwijder: (isbn) => setItems((huidig) => huidig.filter((item) => item.isbn !== isbn)),
      leegmaken: () => setItems([]),
    }
  }, [items])

  return <MandjeContext.Provider value={waarde}>{children}</MandjeContext.Provider>
}

export function useMandje(): MandjeWaarde {
  const waarde = useContext(MandjeContext)
  if (!waarde) throw new Error('useMandje moet binnen een MandjeProvider gebruikt worden')
  return waarde
}
