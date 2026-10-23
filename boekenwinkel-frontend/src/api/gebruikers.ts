import { api } from './client'
import type {
  AuthAntwoord,
  GekochtBoek,
  Melding,
  Profiel,
  ProfielWijziging,
  Registratie,
  Reservatie,
  ReservatieStatus,
} from './types'

// Endpoints van de gebruikers-service (poort 8082, via de Vite-proxy)

export const authApi = {
  login: (gebruikersnaam: string, wachtwoord: string) =>
    api<AuthAntwoord>('/api/auth/login', { methode: 'POST', body: { gebruikersnaam, wachtwoord } }),

  registreer: (gegevens: Registratie) =>
    api<AuthAntwoord>('/api/auth/registreer', { methode: 'POST', body: gegevens }),
}

export const profielApi = {
  haal: () => api<Profiel>('/api/profiel'),

  wijzig: (wijziging: ProfielWijziging) => api<Profiel>('/api/profiel', { methode: 'PUT', body: wijziging }),

  schrijfReview: (isbn: string, score: number, tekst: string) =>
    api<GekochtBoek>(`/api/profiel/gekochte-boeken/${encodeURIComponent(isbn)}/review`, {
      methode: 'POST',
      body: { score, tekst },
    }),

  meldingen: () => api<Melding[]>('/api/profiel/meldingen'),

  markeerMeldingenGelezen: () => api<void>('/api/profiel/meldingen/gelezen', { methode: 'POST' }),
}

export const reservatieApi = {
  mijnReservaties: () => api<Reservatie[]>('/api/reservaties'),

  reserveer: (lijnen: { isbn: string; aantal: number }[]) =>
    api<Reservatie>('/api/reservaties', { methode: 'POST', body: { lijnen } }),

  annuleer: (id: number) => api<Reservatie>(`/api/reservaties/${id}/annuleren`, { methode: 'POST' }),
}

export const beheerApi = {
  reservaties: (status?: ReservatieStatus) =>
    api<Reservatie[]>(status ? `/api/admin/reservaties?status=${status}` : '/api/admin/reservaties'),

  bevestig: (id: number, afhaalDatum: string) =>
    api<Reservatie>(`/api/admin/reservaties/${id}/bevestigen`, { methode: 'POST', body: { afhaalDatum } }),
}
