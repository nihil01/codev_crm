import { useTheme } from '../context/ThemeContext';

export function useThemeClasses() {
  const { theme } = useTheme();
  const isDark = theme === 'dark';

  return {
    isDark,
    // Backgrounds
    pageBg: isDark ? '' : 'bg-slate-100',
    cardBg: isDark ? 'bg-slate-900 border-slate-700/50' : 'bg-white border-slate-200',
    inputBg: isDark ? 'bg-slate-800 border-slate-700 text-white placeholder-slate-500' : 'bg-white border-slate-300 text-slate-900 placeholder-slate-400',
    inputBgModal: isDark ? 'bg-slate-800 border-slate-700 text-white' : 'bg-white border-slate-300 text-slate-900',
    itemBg: isDark ? 'bg-slate-800/50' : 'bg-slate-50',
    progressBg: isDark ? 'bg-slate-800' : 'bg-slate-200',

    // Text
    title: isDark ? 'text-white' : 'text-slate-900',
    text: isDark ? 'text-white' : 'text-slate-900',
    muted: isDark ? 'text-slate-400' : 'text-slate-500',
    subtext: isDark ? 'text-slate-300' : 'text-slate-600',
    label: isDark ? 'text-slate-400' : 'text-slate-600',

    // Borders
    border: isDark ? 'border-slate-700/50' : 'border-slate-200',
    borderStrong: isDark ? 'border-slate-700' : 'border-slate-300',
    rowBorder: isDark ? 'border-slate-800' : 'border-slate-100',
    rowHover: isDark ? 'hover:bg-slate-800/50' : 'hover:bg-slate-50',

    // Actions
    cancelBtn: isDark ? 'bg-slate-800 hover:bg-slate-700 text-white' : 'bg-slate-100 hover:bg-slate-200 text-slate-700',
    iconBtn: isDark ? 'text-slate-400 hover:text-white hover:bg-slate-700' : 'text-slate-400 hover:text-slate-900 hover:bg-slate-100',
    iconBtnDanger: isDark ? 'text-slate-400 hover:text-red-400 hover:bg-slate-700' : 'text-slate-400 hover:text-red-500 hover:bg-red-50',
    iconBtnWarn: isDark ? 'text-slate-400 hover:text-yellow-400 hover:bg-slate-700' : 'text-slate-400 hover:text-yellow-500 hover:bg-yellow-50',

    // Modal
    modalBg: isDark ? 'bg-slate-900 border-slate-700' : 'bg-white border-slate-200',
    overlay: 'bg-black/50',

    // Table
    thClass: isDark ? 'text-slate-400 border-b border-slate-700/50' : 'text-slate-500 border-b border-slate-200',
    tdClass: isDark ? 'text-white' : 'text-slate-900',
    tdMuted: isDark ? 'text-slate-300' : 'text-slate-600',
    tdSubtext: isDark ? 'text-slate-400' : 'text-slate-500',
  };
}
