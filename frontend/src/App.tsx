import { BrowserRouter, Routes, Route, Navigate, useSearchParams } from 'react-router-dom';
import { useAuth } from './context/useAuth';
import Layout from './components/Layout';
import ProtectedRoute from './components/ProtectedRoute';
import LoginPage from './pages/LoginPage';
import RegisterPage from './pages/RegisterPage';
import Dashboard from './pages/Dashboard';
import StudentsPage from './pages/StudentsPage';
import TeachersPage from './pages/TeachersPage';
import CoursesPage from './pages/CoursesPage';
import EnrollmentsPage from './pages/EnrollmentsPage';
import MyCoursesPage from './pages/MyCoursesPage';

// A bare /register visited while already logged in bounces to the dashboard —
// there's nothing useful to do there. But an invite link (?code=...) is a
// deliberate request to create a *different* account (e.g. the admin who just
// issued the invite clicking their own link to test it), so let that through
// even while logged in. AuthProvider.register() simply overwrites the stored
// session with the new one, so switching accounts this way is safe.
// (Needs its own component: useSearchParams requires Router context, which
// isn't available yet in App's own render — only inside <BrowserRouter>.)
function RegisterRoute() {
  const { isAuthenticated } = useAuth();
  const [searchParams] = useSearchParams();

  if (isAuthenticated && !searchParams.has('code')) {
    return <Navigate to="/" replace />;
  }
  return <RegisterPage />;
}

export default function App() {
  const { isAuthenticated } = useAuth();

  return (
    <BrowserRouter>
      <Routes>

        {/* Public routes */}
        <Route
          path="/login"
          element={isAuthenticated ? <Navigate to="/" replace /> : <LoginPage />}
        />
        <Route path="/register" element={<RegisterRoute />} />

        {/* Protected routes */}
        <Route
          path="/"
          element={
            <ProtectedRoute>
              <Layout />
            </ProtectedRoute>
          }
        >
          <Route index          element={<Dashboard />} />
          <Route path="students"    element={<StudentsPage />} />
          <Route path="teachers"    element={<TeachersPage />} />
          <Route path="courses"     element={<CoursesPage />} />
          <Route path="enrollments" element={<EnrollmentsPage />} />
          <Route path="my-courses" element={<MyCoursesPage />} />
        </Route>

        {/* Fallback */}
        <Route path="*" element={<Navigate to="/" replace />} />

      </Routes>
    </BrowserRouter>
  );
}
