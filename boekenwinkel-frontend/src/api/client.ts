/** Fout van de API, met de leesbare boodschap uit het ProblemDetail-veld "detail". */
export class ApiFout extends Error {
  readonly status: number

  constructor(status: number, boodschap: string) {
    super(boodschap)
    this.status = status
  }
}

/** Event dat afgevuurd wordt wanneer de backend de token weigert (verlopen of ongeldig). */
export const SESSIE_VERLOPEN = 'leeshoek:sessie-verlopen'

let huidigeToken: string | null = null

/** Wordt door de AuthProvider ingesteld zodat elk verzoek de token van de ingelogde gebruiker meestuurt. */
export function stelTokenIn(token: string | null) {
  huidigeToken = token
}

interface Opties {
  methode?: 'GET' | 'POST' | 'PUT' | 'DELETE'
  body?: unknown
}

export async function api<T>(pad: string, { methode = 'GET', body }: Opties = {}): Promise<T> {
  const headers: Record<string, string> = {}
  const token = huidigeToken
  if (token) headers.Authorization = `Bearer ${token}`
  if (body !== undefined) headers['Content-Type'] = 'application/json'

  let antwoord: Response
  try {
    antwoord = await fetch(pad, {
      method: methode,
      headers,
      body: body !== undefined ? JSON.stringify(body) : undefined,
    })
  } catch {
    throw new ApiFout(0, 'De server is niet bereikbaar. Controleer of de backend-services draaien.')
  }

  if (antwoord.status === 401 && token) {
    window.dispatchEvent(new Event(SESSIE_VERLOPEN))
  }

  if (!antwoord.ok) {
    throw new ApiFout(antwoord.status, await leesFoutboodschap(antwoord))
  }

  const tekst = await antwoord.text()
  return (tekst ? JSON.parse(tekst) : undefined) as T
}

async function leesFoutboodschap(antwoord: Response): Promise<string> {
  try {
    const probleem = await antwoord.json()
    if (typeof probleem?.detail === 'string' && probleem.detail) return probleem.detail
  } catch {
    // geen JSON in het antwoord
  }
  if (antwoord.status === 401) return 'Je sessie is verlopen. Log opnieuw in.'
  if (antwoord.status === 403) return 'Je hebt geen toegang tot deze actie.'
  if (antwoord.status >= 500) return 'De server is momenteel niet bereikbaar. Controleer of de backend-services draaien.'
  return `Er ging iets mis (HTTP ${antwoord.status}).`
}
