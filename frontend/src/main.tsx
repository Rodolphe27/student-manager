import { StrictMode } from 'react'
import { createRoot } from 'react-dom/client'
import './index.css'
import App from './App.tsx'
import { AuthProvider } from './context/AuthContext.tsx'

// TODO(FE-7) [MEDIUM]: no global error boundary — an uncaught render error in any page blanks
// the whole app with no recovery UI. Wrap <App /> in a top-level React error boundary component.
createRoot(document.getElementById('root')!).render(
  <StrictMode>
    <AuthProvider>
      <App />
    </AuthProvider>
  </StrictMode>,
)
