// Types die overeenkomen met de DTO's van de backend-services.

export interface Boek {
  isbn: string
  titel: string
  auteurs: string[]
  omschrijving: string | null
  prijs: number
  stock: number
  gemiddeldeScore: number | null
  aantalReviews: number
}

export interface Review {
  id: number
  gebruikersnaam: string
  score: number
  tekst: string | null
  aangemaaktOp: string
}

export interface BoekDetail extends Boek {
  reviews: Review[]
}

export interface NieuwBoek {
  isbn: string
  titel: string
  auteurs: string[]
  omschrijving: string
  prijs: number
  stock: number
}

export type Rol = 'KLANT' | 'ADMIN'

export interface Gebruiker {
  id: number
  gebruikersnaam: string
  email: string
  voornaam: string
  achternaam: string
  rol: Rol
}

export interface AuthAntwoord {
  token: string
  gebruiker: Gebruiker
}

export interface Registratie {
  gebruikersnaam: string
  email: string
  voornaam: string
  achternaam: string
  wachtwoord: string
}

export interface GekochtBoek {
  isbn: string
  titel: string
  aantal: number
  laatstGekochtOp: string
  reviewScore: number | null
  reviewTekst: string | null
}

export interface Profiel extends Gebruiker {
  gekochteBoeken: GekochtBoek[]
}

export interface ProfielWijziging {
  voornaam: string
  achternaam: string
  email: string
}

export type ReservatieStatus = 'GERESERVEERD' | 'BEVESTIGD' | 'GEANNULEERD'

export interface ReservatieLijn {
  isbn: string
  titel: string
  prijs: number
  aantal: number
  subtotaal: number
}

export interface Reservatie {
  id: number
  status: ReservatieStatus
  aangemaaktOp: string
  bevestigdOp: string | null
  afhaalDatum: string | null
  totaal: number
  klant: { id: number; gebruikersnaam: string; naam: string; email: string }
  lijnen: ReservatieLijn[]
}

export interface Melding {
  id: number
  tekst: string
  aangemaaktOp: string
  gelezen: boolean
  reservatieId: number | null
}
