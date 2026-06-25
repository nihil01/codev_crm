import React, { useState } from 'react';
import { useQuery } from '@tanstack/react-query';
import { motion } from 'framer-motion';
import { reportsApi } from '../api/reports';
import { useThemeClasses } from '../lib/theme';
import { BarChart, Bar, XAxis, YAxis, CartesianGrid, Tooltip, ResponsiveContainer } from 'recharts';
import { TrendingUp, AlertTriangle, DollarSign } from 'lucide-react';

const monthNames = ['', 'Yanvar', 'Fevral', 'Mart', 'Aprel', 'May', 'İyun', 'İyul', 'Avqust', 'Sentyabr', 'Oktyabr', 'Noyabr', 'Dekabr'];

export const Reports: React.FC = () => {
  const t = useThemeClasses();
  const now = new Date();
  const [year, setYear] = useState(now.getFullYear());
  const [month, setMonth] = useState(now.getMonth() + 1);

  const { data: report, isLoading } = useQuery({
    queryKey: ['report', year, month],
    queryFn: () => reportsApi.getMonthly(year, month),
  });

  if (isLoading) {
    return <div className="flex items-center justify-center h-64"><div className="w-8 h-8 border-2 border-indigo-500 border-t-transparent rounded-full animate-spin" /></div>;
  }
  if (!report) return null;

  const chartData = report.monthlyComparison?.map((mc) => ({
    name: `${monthNames[mc.month]?.substring(0, 3)} ${mc.year}`,
    Gəlir: mc.income,
    Xərc: mc.expenses,
    'Xalis Qazanc': mc.profit,
  })) || [];

  return (
    <div className="space-y-6">
      <div className="flex flex-col sm:flex-row items-start sm:items-center justify-between gap-4">
        <h1 className={`text-2xl font-bold transition-colors duration-300 ${t.title}`}>Hesabatlar</h1>
        <div className="flex gap-2">
          <select value={month} onChange={(e) => setMonth(+e.target.value)} className={`px-3 py-2 text-sm rounded-lg focus:outline-none focus:ring-2 focus:ring-indigo-500 transition-colors duration-300 ${t.inputBgModal}`}>
            {monthNames.slice(1).map((name, i) => <option key={i + 1} value={i + 1}>{name}</option>)}
          </select>
          <select value={year} onChange={(e) => setYear(+e.target.value)} className={`px-3 py-2 text-sm rounded-lg focus:outline-none focus:ring-2 focus:ring-indigo-500 transition-colors duration-300 ${t.inputBgModal}`}>
            {[2024, 2025, 2026, 2027].map((y) => <option key={y} value={y}>{y}</option>)}
          </select>
        </div>
      </div>

      {/* Payment Summary */}
      <div className="grid grid-cols-1 sm:grid-cols-3 gap-4">
        <motion.div initial={{ opacity: 0, y: 20 }} animate={{ opacity: 1, y: 0 }} className={`${t.cardBg} border rounded-xl p-5 transition-colors duration-300`}>
          <div className="flex items-center gap-2 mb-2"><DollarSign size={18} className="text-emerald-400" /><span className={`text-sm ${t.muted}`}>Toplanmış</span></div>
          <p className="text-2xl font-bold text-emerald-400">{report.totalCollected?.toLocaleString()} AZN</p>
          <p className={`text-xs mt-1 ${t.muted}`}>Gözlənilən: {report.totalExpected?.toLocaleString()} AZN</p>
        </motion.div>
        <motion.div initial={{ opacity: 0, y: 20 }} animate={{ opacity: 1, y: 0 }} transition={{ delay: 0.1 }} className={`${t.cardBg} border rounded-xl p-5 transition-colors duration-300`}>
          <div className="flex items-center gap-2 mb-2"><TrendingUp size={18} className="text-indigo-400" /><span className={`text-sm ${t.muted}`}>Toplama Faizi</span></div>
          <p className="text-2xl font-bold text-indigo-400">{report.collectionRate?.toFixed(1)}%</p>
          <div className={`mt-2 w-full rounded-full h-2 ${t.progressBg} transition-colors duration-300`}>
            <div className="h-2 bg-indigo-500 rounded-full" style={{ width: `${Math.min(report.collectionRate || 0, 100)}%` }} />
          </div>
        </motion.div>
        <motion.div initial={{ opacity: 0, y: 20 }} animate={{ opacity: 1, y: 0 }} transition={{ delay: 0.2 }} className={`${t.cardBg} border rounded-xl p-5 transition-colors duration-300`}>
          <div className="flex items-center gap-2 mb-2"><AlertTriangle size={18} className="text-red-400" /><span className={`text-sm ${t.muted}`}>Gecikmiş</span></div>
          <p className="text-2xl font-bold text-red-400">{report.overduePayments?.length || 0}</p>
        </motion.div>
      </div>

      {/* Profit Report - without teacher salary */}
      <motion.div initial={{ opacity: 0, y: 20 }} animate={{ opacity: 1, y: 0 }} transition={{ delay: 0.3 }} className={`${t.cardBg} border rounded-xl p-5 transition-colors duration-300`}>
        <h2 className={`text-lg font-semibold mb-4 transition-colors duration-300 ${t.title}`}>Maliyyə Hesabatı</h2>
        <div className="grid grid-cols-2 lg:grid-cols-3 gap-4">
          <div className={`p-4 rounded-lg transition-colors duration-300 ${t.itemBg}`}>
            <p className={`text-sm ${t.muted}`}>Ümumi Gəlir</p>
            <p className="text-xl font-bold text-emerald-400">{report.totalIncome?.toLocaleString()} AZN</p>
          </div>
          <div className={`p-4 rounded-lg transition-colors duration-300 ${t.itemBg}`}>
            <p className={`text-sm ${t.muted}`}>Müəllim Payları (faiz)</p>
            <p className="text-xl font-bold text-orange-400">{report.totalTeacherSalaries?.toLocaleString()} AZN</p>
          </div>
          <div className={`p-4 rounded-lg transition-colors duration-300 ${t.itemBg}`}>
            <p className={`text-sm ${t.muted}`}>Daimi Xərclər</p>
            <p className="text-xl font-bold text-yellow-400">{report.totalFixedExpenses?.toLocaleString()} AZN</p>
          </div>
          <div className={`p-4 rounded-lg transition-colors duration-300 ${t.itemBg}`}>
            <p className={`text-sm ${t.muted}`}>Kommunal Xərclər</p>
            <p className="text-xl font-bold text-blue-400">{report.totalUtilityExpenses?.toLocaleString()} AZN</p>
          </div>
          <div className={`p-4 rounded-lg transition-colors duration-300 ${t.itemBg}`}>
            <p className={`text-sm ${t.muted}`}>Ümumi Xərc</p>
            <p className="text-xl font-bold text-red-400">{report.totalExpenses?.toLocaleString()} AZN</p>
          </div>
          <div className={`p-4 rounded-lg border border-indigo-500/30 transition-colors duration-300 ${t.itemBg}`}>
            <p className={`text-sm ${t.muted}`}>Xalis Mənfəət</p>
            <p className={`text-xl font-bold ${(report.netProfit || 0) >= 0 ? 'text-emerald-400' : 'text-red-400'}`}>{report.netProfit?.toLocaleString()} AZN</p>
          </div>
        </div>
      </motion.div>

      {/* Chart */}
      {chartData.length > 0 && (
        <motion.div initial={{ opacity: 0, y: 20 }} animate={{ opacity: 1, y: 0 }} transition={{ delay: 0.4 }} className={`${t.cardBg} border rounded-xl p-5 transition-colors duration-300`}>
          <h2 className={`text-lg font-semibold mb-4 transition-colors duration-300 ${t.title}`}>Aylıq Müqayisə (Son 6 Ay)</h2>
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

      {/* Overdue List */}
      {report.overduePayments && report.overduePayments.length > 0 && (
        <motion.div initial={{ opacity: 0, y: 20 }} animate={{ opacity: 1, y: 0 }} transition={{ delay: 0.5 }} className={`${t.cardBg} border rounded-xl p-5 transition-colors duration-300`}>
          <h2 className={`text-lg font-semibold mb-4 transition-colors duration-300 ${t.title}`}>Gecikmiş Ödənişlər</h2>
          <div className="space-y-2">
            {report.overduePayments.map((p) => (
              <div key={p.id} className={`flex items-center justify-between p-3 rounded-lg transition-colors duration-300 ${t.isDark ? 'bg-red-500/5 border border-red-500/10' : 'bg-red-50 border border-red-100'}`}>
                <div>
                  <p className={`text-sm font-medium transition-colors duration-300 ${t.title}`}>{p.studentFirstName} {p.studentLastName}</p>
                  <p className={`text-xs ${t.muted}`}>{p.groupNumber} - {monthNames[p.paymentMonth]} {p.paymentYear}</p>
                </div>
                <p className="text-sm font-medium text-red-400">{p.amount.toLocaleString()} AZN</p>
              </div>
            ))}
          </div>
        </motion.div>
      )}
    </div>
  );
};
