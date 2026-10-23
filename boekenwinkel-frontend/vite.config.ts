import { defineConfig } from 'vite'
import react from '@vitejs/plugin-react'

const CATALOGUS_SERVICE = 'http://localhost:8081'
const GEBRUIKERS_SERVICE = 'http://localhost:8082'

// De dev-server stuurt elk API-pad door naar de juiste backend-service.
// Zo spreekt de frontend één adres aan en zijn er geen CORS-problemen.
export default defineConfig({
  plugins: [react()],
  server: {
    port: 5173,
    proxy: {
      '/api/boeken': CATALOGUS_SERVICE,
      '/api/auth': GEBRUIKERS_SERVICE,
      '/api/profiel': GEBRUIKERS_SERVICE,
      '/api/reservaties': GEBRUIKERS_SERVICE,
      '/api/admin': GEBRUIKERS_SERVICE,
    },
  },
})
