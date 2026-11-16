import React, { useState } from 'react';
import { Link, useLocation, useNavigate } from 'react-router-dom';
import { motion, AnimatePresence } from 'framer-motion';
import { useAuth } from '../context/AuthContext';
import { useTheme } from '../context/ThemeContext';
import {
  LayoutDashboard,
  Users,
  GraduationCap,
  Layers,
  CreditCard,
  Receipt,
  BarChart3,
  Award,
  TrendingUp,
  LogOut,
  Menu,
  X,
  ChevronDown,
  Sun,
  Moon,
  UserCog,
  ClipboardList,
  History,
  MessageSquareText,
  ChartNoAxesCombined,
  Target,
  CalendarOff,
  Repeat2,
} from 'lucide-react';

const navItems = [
  { path: '/', label: 'Dashboard', icon: LayoutDashboard },
  { path: '/leads', label: 'Leadlər', icon: ClipboardList },
  { path: '/reminders', label: 'Xatırlatmalar', icon: MessageSquareText },
  { path: '/freezes', label: 'Dondurulmuş aylar', icon: CalendarOff },
  { path: '/students', label: 'Tələbələr', icon: Users },
  { path: '/teachers', label: 'Müəllimlər', icon: GraduationCap },
  { path: '/groups', label: 'Qruplar', icon: Layers },
  { path: '/payments', label: 'Ödənişlər', icon: CreditCard },
  { path: '/expenses', label: 'Xərclər', icon: Receipt },
  { path: '/reports', label: 'Hesabatlar', icon: BarChart3 },
  { path: '/forecast', label: 'Proqnoz', icon: TrendingUp },
  { path: '/forecast/scenarios', label: 'Ssenarilər', icon: Target },
  { path: '/analytics', label: 'Analitika', icon: ChartNoAxesCombined },
  { path: '/managers', label: 'Menecerlər', icon: UserCog },
  { path: '/audit', label: 'Dəyişiklik tarixçəsi', icon: History },
  { path: '/certificates', label: 'Sertifikatlar', icon: Award },
];

