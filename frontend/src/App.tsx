import { BrowserRouter, Routes, Route, Navigate } from 'react-router-dom';
import { useAuth } from './context/useAuth';
import Layout from './components/Layout';
import ProtectedRoute from './components/ProtectedRoute';
import LoginPage from './pages/LoginPage';
import Dashboard from './pages/Dashboard';
import StudentsPage from './pages/StudentsPage';
import TeachersPage from './pages/TeachersPage';
import CoursesPage from './pages/CoursesPage';
import EnrollmentsPage from './pages/EnrollmentsPage';
import MyCoursesPage from './pages/MyCoursesPage';
import ProfilePage from './pages/ProfilePage';

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
          {/* Backend restricts these rosters' GET to TEACHER/ADMIN — mirror it here so a
              STUDENT navigating directly to the URL gets redirected instead of an API 403. */}
          <Route path="students"    element={<ProtectedRoute roles={['TEACHER', 'ADMIN']}><StudentsPage /></ProtectedRoute>} />
          <Route path="teachers"    element={<ProtectedRoute roles={['TEACHER', 'ADMIN']}><TeachersPage /></ProtectedRoute>} />
          {/* Course catalogue GET is open to any authenticated role (see SecurityConfig) — no restriction. */}
          <Route path="courses"     element={<CoursesPage />} />
          <Route path="enrollments" element={<ProtectedRoute roles={['TEACHER', 'ADMIN']}><EnrollmentsPage /></ProtectedRoute>} />
          <Route path="my-courses" element={<MyCoursesPage />} />
          {/* Self-service: every role (including ADMIN) edits their own account here. */}
          <Route path="profile"    element={<ProfilePage />} />
        </Route>

        {/* Fallback */}
        <Route path="*" element={<Navigate to="/" replace />} />

      </Routes>
    </BrowserRouter>
  );
}
