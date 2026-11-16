import React from 'react';
import { useQuery } from '@tanstack/react-query';
import { motion } from 'framer-motion';
import { dashboardApi } from '../api/dashboard';
import { useTheme } from '../context/ThemeContext';
import { Users, GraduationCap, Layers, AlertTriangle } from 'lucide-react';

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

      {/* Invoiced-month metrics and real cash are distinct accounting periods. */}
      <div className="grid grid-cols-1 sm:grid-cols-2 xl:grid-cols-3 gap-4">
        {([
          { label: 'Hesablanıb', value: data.currentMonthExpected, color: 'text-blue-400',
            note: 'Bu ayın endirimlərdən sonrakı hesabları' },
          { label: 'Hesablara ödənilib', value: data.currentMonthPaidAgainstExpected, color: 'text-emerald-400',
            note: 'Aşağıdakı “Hesablara ödənilib” sütununun cəmi' },
          { label: 'Qalıq', value: data.currentMonthOutstanding, color: 'text-amber-400',
            note: 'Bu ayın hesabları üzrə ödənilməmiş borc' },
          { label: 'Bu ay kassaya daxil olub', value: data.currentMonthCollected, color: 'text-cyan-400',
            note: 'Ödənişin faktiki daxilolma tarixinə görə' },
          { label: 'Müəllim Payı', value: data.currentMonthTeacherShare, color: 'text-orange-400',
            note: 'Bu ayın mədaxilindən hesablanan paylar' },
          { label: 'Müəllim payından sonra', value: data.currentMonthAfterTeacherShare, color: 'text-violet-400',
            note: 'Kassa mədaxili − müəllim payı; digər xərclər xaric' },
        ] as const).map((item, i) => (
          <motion.div key={item.label} initial={{ opacity: 0, y: 15 }} animate={{ opacity: 1, y: 0 }}
            transition={{ delay: i * 0.06 }}
            className={`${cardBg} border rounded-xl p-5 transition-colors duration-300`}>
            <p className={`text-sm mb-2 ${mutedText}`}>{item.label}</p>
            <p className={`text-2xl font-bold tabular-nums ${item.color}`}>
              {Number(item.value ?? 0).toLocaleString('az-AZ', { maximumFractionDigits: 2 })} AZN
            </p>
            <p className={`text-xs mt-2 ${mutedText}`}>{item.note}</p>
          </motion.div>
        ))}
      </div>
      <div className={`${cardBg} border rounded-xl p-4 transition-colors duration-300`}>
        <div className="flex items-center justify-between gap-3 mb-2">
          <span className={`text-sm ${mutedText}`}>Bu ayın hesabları üzrə ödəniş faizi</span>
          <span className="font-semibold text-indigo-400">
            {Number(data.collectionRate ?? 0).toFixed(1)}%
          </span>
        </div>
        <div className={`w-full rounded-full h-2 ${progressBg}`}>
          <div className="h-2 bg-indigo-500 rounded-full transition-all"
            style={{ width: `${Math.max(0, Math.min(Number(data.collectionRate ?? 0), 100))}%` }} />
        </div>
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
                  <th className="text-right py-2">Hesablanıb</th>
                  <th className="text-right py-2">Hesablara ödənilib</th>
                  <th className="text-right py-2">Qalıq</th>
                  <th className="text-right py-2">Bu ay kassaya daxil olub</th>
                  <th className="text-right py-2">Müəllim Payı</th>
                  <th className="text-right py-2">Müəllim payından sonra</th>
                </tr>
              </thead>
              <tbody>
                {data.groupSummaries.map((gs) => (
                  <tr key={gs.groupId} className={`border-b ${rowBorder} ${rowHover} transition-colors duration-300`}>
                    <td className={`py-3 font-medium transition-colors duration-300 ${titleColor}`}>{gs.groupNumber}</td>
                    <td className={`py-3 ${mutedText}`}>{gs.studentCount}</td>
                    <td className="py-3 text-right text-blue-400">{gs.expected?.toLocaleString()} AZN</td>
                    <td className="py-3 text-right text-emerald-400">{gs.paidAgainstInvoices?.toLocaleString()} AZN</td>
                    <td className="py-3 text-right text-amber-400">{gs.outstanding?.toLocaleString()} AZN</td>
                    <td className="py-3 text-right text-emerald-400">{gs.collected?.toLocaleString()} AZN</td>
                    <td className="py-3 text-right text-orange-400">{gs.teacherShare?.toLocaleString()} AZN</td>
                    <td className={`py-3 text-right font-medium transition-colors duration-300 ${titleColor}`}>{gs.netProfit?.toLocaleString()} AZN</td>
                  </tr>
                ))}
              </tbody>
              <tfoot>
                <tr className={`border-t-2 ${rowBorder} font-semibold ${titleColor}`}>
                  <td className="py-3">Cəmi</td>
                  <td className={`py-3 text-xs ${mutedText}`}>Unikal tələbələr yuxarıda</td>
                  <td className="py-3 text-right text-blue-400">{data.currentMonthExpected?.toLocaleString()} AZN</td>
                  <td className="py-3 text-right text-emerald-400">{data.currentMonthPaidAgainstExpected?.toLocaleString()} AZN</td>
                  <td className="py-3 text-right text-amber-400">{data.currentMonthOutstanding?.toLocaleString()} AZN</td>
                  <td className="py-3 text-right text-cyan-400">{data.currentMonthCollected?.toLocaleString()} AZN</td>
                  <td className="py-3 text-right text-orange-400">{data.currentMonthTeacherShare?.toLocaleString()} AZN</td>
                  <td className="py-3 text-right">{data.currentMonthAfterTeacherShare?.toLocaleString()} AZN</td>
                </tr>
              </tfoot>
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
