import { Navigate } from 'react-router-dom';
import { useAuth } from '../context/useAuth';

interface ProtectedRouteProps {
  children: React.ReactNode;
}

// TODO(FE-6) [HIGH]: only checks isAuthenticated, never role — /students, /teachers, /courses,
// /enrollments are reachable by any logged-in user (e.g. a STUDENT) by navigating directly to
// the URL; Sidebar.tsx only hides the nav links. Add a role-aware guard (e.g. accept an allowed
// `roles` prop) as defense-in-depth — this must never replace server-side authorization.
export default function ProtectedRoute({ children }: ProtectedRouteProps) {
  const { isAuthenticated, loading } = useAuth();

  // Still checking localStorage
  if (loading) {
    return (
      <div className="flex items-center justify-center min-h-screen">
        <div className="w-8 h-8 border-4 border-blue-600 border-t-transparent rounded-full animate-spin" />
      </div>
    );
  }

  // Not logged in → redirect to login
  if (!isAuthenticated) {
    return <Navigate to="/login" replace />;
  }

  return <>{children}</>;
}
