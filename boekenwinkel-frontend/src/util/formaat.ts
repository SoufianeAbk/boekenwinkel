const euro = new Intl.NumberFormat('nl-BE', { style: 'currency', currency: 'EUR' })
const datum = new Intl.DateTimeFormat('nl-BE', { day: 'numeric', month: 'long', year: 'numeric' })
const datumKort = new Intl.DateTimeFormat('nl-BE', { day: '2-digit', month: '2-digit', year: 'numeric' })
const datumTijd = new Intl.DateTimeFormat('nl-BE', {
  day: 'numeric',
  month: 'short',
  year: 'numeric',
  hour: '2-digit',
  minute: '2-digit',
})

export const formatPrijs = (bedrag: number) => euro.format(bedrag)

/** "2026-10-20" of "2026-10-20T14:03:00" → "20 oktober 2026" */
export const formatDatum = (iso: string) => datum.format(new Date(iso.length === 10 ? `${iso}T00:00:00` : iso))

export const formatDatumKort = (iso: string) => datumKort.format(new Date(iso.length === 10 ? `${iso}T00:00:00` : iso))

export const formatDatumTijd = (iso: string) => datumTijd.format(new Date(iso))

/** Datum in het formaat van een <input type="date">, x dagen vanaf vandaag. */
export function datumVanafVandaag(dagen: number): string {
  const d = new Date()
  d.setDate(d.getDate() + dagen)
  const maand = String(d.getMonth() + 1).padStart(2, '0')
  const dag = String(d.getDate()).padStart(2, '0')
  return `${d.getFullYear()}-${maand}-${dag}`
}

export const auteursTekst = (auteurs: string[]) =>
  auteurs.length <= 1 ? (auteurs[0] ?? '') : `${auteurs.slice(0, -1).join(', ')} en ${auteurs[auteurs.length - 1]}`
