import React from 'react';
import { useQuery } from '@tanstack/react-query';
import { motion } from 'framer-motion';
import { dashboardApi } from '../api/dashboard';
import { useTheme } from '../context/ThemeContext';
import { Users, GraduationCap, Layers, TrendingUp, AlertTriangle, ArrowUpRight, ArrowDownRight } from 'lucide-react';

const monthNames = ['', 'Yanvar', 'Fevral', 'Mart', 'Aprel', 'May', 'İyun', 'İyul', 'Avqust', 'Sentyabr', 'Oktyabr', 'Noyabr', 'Dekabr'];

export const Dashboard: React.FC = () => {
  const { data, isLoading } = useQuery({ queryKey: ['dashboard'], queryFn: dashboardApi.get });
  const { theme } = useTheme();
  const isDark = theme === 'dark';

  if (isLoading) {
    return (
      <div className="flex items-center justify-center h-64">
        <div className="w-8 h-8 border-2 border-indigo-500 border-t-transparent rounded-full animate-spin" />
      </div>
    );
  }
  if (!data) return null;

  const cardBg = isDark ? 'bg-slate-900 border-slate-700/50' : 'bg-white border-slate-200';
  const titleColor = isDark ? 'text-white' : 'text-slate-900';
  const mutedText = isDark ? 'text-slate-400' : 'text-slate-500';
  const rowHover = isDark ? 'hover:bg-slate-800/50' : 'hover:bg-slate-50';
  const rowBorder = isDark ? 'border-slate-800' : 'border-slate-100';
  const progressBg = isDark ? 'bg-slate-800' : 'bg-slate-200';
  const itemBg = isDark ? 'bg-slate-800/50' : 'bg-slate-50';

  const stats = [
    { label: 'Aktiv Tələbələr', value: data.activeStudents, total: data.totalStudents, icon: Users, color: 'text-blue-400', bg: 'bg-blue-500/10' },
    { label: 'Müəllimlər', value: data.totalTeachers, icon: GraduationCap, color: 'text-purple-400', bg: 'bg-purple-500/10' },
    { label: 'Aktiv Qruplar', value: data.activeGroups, icon: Layers, color: 'text-emerald-400', bg: 'bg-emerald-500/10' },
    { label: 'Gecikmiş Ödənişlər', value: data.overdueCount, icon: AlertTriangle, color: 'text-red-400', bg: 'bg-red-500/10' },
  ];

  return (
    <div className="space-y-6">
      <h1 className={`text-2xl font-bold transition-colors duration-300 ${titleColor}`}>Dashboard</h1>

      <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-4">
        {stats.map((stat, i) => (
          <motion.div key={stat.label} initial={{ opacity: 0, y: 20 }} animate={{ opacity: 1, y: 0 }} transition={{ delay: i * 0.1 }}
            className={`${cardBg} border rounded-xl p-5 transition-colors duration-300`}>
            <div className="flex items-center justify-between">
              <div className={`p-2.5 rounded-lg ${stat.bg}`}>
                <stat.icon size={20} className={stat.color} />
              </div>
              {stat.total && <span className={`text-xs ${mutedText}`}>/ {stat.total} cəmi</span>}
            </div>
            <div className="mt-3">
              <p className={`text-2xl font-bold transition-colors duration-300 ${titleColor}`}>{stat.value}</p>
              <p className={`text-sm ${mutedText}`}>{stat.label}</p>
            </div>
          </motion.div>
        ))}
      </div>

      <div className="grid grid-cols-1 lg:grid-cols-3 gap-4">
        <motion.div initial={{ opacity: 0, y: 20 }} animate={{ opacity: 1, y: 0 }} transition={{ delay: 0.4 }} className={`${cardBg} border rounded-xl p-5 transition-colors duration-300`}>
          <div className="flex items-center gap-2 mb-2">
            <ArrowUpRight size={18} className="text-emerald-400" />
            <span className={`text-sm ${mutedText}`}>Bu ay faktiki mədaxil (kassa)</span>
          </div>
          <p className="text-3xl font-bold text-emerald-400">{data.currentMonthCollected?.toLocaleString()} AZN</p>
        </motion.div>
        <motion.div initial={{ opacity: 0, y: 20 }} animate={{ opacity: 1, y: 0 }} transition={{ delay: 0.5 }} className={`${cardBg} border rounded-xl p-5 transition-colors duration-300`}>
          <div className="flex items-center gap-2 mb-2">
            <ArrowDownRight size={18} className="text-blue-400" />
            <span className={`text-sm ${mutedText}`}>Bu ay üzrə hesablanıb</span>
          </div>
          <p className="text-3xl font-bold text-blue-400">{data.currentMonthExpected?.toLocaleString()} AZN</p>
          <p className={`text-xs mt-2 ${mutedText}`}>Bu ayın hesablarına ödənilib: {data.currentMonthPaidAgainstExpected?.toLocaleString()} AZN</p>
          <p className={`text-xs ${mutedText}`}>Ödənilməmiş qalıq: {data.currentMonthOutstanding?.toLocaleString()} AZN</p>
        </motion.div>
        <motion.div initial={{ opacity: 0, y: 20 }} animate={{ opacity: 1, y: 0 }} transition={{ delay: 0.6 }} className={`${cardBg} border rounded-xl p-5 transition-colors duration-300`}>
          <div className="flex items-center gap-2 mb-2">
            <TrendingUp size={18} className="text-indigo-400" />
            <span className={`text-sm ${mutedText}`}>Bu ayın hesabları üzrə ödəniş faizi</span>
          </div>
          <p className="text-3xl font-bold text-indigo-400">{data.collectionRate?.toFixed(1)}%</p>
          <div className={`mt-2 w-full rounded-full h-2 ${progressBg} transition-colors duration-300`}>
            <motion.div initial={{ width: 0 }} animate={{ width: `${Math.min(data.collectionRate || 0, 100)}%` }} transition={{ duration: 1, delay: 0.8 }} className="h-2 bg-indigo-500 rounded-full" />
          </div>
        </motion.div>
      </div>

      {data.groupSummaries && data.groupSummaries.length > 0 && (
        <motion.div initial={{ opacity: 0, y: 20 }} animate={{ opacity: 1, y: 0 }} transition={{ delay: 0.7 }} className={`${cardBg} border rounded-xl p-5 transition-colors duration-300`}>
          <h2 className={`text-lg font-semibold mb-4 transition-colors duration-300 ${titleColor}`}>Qruplar Üzrə İcmal</h2>
          <div className="overflow-x-auto">
            <table className="w-full text-sm">
              <thead>
                <tr className={`${mutedText} border-b ${rowBorder} transition-colors duration-300`}>
                  <th className="text-left py-2">Qrup</th>
                  <th className="text-left py-2">Tələbələr</th>
                  <th className="text-right py-2">Toplanmış</th>
                  <th className="text-right py-2">Müəllim Payı</th>
                  <th className="text-right py-2">Xalis Qazanc</th>
                </tr>
              </thead>
              <tbody>
                {data.groupSummaries.map((gs) => (
                  <tr key={gs.groupId} className={`border-b ${rowBorder} ${rowHover} transition-colors duration-300`}>
                    <td className={`py-3 font-medium transition-colors duration-300 ${titleColor}`}>{gs.groupNumber}</td>
                    <td className={`py-3 ${mutedText}`}>{gs.studentCount}</td>
                    <td className="py-3 text-right text-emerald-400">{gs.collected?.toLocaleString()} AZN</td>
                    <td className="py-3 text-right text-orange-400">{gs.teacherShare?.toLocaleString()} AZN</td>
                    <td className={`py-3 text-right font-medium transition-colors duration-300 ${titleColor}`}>{gs.netProfit?.toLocaleString()} AZN</td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        </motion.div>
      )}

      {data.recentPayments && data.recentPayments.length > 0 && (
        <motion.div initial={{ opacity: 0, y: 20 }} animate={{ opacity: 1, y: 0 }} transition={{ delay: 0.8 }} className={`${cardBg} border rounded-xl p-5 transition-colors duration-300`}>
          <h2 className={`text-lg font-semibold mb-4 transition-colors duration-300 ${titleColor}`}>Bu Ayın Hesabları (mədaxil əməliyyatları deyil)</h2>
          <div className="space-y-2">
            {data.recentPayments.slice(0, 5).map((p) => (
              <div key={p.id} className={`flex items-center justify-between p-3 rounded-lg transition-colors duration-300 ${itemBg}`}>
                <div>
                  <p className={`text-sm font-medium transition-colors duration-300 ${titleColor}`}>{p.studentFirstName} {p.studentLastName}</p>
                  <p className={`text-xs ${mutedText}`}>{p.groupNumber}</p>
                </div>
                <div className="text-right">
                  <p className="text-sm font-medium text-emerald-400">{p.amount?.toLocaleString()} AZN</p>
                  <p className={`text-xs ${mutedText}`}>{monthNames[p.paymentMonth]} {p.paymentYear}</p>
                </div>
              </div>
            ))}
          </div>
        </motion.div>
      )}
    </div>
  );
};
