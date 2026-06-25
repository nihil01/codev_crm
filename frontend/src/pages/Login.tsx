import React, { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { motion } from 'framer-motion';
import { useAuth } from '../context/AuthContext';
import { useTheme } from '../context/ThemeContext';
import { Lock, Mail, AlertCircle, Eye, EyeOff, ArrowRight, GraduationCap, Sun, Moon } from 'lucide-react';
import toast from 'react-hot-toast';

export const Login: React.FC = () => {
  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');
  const [error, setError] = useState('');
  const [loading, setLoading] = useState(false);
  const [showPassword, setShowPassword] = useState(false);
  const { login } = useAuth();
  const { theme, toggleTheme } = useTheme();
  const navigate = useNavigate();
  const isDark = theme === 'dark';

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setError('');
    setLoading(true);
    try {
      await login(email.trim(), password);
      toast.success('Giriş uğurlu oldu!');
      navigate('/');
    } catch (err: any) {
      setError(err?.response?.data?.message || err?.message || 'E-poçt və ya şifrə yanlışdır');
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className={`relative flex min-h-screen items-center justify-center overflow-hidden px-4 py-8 transition-colors duration-300 ${isDark ? 'bg-[#070b14]' : 'bg-slate-100'}`}>
      {/* Background */}
      <div className="pointer-events-none absolute inset-0">
        {isDark && (
          <>
            <div className="absolute inset-0 opacity-[0.045]" style={{ backgroundImage: 'linear-gradient(rgba(255,255,255,0.75) 1px, transparent 1px), linear-gradient(90deg, rgba(255,255,255,0.75) 1px, transparent 1px)', backgroundSize: '36px 36px' }} />
            <div className="absolute -left-40 -top-40 h-[420px] w-[420px] rounded-full bg-indigo-600/15 blur-[110px]" />
            <div className="absolute -bottom-40 -right-40 h-[420px] w-[420px] rounded-full bg-violet-700/15 blur-[110px]" />
          </>
        )}
      </div>

      <motion.div initial={{ opacity: 0, y: 18, scale: 0.98 }} animate={{ opacity: 1, y: 0, scale: 1 }} transition={{ duration: 0.35, ease: 'easeOut' }} className="relative z-10 w-full max-w-[430px]">
        <div className={`rounded-[26px] border p-6 backdrop-blur-xl sm:p-8 transition-colors duration-300 ${isDark ? 'border-slate-800 bg-[#090f20]/95 shadow-[0_28px_80px_rgba(0,0,0,0.5)]' : 'border-slate-200 bg-white/95 shadow-[0_28px_80px_rgba(0,0,0,0.08)]'}`}>
          {/* Theme toggle */}
          <div className="flex justify-end mb-2">
            <button onClick={toggleTheme} className={`p-2 rounded-lg transition-colors ${isDark ? 'text-slate-400 hover:text-yellow-400 hover:bg-slate-800' : 'text-slate-400 hover:text-indigo-600 hover:bg-slate-100'}`}>
              {isDark ? <Sun size={18} /> : <Moon size={18} />}
            </button>
          </div>

          {/* Header */}
          <div className="mb-8 text-center">
            <div className="relative mx-auto mb-5 flex h-16 w-16 items-center justify-center rounded-[20px] border border-white/20 bg-gradient-to-br from-indigo-500 to-violet-700 shadow-lg shadow-violet-700/30">
              <GraduationCap size={30} className="text-white" />
            </div>
            <h1 className={`text-[26px] font-bold tracking-tight transition-colors duration-300 ${isDark ? 'text-white' : 'text-slate-900'}`}>Codev CRM</h1>
            <p className={`mt-2 text-sm transition-colors duration-300 ${isDark ? 'text-slate-400' : 'text-slate-500'}`}>İdarəetmə panelinizə daxil olun</p>
          </div>

          {error && (
            <motion.div initial={{ opacity: 0, y: -8 }} animate={{ opacity: 1, y: 0 }} className="mb-5 flex items-start gap-3 rounded-xl border border-red-500/25 bg-red-500/10 px-4 py-3">
              <AlertCircle size={18} className="mt-0.5 shrink-0 text-red-400" />
              <p className="text-sm leading-5 text-red-300">{error}</p>
            </motion.div>
          )}

          <form onSubmit={handleSubmit} className="space-y-5">
            <div>
              <label className={`mb-2 block text-sm font-semibold transition-colors duration-300 ${isDark ? 'text-slate-200' : 'text-slate-700'}`}>E-poçt ünvanı</label>
              <div className="relative">
                <Mail size={19} className="pointer-events-none absolute left-4 top-1/2 z-10 -translate-y-1/2 text-slate-500" />
                <input id="email" type="email" autoComplete="email" value={email} onChange={(e) => setEmail(e.target.value)} placeholder="Email unvani" required disabled={loading}
                  className={`box-border block h-12 w-full appearance-none rounded-xl py-0 pl-12 pr-4 text-[15px] placeholder:text-slate-500 transition-colors duration-200 outline-none focus:border-indigo-500 focus:outline-none focus:ring-0 disabled:cursor-not-allowed disabled:opacity-60 ${isDark ? 'border border-slate-700 bg-slate-900 text-white hover:border-slate-600' : 'border border-slate-300 bg-slate-50 text-slate-900 hover:border-slate-400'}`} />
              </div>
            </div>

            <div>
              <label className={`mb-2 block text-sm font-semibold transition-colors duration-300 ${isDark ? 'text-slate-200' : 'text-slate-700'}`}>Şifrə</label>
              <div className="relative">
                <Lock size={19} className="pointer-events-none absolute left-4 top-1/2 z-10 -translate-y-1/2 text-slate-500" />
                <input id="password" type={showPassword ? 'text' : 'password'} autoComplete="current-password" value={password} onChange={(e) => setPassword(e.target.value)} placeholder="Şifrənizi daxil edin" required disabled={loading}
                  className={`box-border block h-12 w-full appearance-none rounded-xl py-0 pl-12 pr-12 text-[15px] placeholder:text-slate-500 transition-colors duration-200 outline-none focus:border-indigo-500 focus:outline-none focus:ring-0 disabled:cursor-not-allowed disabled:opacity-60 ${isDark ? 'border border-slate-700 bg-slate-900 text-white hover:border-slate-600' : 'border border-slate-300 bg-slate-50 text-slate-900 hover:border-slate-400'}`} />
                <button type="button" onClick={() => setShowPassword((prev) => !prev)} className="absolute right-2 top-1/2 flex h-9 w-9 -translate-y-1/2 items-center justify-center rounded-lg text-slate-500 transition-colors hover:bg-slate-800 hover:text-slate-200 focus:outline-none">
                  {showPassword ? <EyeOff size={19} /> : <Eye size={19} />}
                </button>
              </div>
            </div>

            <motion.button whileHover={{ scale: loading ? 1 : 1.01 }} whileTap={{ scale: loading ? 1 : 0.985 }} type="submit" disabled={loading}
              className="flex h-12 w-full items-center justify-center gap-2 rounded-xl bg-gradient-to-r from-indigo-600 to-violet-600 text-[15px] font-semibold text-white shadow-lg shadow-indigo-700/25 transition-all hover:from-indigo-500 hover:to-violet-500 hover:shadow-indigo-700/40 disabled:cursor-not-allowed disabled:opacity-60">
              {loading ? (
                <><span className="h-4 w-4 animate-spin rounded-full border-2 border-white/30 border-t-white" />Giriş edilir...</>
              ) : (
                <>{isDark ? 'Daxil ol' : 'Daxil ol'}<ArrowRight size={18} /></>
              )}
            </motion.button>
          </form>
        </div>

        <p className={`mt-5 text-center text-xs transition-colors duration-300 ${isDark ? 'text-slate-600' : 'text-slate-400'}`}>
          © {new Date().getFullYear()} Codev CRM
        </p>
      </motion.div>
    </div>
  );
};
