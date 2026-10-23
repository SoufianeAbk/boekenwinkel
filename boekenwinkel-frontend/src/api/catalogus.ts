import { api } from './client'
import type { Boek, BoekDetail, NieuwBoek } from './types'

// Endpoints van de catalogus-service (poort 8081, via de Vite-proxy)

export const catalogusApi = {
  zoekBoeken: (zoekterm?: string) =>
    api<Boek[]>(zoekterm ? `/api/boeken?zoek=${encodeURIComponent(zoekterm)}` : '/api/boeken'),

  haalBoek: (isbn: string) => api<BoekDetail>(`/api/boeken/${encodeURIComponent(isbn)}`),

  voegBoekToe: (boek: NieuwBoek) => api<Boek>('/api/boeken', { methode: 'POST', body: boek }),

  registreerLevering: (isbn: string, aantal: number) =>
    api<Boek>(`/api/boeken/${encodeURIComponent(isbn)}/levering`, { methode: 'POST', body: { aantal } }),
}
