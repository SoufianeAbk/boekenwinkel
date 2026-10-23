import { useCallback, useEffect, useState, type DependencyList } from 'react'

/** Laadt data bij het tonen van een pagina en houdt laad- en foutstatus bij. */
export function useData<T>(laad: () => Promise<T>, afhankelijkheden: DependencyList) {
  const [data, setData] = useState<T | null>(null)
  const [fout, setFout] = useState<string | null>(null)
  const [laden, setLaden] = useState(true)
  const [versie, setVersie] = useState(0)

  useEffect(() => {
    let actief = true
    setLaden(true)
    setFout(null)
    laad()
      .then((resultaat) => actief && setData(resultaat))
      .catch((e: Error) => actief && setFout(e.message))
      .finally(() => actief && setLaden(false))
    return () => {
      actief = false
    }
    // de laadfunctie zelf is bewust geen afhankelijkheid: enkel de meegegeven waarden bepalen wanneer we herladen
  }, [...afhankelijkheden, versie])

  const herlaad = useCallback(() => setVersie((v) => v + 1), [])

  return { data, setData, fout, laden, herlaad }
}