export const Layout: React.FC<{ children: React.ReactNode }> = ({ children }) => {
  const [sidebarOpen, setSidebarOpen] = useState(true);
  const [mobileOpen, setMobileOpen] = useState(false);
  const location = useLocation();
  const navigate = useNavigate();
  const { user, logout } = useAuth();
  const { theme, toggleTheme } = useTheme();

  const isDark = theme === 'dark';

  const handleLogout = async () => {
    await logout();
    navigate('/login');
  };

  const sidebarBg = isDark ? 'bg-slate-900' : 'bg-white';
  const sidebarBorder = isDark ? 'border-slate-700/50' : 'border-slate-200';
  const headerBg = isDark ? 'bg-slate-900/80' : 'bg-white/80';
  const navActive = isDark ? 'bg-indigo-600/20 text-indigo-400' : 'bg-indigo-50 text-indigo-600';
  const navHover = isDark ? 'hover:bg-slate-800 hover:text-white' : 'hover:bg-slate-100 hover:text-slate-900';
  const navText = isDark ? 'text-slate-400' : 'text-slate-500';
  const logoText = isDark ? 'text-white' : 'text-slate-900';
  const userBg = isDark ? 'bg-slate-700' : 'bg-slate-200';
  const userText = isDark ? 'text-white' : 'text-slate-900';
  const userSubtext = isDark ? 'text-slate-400' : 'text-slate-500';

  return (
    <div className="flex h-screen overflow-hidden transition-colors duration-300">
      {/* Sidebar */}
      <motion.aside
        initial={false}
        animate={{ width: sidebarOpen ? 256 : 72 }}
        className={`hidden lg:flex flex-col ${sidebarBg} border-r ${sidebarBorder} h-full transition-colors duration-300`}
      >
        <div className={`h-16 flex items-center px-4 border-b ${sidebarBorder} transition-colors duration-300`}>
          <div className="w-8 h-8 bg-indigo-600 rounded-lg flex items-center justify-center font-bold text-white">T</div>
          {sidebarOpen && (
            <motion.span initial={{ opacity: 0 }} animate={{ opacity: 1 }} className={`ml-3 font-semibold ${logoText} whitespace-nowrap transition-colors duration-300`}>
              Codev CRM
            </motion.span>
          )}
        </div>

        <nav className="flex-1 py-4 px-2 space-y-1 overflow-y-auto">
          {navItems.filter((item) => user?.role === 'ADMIN' || !['/', '/expenses', '/reports', '/forecast', '/certificates', '/managers', '/audit', '/forecast/scenarios', '/analytics', '/expense-rules'].includes(item.path)).map((item) => {
            const isActive = location.pathname === item.path;
            return (
              <Link
                key={item.path}
                to={item.path}
                className={`flex items-center gap-3 px-3 py-2.5 rounded-lg transition-all duration-200 ${isActive ? navActive : `${navText} ${navHover}`}`}
              >
                <item.icon size={20} />
                {sidebarOpen && <span className="whitespace-nowrap">{item.label}</span>}
              </Link>
            );
          })}
        </nav>

        <div className={`p-4 border-t ${sidebarBorder} transition-colors duration-300`}>
          <div className="flex items-center gap-3">
            <div className={`w-8 h-8 ${userBg} rounded-full flex items-center justify-center text-sm font-medium transition-colors duration-300`}>
              {user?.fullName?.charAt(0)}
            </div>
            {sidebarOpen && (
              <div className="flex-1 min-w-0">
                <p className={`text-sm font-medium ${userText} truncate transition-colors duration-300`}>{user?.fullName}</p>
                <p className={`text-xs ${userSubtext} transition-colors duration-300`}>{user?.role}</p>
              </div>
            )}
            {sidebarOpen && (
              <button onClick={handleLogout} className="p-1.5 text-slate-400 hover:text-red-400 hover:bg-slate-800 rounded-lg transition-colors">
                <LogOut size={16} />
              </button>
            )}
          </div>
        </div>

        <button
          onClick={() => setSidebarOpen(!sidebarOpen)}
          className={`absolute -right-3 top-20 w-6 h-6 ${isDark ? 'bg-slate-700 border-slate-600' : 'bg-slate-200 border-slate-300'} border rounded-full flex items-center justify-center ${navText} hover:text-indigo-500 transition-colors`}
        >
          <ChevronDown size={14} className={`transition-transform ${sidebarOpen ? 'rotate-90' : '-rotate-90'}`} />
        </button>
      </motion.aside>

      {/* Mobile sidebar */}
      <AnimatePresence>
        {mobileOpen && (
          <>
            <motion.div initial={{ opacity: 0 }} animate={{ opacity: 1 }} exit={{ opacity: 0 }} className="fixed inset-0 bg-black/50 z-40 lg:hidden" onClick={() => setMobileOpen(false)} />
            <motion.aside initial={{ x: -256 }} animate={{ x: 0 }} exit={{ x: -256 }} className={`fixed left-0 top-0 w-64 h-full ${sidebarBg} z-50 lg:hidden transition-colors duration-300`}>
              <div className={`h-16 flex items-center justify-between px-4 border-b ${sidebarBorder} transition-colors duration-300`}>
                <span className={`font-semibold ${logoText} transition-colors duration-300`}>Codev CRM</span>
                <button onClick={() => setMobileOpen(false)} className={navText}>
                  <X size={20} />
                </button>
              </div>
              <nav className="py-4 px-2 space-y-1">
                {navItems.filter((item) => user?.role === 'ADMIN' || !['/', '/expenses', '/reports', '/forecast', '/certificates', '/managers', '/audit', '/forecast/scenarios', '/analytics', '/expense-rules'].includes(item.path)).map((item) => {
                  const isActive = location.pathname === item.path;
                  return (
                    <Link key={item.path} to={item.path} onClick={() => setMobileOpen(false)} className={`flex items-center gap-3 px-3 py-2.5 rounded-lg transition-all ${isActive ? navActive : `${navText} ${navHover}`}`}>
                      <item.icon size={20} />
                      <span>{item.label}</span>
                    </Link>
                  );
                })}
              </nav>
            </motion.aside>
          </>
        )}
      </AnimatePresence>

      {/* Main */}
      <div className="flex-1 flex flex-col overflow-hidden">
        <header className={`h-16 ${headerBg} backdrop-blur-sm border-b ${sidebarBorder} flex items-center px-4 lg:px-6 transition-colors duration-300`}>
          <button onClick={() => setMobileOpen(true)} className={`lg:hidden p-2 ${navText}`}>
            <Menu size={20} />
          </button>
          <div className="flex-1" />
          <div className="flex items-center gap-3">
            <button
              onClick={toggleTheme}
              className={`p-2 rounded-lg transition-colors duration-300 ${isDark ? 'text-slate-400 hover:text-yellow-400 hover:bg-slate-800' : 'text-slate-500 hover:text-indigo-600 hover:bg-slate-100'}`}
            >
              {isDark ? <Sun size={18} /> : <Moon size={18} />}
            </button>
            <span className={`text-sm ${userSubtext} transition-colors duration-300`}>
              {new Date().toLocaleDateString('az-AZ', { year: 'numeric', month: 'long', day: 'numeric' })}
            </span>
          </div>
        </header>

        <main className="flex-1 overflow-y-auto p-4 lg:p-6">
          <motion.div key={location.pathname} initial={{ opacity: 0, y: 10 }} animate={{ opacity: 1, y: 0 }} transition={{ duration: 0.2 }}>
            {children}
          </motion.div>
        </main>
      </div>
    </div>
  );
};
