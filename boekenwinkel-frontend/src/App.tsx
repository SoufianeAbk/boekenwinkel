import { BrowserRouter, Link, Route, Routes } from 'react-router-dom'
import { BeveiligdeRoute } from './components/BeveiligdeRoute'
import { Layout } from './components/Layout'
import { AuthProvider } from './context/AuthContext'
import { MandjeProvider } from './context/MandjeContext'
import { MeldingenProvider } from './context/MeldingenContext'
import { BeheerPagina } from './pages/BeheerPagina'
import { BoekDetailPagina } from './pages/BoekDetailPagina'
import { CatalogusPagina } from './pages/CatalogusPagina'
import { LoginPagina } from './pages/LoginPagina'
import { MandjePagina } from './pages/MandjePagina'
import { ProfielPagina } from './pages/ProfielPagina'
import { RegistreerPagina } from './pages/RegistreerPagina'

export function App() {
  return (
    <BrowserRouter>
      <AuthProvider>
        <MeldingenProvider>
          <MandjeProvider>
            <Routes>
              <Route element={<Layout />}>
                <Route index element={<CatalogusPagina />} />
                <Route path="boeken/:isbn" element={<BoekDetailPagina />} />
                <Route path="mandje" element={<MandjePagina />} />
                <Route path="login" element={<LoginPagina />} />
                <Route path="registreer" element={<RegistreerPagina />} />
                <Route
                  path="profiel"
                  element={
                    <BeveiligdeRoute>
                      <ProfielPagina />
                    </BeveiligdeRoute>
                  }
                />
                <Route
                  path="beheer"
                  element={
                    <BeveiligdeRoute enkelAdmin>
                      <BeheerPagina />
                    </BeveiligdeRoute>
                  }
                />
                <Route
                  path="*"
                  element={
                    <div className="leeg">
                      <h2>Pagina niet gevonden</h2>
                      <Link to="/">Terug naar de catalogus</Link>
                    </div>
                  }
                />
              </Route>
            </Routes>
          </MandjeProvider>
        </MeldingenProvider>
      </AuthProvider>
    </BrowserRouter>
  )
}
