import { NavLink } from 'react-router-dom';
import { useAuth } from '../context/AuthContext';

interface NavItem {
  path: string;
  label: string;
  icon: string;
}

const adminNavItems: NavItem[] = [
  { path: '/',            label: 'Dashboard',   icon: '▣' },
  { path: '/students',    label: 'Students',    icon: '👤' },
  { path: '/courses',     label: 'Courses',     icon: '📚' },
  { path: '/enrollments', label: 'Enrollments', icon: '📋' },
];

const studentNavItems: NavItem[] = [
  { path: '/',            label: 'Dashboard',   icon: '▣' },
  { path: '/my-courses',  label: 'My Courses',  icon: '📋' },
];

interface SidebarProps {
  open: boolean;
  onNavigate: () => void;
}

export default function Sidebar({ open, onNavigate }: SidebarProps) {
  const { user, logout } = useAuth();
  const navItems = user?.role === 'STUDENT' ? studentNavItems : adminNavItems;

  return (
    <div
      className={`w-56 min-h-screen bg-slate-900 flex flex-col fixed inset-y-0 left-0 z-40 transform transition-transform duration-200 ease-in-out
        ${open ? 'translate-x-0' : '-translate-x-full'} md:translate-x-0 md:static md:z-auto`}
    >

      {/* Logo */}
      <div className="flex items-center gap-3 px-4 py-5 border-b border-slate-700">
        <div className="w-9 h-9 bg-blue-600 rounded-lg flex items-center justify-center text-white font-bold text-sm">
          SM
        </div>
        <span className="text-white font-semibold text-sm">StudentManager</span>
      </div>

      {/* Nav */}
      <nav className="flex-1 px-3 py-4 space-y-1">
        {navItems.map((item: NavItem) => (
          <NavLink
            key={item.path}
            to={item.path}
            end={item.path === '/'}
            onClick={onNavigate}
            className={({ isActive }: { isActive: boolean }) =>
              `flex items-center gap-3 px-3 py-2 rounded-lg text-sm transition-colors ${
                isActive
                  ? 'bg-blue-600 text-white font-semibold'
                  : 'text-slate-400 hover:text-white hover:bg-slate-800'
              }`
            }
          >
            <span>{item.icon}</span>
            <span>{item.label}</span>
          </NavLink>
        ))}
      </nav>

      {/* User + Logout */}
      <div className="px-3 py-4 border-t border-slate-700 space-y-2">
        <div className="flex items-center gap-3 px-3 py-2 bg-slate-800 rounded-lg">
          <div className="w-7 h-7 bg-blue-600 rounded-full flex items-center justify-center text-white text-xs font-bold">
            {user?.username?.charAt(0).toUpperCase()}
          </div>
          <div className="flex-1 min-w-0">
            <p className="text-white text-xs font-medium truncate">{user?.username}</p>
            <p className="text-slate-400 text-xs">{user?.role}</p>
          </div>
        </div>
        <button
          onClick={logout}
          className="w-full text-slate-400 hover:text-white hover:bg-slate-800 text-xs px-3 py-2 rounded-lg transition-colors text-left"
        >
          Sign out
        </button>
      </div>
    </div>
  );
}
