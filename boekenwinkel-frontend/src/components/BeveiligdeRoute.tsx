import type { ReactNode } from 'react'
import { Navigate, useLocation } from 'react-router-dom'
import { useAuth } from '../context/AuthContext'

/** Stuurt niet-ingelogde gebruikers naar de loginpagina, en niet-beheerders weg van beheerpagina's. */
export function BeveiligdeRoute({ children, enkelAdmin = false }: { children: ReactNode; enkelAdmin?: boolean }) {
  const { gebruiker, isAdmin } = useAuth()
  const locatie = useLocation()

  if (!gebruiker) return <Navigate to="/login" replace state={{ van: locatie.pathname + locatie.search }} />
  if (enkelAdmin && !isAdmin) return <Navigate to="/" replace />
  return <>{children}</>
}
