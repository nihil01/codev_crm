import { useState } from 'react';
import { useQuery } from '@tanstack/react-query';
import { motion } from 'framer-motion';
import { reportsApi } from '../api/reports';
import { useThemeClasses } from '../lib/theme';
import { BarChart, Bar, XAxis, YAxis, CartesianGrid, Tooltip, ResponsiveContainer } from 'recharts';
import { TrendingUp, AlertTriangle, DollarSign, CalendarDays } from 'lucide-react';

const MN = ['', 'Yanvar', 'Fevral', 'Mart', 'Aprel', 'May', 'İyun', 'İyul', 'Avqust', 'Sentyabr', 'Oktyabr', 'Noyabr', 'Dekabr'];

export function Reports() {
  const t = useThemeClasses();
  const now = new Date();
  const cy = now.getFullYear();
  const cm = now.getMonth() + 1;

  const [year, setYear] = useState(cy);
  const [month, setMonth] = useState(cm);
  const [view6, setView6] = useState(false);

  const { data: report, isLoading } = useQuery({
    queryKey: ['report', year, month],
    queryFn: () => reportsApi.getMonthly(year, month),
    enabled: !view6,
  });

  const { data: sixMonths } = useQuery({
    queryKey: ['report6'],
    queryFn: async () => {
      const results: any[] = [];
      for (let i = 5; i >= 0; i--) {
        let m = cm - i, y = cy;
        if (m <= 0) { m += 12; y -= 1; }
        const r = await reportsApi.getMonthly(y, m);
        results.push(r);
      }
      return results;
    },
    enabled: view6,
  });

  const chartData = report?.monthlyComparison?.map((mc: any) => ({
    name: `${MN[mc.month]?.substring(0, 3)} ${mc.year}`,
    Gəlir: mc.income,
    Xərc: mc.expenses,
    'Xalis Qazanc': mc.profit,
  })) || [];

  if (isLoading) return <div className="flex items-center justify-center h-64"><div className="w-8 h-8 border-2 border-indigo-500 border-t-transparent rounded-full animate-spin" /></div>;

  return (
    <div className="space-y-6">
      {/* Header */}
      <div className="flex flex-col sm:flex-row items-start sm:items-center justify-between gap-4">
        <h1 className={`text-2xl font-bold ${t.title}`}>Hesabatlar</h1>
        <div className="flex gap-2 items-center flex-wrap">
          <button onClick={() => reportsApi.exportExcel(year,month)} className="rounded-lg bg-indigo-600 px-3 py-2 text-white">Excel ixrac</button>
          <button onClick={() => setView6(!view6)} className={`flex items-center gap-1.5 px-3 py-2 text-sm font-medium rounded-lg border transition-colors ${view6 ? 'bg-indigo-600 border-indigo-600 text-white' : `${t.cardBg} ${t.border}`}`}>
            <CalendarDays size={16} /> Son 6 ay
          </button>
          {!view6 && (
            <>
              <select value={month} onChange={e => setMonth(+e.target.value)} className={`px-3 py-2 text-sm rounded-lg border ${t.cardBg} ${t.border}`}>
                {MN.slice(1).map((n, i) => <option key={i} value={i + 1}>{n}</option>)}
              </select>
              <select value={year} onChange={e => setYear(+e.target.value)} className={`px-3 py-2 text-sm rounded-lg border ${t.cardBg} ${t.border}`}>
                {[2024, 2025, 2026, 2027].map(y => <option key={y} value={y}>{y}</option>)}
              </select>
            </>
          )}
        </div>
      </div>

      {/* =================== SINGLE MONTH VIEW =================== */}
      {!view6 && report && (
        <>
          {/* Summary Cards */}
          <div className="grid grid-cols-1 sm:grid-cols-3 gap-4">
            <motion.div initial={{ opacity: 0, y: 20 }} animate={{ opacity: 1, y: 0 }} className={`${t.cardBg} border ${t.border} rounded-xl p-5`}>
              <div className="flex items-center gap-2 mb-2"><DollarSign size={18} className="text-emerald-400" /><span className={`text-sm ${t.muted}`}>Bu ay faktiki kassaya daxil olub</span></div>
              <p className="text-2xl font-bold text-emerald-400">{report.totalCollected?.toLocaleString()} AZN</p>
              <p className={`text-xs mt-1 ${t.muted}`}>Bu ay hesablanıb: {report.totalExpected?.toLocaleString()} AZN</p>
              <p className={`text-xs ${t.muted}`}>Bu ayın hesablarına ödənilib: {report.paidAgainstExpected?.toLocaleString()} AZN</p>
              <p className={`text-xs ${t.muted}`}>Həmin hesabların qalığı: {report.outstandingForPeriod?.toLocaleString()} AZN</p>
            </motion.div>
            <motion.div initial={{ opacity: 0, y: 20 }} animate={{ opacity: 1, y: 0 }} transition={{ delay: 0.1 }} className={`${t.cardBg} border ${t.border} rounded-xl p-5`}>
              <div className="flex items-center gap-2 mb-2"><TrendingUp size={18} className="text-indigo-400" /><span className={`text-sm ${t.muted}`}>Bu ayın hesabları üzrə ödənilmə faizi</span></div>
              <p className="text-2xl font-bold text-indigo-400">{report.collectionRate?.toFixed(1)}%</p>
              <div className={`mt-2 w-full rounded-full h-2 ${t.progressBg}`}><div className="h-2 bg-indigo-500 rounded-full" style={{ width: `${Math.min(report.collectionRate || 0, 100)}%` }} /></div>
            </motion.div>
            <motion.div initial={{ opacity: 0, y: 20 }} animate={{ opacity: 1, y: 0 }} transition={{ delay: 0.2 }} className={`${t.cardBg} border ${t.border} rounded-xl p-5`}>
              <div className="flex items-center gap-2 mb-2"><AlertTriangle size={18} className="text-red-400" /><span className={`text-sm ${t.muted}`}>Gecikmiş</span></div>
              <p className="text-2xl font-bold text-red-400">{report.totalOverdueCount || 0}</p>
              <p className={`text-xs mt-1 ${t.muted}`}>Bütün aylardan gecikmiş borc: {report.totalOverdueDebt?.toLocaleString()} AZN</p>
            </motion.div>
          </div>

          {/* Profit */}
          <motion.div initial={{ opacity: 0, y: 20 }} animate={{ opacity: 1, y: 0 }} transition={{ delay: 0.3 }} className={`${t.cardBg} border ${t.border} rounded-xl p-5`}>
            <h2 className={`text-lg font-semibold mb-4 ${t.title}`}>Maliyyə Hesabatı</h2>
            <div className="grid grid-cols-2 lg:grid-cols-3 gap-4">
              <div className={`p-4 rounded-lg ${t.itemBg}`}><p className={`text-sm ${t.muted}`}>Ümumi Gəlir</p><p className="text-xl font-bold text-emerald-400">{report.totalIncome?.toLocaleString()} AZN</p></div>
              <div className={`p-4 rounded-lg ${t.itemBg}`}><p className={`text-sm ${t.muted}`}>Müəllim Payları</p><p className="text-xl font-bold text-orange-400">{report.totalTeacherSalaries?.toLocaleString()} AZN</p></div>
              <div className={`p-4 rounded-lg ${t.itemBg}`}><p className={`text-sm ${t.muted}`}>Daimi Xərclər</p><p className="text-xl font-bold text-yellow-400">{report.totalFixedExpenses?.toLocaleString()} AZN</p></div>
              <div className={`p-4 rounded-lg ${t.itemBg}`}><p className={`text-sm ${t.muted}`}>Ümumi Xərc</p><p className="text-xl font-bold text-red-400">{report.totalExpenses?.toLocaleString()} AZN</p></div>
              <div className={`p-4 rounded-lg border border-indigo-500/30 ${t.itemBg}`}><p className={`text-sm ${t.muted}`}>Xalis Mənfəət</p><p className={`text-xl font-bold ${(report.netProfit || 0) >= 0 ? 'text-emerald-400' : 'text-red-400'}`}>{report.netProfit?.toLocaleString()} AZN</p></div>
            </div>
          </motion.div>

          {/* Chart */}
          {chartData.length > 0 && (
            <motion.div initial={{ opacity: 0, y: 20 }} animate={{ opacity: 1, y: 0 }} transition={{ delay: 0.4 }} className={`${t.cardBg} border ${t.border} rounded-xl p-5`}>
              <h2 className={`text-lg font-semibold mb-4 ${t.title}`}>Aylıq Müqayisə</h2>
              <div className="h-80">
                <ResponsiveContainer width="100%" height="100%">
                  <BarChart data={chartData}>
                    <CartesianGrid strokeDasharray="3 3" stroke={t.isDark ? '#334155' : '#e2e8f0'} />
                    <XAxis dataKey="name" stroke={t.isDark ? '#94a3b8' : '#64748b'} fontSize={12} />
                    <YAxis stroke={t.isDark ? '#94a3b8' : '#64748b'} fontSize={12} />
                    <Tooltip contentStyle={{ backgroundColor: t.isDark ? '#1e293b' : '#ffffff', border: `1px solid ${t.isDark ? '#334155' : '#e2e8f0'}`, borderRadius: '8px', color: t.isDark ? '#f1f5f9' : '#1e293b' }} />
                    <Bar dataKey="Gəlir" fill="#22c55e" radius={[4, 4, 0, 0]} />
                    <Bar dataKey="Xərc" fill="#ef4444" radius={[4, 4, 0, 0]} />
                    <Bar dataKey="Xalis Qazanc" fill="#6366f1" radius={[4, 4, 0, 0]} />
                  </BarChart>
                </ResponsiveContainer>
              </div>
            </motion.div>
          )}

          {/* Overdue */}
          {report.overduePayments && report.overduePayments.length > 0 && (
            <motion.div initial={{ opacity: 0, y: 20 }} animate={{ opacity: 1, y: 0 }} transition={{ delay: 0.5 }} className={`${t.cardBg} border ${t.border} rounded-xl p-5`}>
              <h2 className={`text-lg font-semibold mb-4 ${t.title}`}>Gecikmiş Ödənişlər</h2>
              <div className="space-y-2">
                {report.overduePayments.map((p: any) => (
                  <div key={p.id} className={`flex items-center justify-between p-3 rounded-lg ${t.isDark ? 'bg-red-500/5 border border-red-500/10' : 'bg-red-50 border border-red-100'}`}>
                    <div>
                      <p className={`text-sm font-medium ${t.title}`}>{p.studentFirstName} {p.studentLastName}</p>
                      <p className={`text-xs ${t.muted}`}>{p.groupNumber} - {MN[p.paymentMonth]} {p.paymentYear}</p>
                    </div>
                    <p className="text-sm font-medium text-red-400">{(p.remainingAmount||0).toLocaleString()} AZN</p>
                  </div>
                ))}
              </div>
            </motion.div>
          )}
        </>
      )}

      {/* =================== 6 MONTHS VIEW =================== */}
      {view6 && sixMonths && (
        <>
          {/* 6 month summary cards */}
          <div className="grid grid-cols-2 sm:grid-cols-3 lg:grid-cols-6 gap-3">
            {sixMonths.map((r: any, i: number) => (
              <motion.div key={`${r.year}-${r.month}`} initial={{ opacity: 0, y: 20 }} animate={{ opacity: 1, y: 0 }} transition={{ delay: i * 0.05 }} className={`${t.cardBg} border ${t.border} rounded-xl p-4 text-center`}>
                <p className={`text-xs font-medium ${t.muted}`}>{MN[r.month]} {r.year}</p>
                <p className="text-lg font-bold text-emerald-400 mt-1">{r.totalCollected?.toLocaleString()}</p>
                <p className={`text-xs ${t.muted}`}>toplanıb</p>
                <div className={`mt-2 pt-2 border-t ${t.border}`}>
                  <p className="text-sm font-bold text-red-400">{r.totalExpenses?.toLocaleString()}</p>
                  <p className={`text-xs ${t.muted}`}>xərc</p>
                </div>
              </motion.div>
            ))}
          </div>

          {/* 6 month totals */}
          <motion.div initial={{ opacity: 0, y: 20 }} animate={{ opacity: 1, y: 0 }} transition={{ delay: 0.3 }} className={`${t.cardBg} border ${t.border} rounded-xl p-5`}>
            <h2 className={`text-lg font-semibold mb-4 ${t.title}`}>Son 6 Ayın Ümumi Göstəriciləri</h2>
            <div className="grid grid-cols-2 lg:grid-cols-4 gap-4">
              <div className={`p-4 rounded-lg ${t.itemBg}`}>
                <p className={`text-sm ${t.muted}`}>Ümumi Gəlir</p>
                <p className="text-xl font-bold text-emerald-400">{sixMonths.reduce((s: number, r: any) => s + (r.totalIncome || 0), 0).toLocaleString()} AZN</p>
              </div>
              <div className={`p-4 rounded-lg ${t.itemBg}`}>
                <p className={`text-sm ${t.muted}`}>Ümumi Xərc</p>
                <p className="text-xl font-bold text-red-400">{sixMonths.reduce((s: number, r: any) => s + (r.totalExpenses || 0), 0).toLocaleString()} AZN</p>
              </div>
              <div className={`p-4 rounded-lg ${t.itemBg}`}>
                <p className={`text-sm ${t.muted}`}>Müəllim Payları</p>
                <p className="text-xl font-bold text-orange-400">{sixMonths.reduce((s: number, r: any) => s + (r.totalTeacherSalaries || 0), 0).toLocaleString()} AZN</p>
              </div>
              <div className={`p-4 rounded-lg border border-indigo-500/30 ${t.itemBg}`}>
                <p className={`text-sm ${t.muted}`}>Xalis Mənfəət</p>
                <p className="text-xl font-bold text-indigo-400">{sixMonths.reduce((s: number, r: any) => s + (r.netProfit || 0), 0).toLocaleString()} AZN</p>
              </div>
            </div>
          </motion.div>

          {/* 6 month chart */}
          <motion.div initial={{ opacity: 0, y: 20 }} animate={{ opacity: 1, y: 0 }} transition={{ delay: 0.4 }} className={`${t.cardBg} border ${t.border} rounded-xl p-5`}>
            <h2 className={`text-lg font-semibold mb-4 ${t.title}`}>Son 6 Ayın Müqayisəsi</h2>
            <div className="h-80">
              <ResponsiveContainer width="100%" height="100%">
                <BarChart data={sixMonths.map((r: any) => ({ name: `${MN[r.month]?.substring(0, 3)} ${r.year}`, Gəlir: r.totalIncome || 0, Xərc: r.totalExpenses || 0, 'Xalis Qazanc': r.netProfit || 0 }))}>
                  <CartesianGrid strokeDasharray="3 3" stroke={t.isDark ? '#334155' : '#e2e8f0'} />
                  <XAxis dataKey="name" stroke={t.isDark ? '#94a3b8' : '#64748b'} fontSize={12} />
                  <YAxis stroke={t.isDark ? '#94a3b8' : '#64748b'} fontSize={12} />
                  <Tooltip contentStyle={{ backgroundColor: t.isDark ? '#1e293b' : '#ffffff', border: `1px solid ${t.isDark ? '#334155' : '#e2e8f0'}`, borderRadius: '8px', color: t.isDark ? '#f1f5f9' : '#1e293b' }} />
                  <Bar dataKey="Gəlir" fill="#22c55e" radius={[4, 4, 0, 0]} />
                  <Bar dataKey="Xərc" fill="#ef4444" radius={[4, 4, 0, 0]} />
                  <Bar dataKey="Xalis Qazanc" fill="#6366f1" radius={[4, 4, 0, 0]} />
                </BarChart>
              </ResponsiveContainer>
            </div>
          </motion.div>
        </>
      )}
    </div>
  );
}
