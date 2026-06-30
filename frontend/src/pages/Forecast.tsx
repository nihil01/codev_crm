import { useQuery } from '@tanstack/react-query';
import { motion } from 'framer-motion';
import { forecastApi } from '../api/forecast';
import { useThemeClasses } from '../lib/theme';
import { ForecastData, ForecastPoint } from '../types';
import {
  AreaChart, Area, XAxis, YAxis, CartesianGrid, Tooltip,
  ResponsiveContainer, ReferenceLine, Legend,
} from 'recharts';
import {
  TrendingUp, TrendingDown, Minus, DollarSign,
  PiggyBank, ArrowUpRight, ArrowDownRight, Brain,
} from 'lucide-react';

const MN = ['', 'Yan', 'Fev', 'Mar', 'Apr', 'May', 'İyun', 'İyul', 'Avq', 'Sen', 'Okt', 'Noy', 'Dek'];
const MN_FULL = ['', 'Yanvar', 'Fevral', 'Mart', 'Aprel', 'May', 'İyun', 'İyul', 'Avqust', 'Sentyabr', 'Oktyabr', 'Noyabr', 'Dekabr'];

function formatMoney(v: number) {
  if (v == null) return '0';
  return v.toLocaleString('az-AZ', { maximumFractionDigits: 0 });
}

interface CustomTooltipProps {
  active?: boolean;
  payload?: Array<{ value: number; dataKey: string; color: string }>;
  label?: string;
  isDark: boolean;
}

function CustomTooltip({ active, payload, label, isDark }: CustomTooltipProps) {
  if (!active || !payload?.length) return null;
  return (
    <div className={`${isDark ? 'bg-slate-800 border-slate-700' : 'bg-white border-slate-200'} border rounded-xl p-3 shadow-xl`}>
      <p className={`text-sm font-semibold mb-2 ${isDark ? 'text-white' : 'text-slate-900'}`}>{label}</p>
      {payload.map((entry, i) => (
        <div key={i} className="flex items-center gap-2 text-sm">
          <span className="w-2.5 h-2.5 rounded-full" style={{ backgroundColor: entry.color }} />
          <span className={isDark ? 'text-slate-400' : 'text-slate-500'}>{entry.dataKey}:</span>
          <span className="font-medium" style={{ color: entry.color }}>{formatMoney(entry.value)} AZN</span>
        </div>
      ))}
    </div>
  );
}

export function Forecast() {
  const t = useThemeClasses();

  const { data, isLoading, error } = useQuery<ForecastData>({
    queryKey: ['forecast'],
    queryFn: forecastApi.get,
  });

  if (isLoading) {
    return (
      <div className="flex items-center justify-center h-64">
        <div className="w-8 h-8 border-2 border-indigo-500 border-t-transparent rounded-full animate-spin" />
      </div>
    );
  }

  if (error || !data) {
    return (
      <div className="flex items-center justify-center h-64">
        <p className={t.muted}>Proqnoz məlumatları yüklənə bilmədi</p>
      </div>
    );
  }

  const { history, forecast, trend } = data;

  // Merge history + forecast for the chart
  const allPoints = [...history, ...forecast];
  const chartData = allPoints.map((p) => ({
    name: `${MN[p.month]} ${p.year}`,
    'Gəlir': p.income,
    'Xərc': p.expenses,
    'Xalis Qazanc': p.netProfit,
    isProjected: p.isProjected,
  }));

  // Find the separator index (last history point)
  const separatorIndex = history.length - 1;

  const trendIcon = trend.trendDirection === 'GELIŞEN'
    ? <TrendingUp size={20} />
    : trend.trendDirection === 'AZALAN'
      ? <TrendingDown size={20} />
      : <Minus size={20} />;

  const trendColor = trend.trendDirection === 'GELIŞEN'
    ? 'text-emerald-400'
    : trend.trendDirection === 'AZALAN'
      ? 'text-red-400'
      : 'text-yellow-400';

  const trendBg = trend.trendDirection === 'GELIŞEN'
    ? 'bg-emerald-500/10 border-emerald-500/20'
    : trend.trendDirection === 'AZALAN'
      ? 'bg-red-500/10 border-red-500/20'
      : 'bg-yellow-500/10 border-yellow-500/20';

  return (
    <div className="space-y-6">
      {/* Header */}
      <div className="flex flex-col sm:flex-row items-start sm:items-center justify-between gap-4">
        <div>
          <h1 className={`text-2xl font-bold ${t.title}`}>Proqnoz</h1>
          <p className={`text-sm mt-1 ${t.muted}`}>Gəlir və xərc proqnozu — son 12 ay əsasında 6 ay irəli</p>
        </div>
        <div className={`flex items-center gap-2 px-3 py-2 rounded-lg border ${trendBg}`}>
          <span className={trendColor}>{trendIcon}</span>
          <span className={`text-sm font-medium ${trendColor}`}>{trend.trendDirection}</span>
        </div>
      </div>

      {/* Trend Summary Cards */}
      <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-4">
        <motion.div
          initial={{ opacity: 0, y: 20 }}
          animate={{ opacity: 1, y: 0 }}
          className={`${t.cardBg} border ${t.border} rounded-xl p-5`}
        >
          <div className="flex items-center gap-2 mb-3">
            <div className={`p-2 rounded-lg ${trendBg}`}>
              <span className={trendColor}>{trendIcon}</span>
            </div>
            <span className={`text-sm font-medium ${t.muted}`}>Aylıq Trend</span>
          </div>
          <p className={`text-2xl font-bold ${trendColor}`}>
            {trend.monthlyProfitTrend >= 0 ? '+' : ''}{formatMoney(trend.monthlyProfitTrend)} AZN
          </p>
          <p className={`text-xs mt-1 ${t.muted}`}>ayda orta dəyişim</p>
        </motion.div>

        <motion.div
          initial={{ opacity: 0, y: 20 }}
          animate={{ opacity: 1, y: 0 }}
          transition={{ delay: 0.05 }}
          className={`${t.cardBg} border ${t.border} rounded-xl p-5`}
        >
          <div className="flex items-center gap-2 mb-3">
            <div className="p-2 rounded-lg bg-indigo-500/10 border border-indigo-500/20">
              <Brain size={20} className="text-indigo-400" />
            </div>
            <span className={`text-sm font-medium ${t.muted}`}>Proqnoz Qazanc (12 ay)</span>
          </div>
          <p className={`text-2xl font-bold ${trend.projectedAnnualProfit >= 0 ? 'text-indigo-400' : 'text-red-400'}`}>
            {formatMoney(trend.projectedAnnualProfit)} AZN
          </p>
          <p className={`text-xs mt-1 ${t.muted}`}>növbəti 12 ay üçün proqnoz</p>
        </motion.div>

        <motion.div
          initial={{ opacity: 0, y: 20 }}
          animate={{ opacity: 1, y: 0 }}
          transition={{ delay: 0.1 }}
          className={`${t.cardBg} border ${t.border} rounded-xl p-5`}
        >
          <div className="flex items-center gap-2 mb-3">
            <div className="p-2 rounded-lg bg-emerald-500/10 border border-emerald-500/20">
              <DollarSign size={20} className="text-emerald-400" />
            </div>
            <span className={`text-sm font-medium ${t.muted}`}>Orta Aylıq Gəlir</span>
          </div>
          <p className="text-2xl font-bold text-emerald-400">{formatMoney(trend.averageMonthlyIncome)} AZN</p>
          <p className={`text-xs mt-1 ${t.muted}`}>son 12 ayın ortası</p>
        </motion.div>

        <motion.div
          initial={{ opacity: 0, y: 20 }}
          animate={{ opacity: 1, y: 0 }}
          transition={{ delay: 0.15 }}
          className={`${t.cardBg} border ${t.border} rounded-xl p-5`}
        >
          <div className="flex items-center gap-2 mb-3">
            <div className="p-2 rounded-lg bg-red-500/10 border border-red-500/20">
              <PiggyBank size={20} className="text-red-400" />
            </div>
            <span className={`text-sm font-medium ${t.muted}`}>Orta Aylıq Xərc</span>
          </div>
          <p className="text-2xl font-bold text-red-400">{formatMoney(trend.averageMonthlyExpenses)} AZN</p>
          <p className={`text-xs mt-1 ${t.muted}`}>son 12 ayın ortası</p>
        </motion.div>
      </div>

      {/* Main Chart */}
      <motion.div
        initial={{ opacity: 0, y: 20 }}
        animate={{ opacity: 1, y: 0 }}
        transition={{ delay: 0.2 }}
        className={`${t.cardBg} border ${t.border} rounded-xl p-5`}
      >
        <div className="flex items-center justify-between mb-4">
          <h2 className={`text-lg font-semibold ${t.title}`}>Gəlir və Xərc Dinamikası</h2>
          <div className="flex items-center gap-4 text-xs">
            <span className="flex items-center gap-1.5">
              <span className="w-6 h-0.5 bg-emerald-400 rounded" />
              <span className={t.muted}>Həqiqi</span>
            </span>
            <span className="flex items-center gap-1.5">
              <span className="w-6 h-0.5 bg-emerald-400 rounded border-dashed" style={{ borderTop: '2px dashed #22c55e', height: 0, background: 'none' }} />
              <span className={t.muted}>Proqnoz</span>
            </span>
          </div>
        </div>
        <div className="h-96">
          <ResponsiveContainer width="100%" height="100%">
            <AreaChart data={chartData} margin={{ top: 10, right: 10, left: 0, bottom: 0 }}>
              <defs>
                <linearGradient id="gradIncome" x1="0" y1="0" x2="0" y2="1">
                  <stop offset="5%" stopColor="#22c55e" stopOpacity={0.3} />
                  <stop offset="95%" stopColor="#22c55e" stopOpacity={0} />
                </linearGradient>
                <linearGradient id="gradExpense" x1="0" y1="0" x2="0" y2="1">
                  <stop offset="5%" stopColor="#ef4444" stopOpacity={0.3} />
                  <stop offset="95%" stopColor="#ef4444" stopOpacity={0} />
                </linearGradient>
                <linearGradient id="gradProfit" x1="0" y1="0" x2="0" y2="1">
                  <stop offset="5%" stopColor="#6366f1" stopOpacity={0.3} />
                  <stop offset="95%" stopColor="#6366f1" stopOpacity={0} />
                </linearGradient>
              </defs>
              <CartesianGrid strokeDasharray="3 3" stroke={t.isDark ? '#334155' : '#e2e8f0'} />
              <XAxis
                dataKey="name"
                stroke={t.isDark ? '#94a3b8' : '#64748b'}
                fontSize={11}
                tickLine={false}
                angle={-45}
                textAnchor="end"
                height={50}
              />
              <YAxis
                stroke={t.isDark ? '#94a3b8' : '#64748b'}
                fontSize={12}
                tickLine={false}
                tickFormatter={(v) => `${(v / 1000).toFixed(0)}k`}
              />
              <Tooltip content={<CustomTooltip isDark={t.isDark} />} />
              <Legend
                wrapperStyle={{ paddingTop: '10px' }}
                formatter={(value) => <span className={t.isDark ? 'text-slate-300' : 'text-slate-600'}>{value}</span>}
              />
              {/* Separator line between history and forecast */}
              <ReferenceLine
                x={chartData[separatorIndex]?.name}
                stroke={t.isDark ? '#6366f1' : '#6366f1'}
                strokeDasharray="6 3"
                strokeWidth={2}
                label={{
                  value: '← Həqiqi | Proqnoz →',
                  position: 'insideTopRight',
                  fill: t.isDark ? '#94a3b8' : '#64748b',
                  fontSize: 11,
                }}
              />
              <Area
                type="monotone"
                dataKey="Gəlir"
                stroke="#22c55e"
                strokeWidth={2}
                fill="url(#gradIncome)"
                dot={false}
                activeDot={{ r: 5, strokeWidth: 2 }}
              />
              <Area
                type="monotone"
                dataKey="Xərc"
                stroke="#ef4444"
                strokeWidth={2}
                fill="url(#gradExpense)"
                dot={false}
                activeDot={{ r: 5, strokeWidth: 2 }}
              />
              <Area
                type="monotone"
                dataKey="Xalis Qazanc"
                stroke="#6366f1"
                strokeWidth={2}
                fill="url(#gradProfit)"
                dot={false}
                activeDot={{ r: 5, strokeWidth: 2 }}
              />
            </AreaChart>
          </ResponsiveContainer>
        </div>
      </motion.div>

      {/* Forecast Table */}
      <motion.div
        initial={{ opacity: 0, y: 20 }}
        animate={{ opacity: 1, y: 0 }}
        transition={{ delay: 0.3 }}
        className={`${t.cardBg} border ${t.border} rounded-xl p-5`}
      >
        <h2 className={`text-lg font-semibold mb-4 ${t.title}`}>Proqnoz Cədvəli — Növbəti 6 Ay</h2>
        <div className="overflow-x-auto">
          <table className="w-full text-sm">
            <thead>
              <tr>
                <th className={`text-left px-4 py-3 font-medium ${t.thClass}`}>Ay</th>
                <th className={`text-right px-4 py-3 font-medium ${t.thClass}`}>Gəlir</th>
                <th className={`text-right px-4 py-3 font-medium ${t.thClass}`}>Xərc</th>
                <th className={`text-right px-4 py-3 font-medium ${t.thClass}`}>Xalis Qazanc</th>
                <th className={`text-center px-4 py-3 font-medium ${t.thClass}`}>Trend</th>
              </tr>
            </thead>
            <tbody>
              {forecast.map((p: ForecastPoint, i: number) => {
                const prev = i === 0 ? history[history.length - 1] : forecast[i - 1];
                const profitDiff = prev ? p.netProfit - prev.netProfit : 0;
                return (
                  <tr key={`${p.year}-${p.month}`} className={t.rowHover}>
                    <td className={`px-4 py-3 font-medium ${t.tdClass}`}>
                      {MN_FULL[p.month]} {p.year}
                    </td>
                    <td className={`px-4 py-3 text-right text-emerald-400 font-medium`}>
                      {formatMoney(p.income)} AZN
                    </td>
                    <td className={`px-4 py-3 text-right text-red-400 font-medium`}>
                      {formatMoney(p.expenses)} AZN
                    </td>
                    <td className={`px-4 py-3 text-right font-bold ${p.netProfit >= 0 ? 'text-indigo-400' : 'text-red-400'}`}>
                      {formatMoney(p.netProfit)} AZN
                    </td>
                    <td className="px-4 py-3 text-center">
                      {profitDiff > 0 ? (
                        <span className="inline-flex items-center gap-1 text-emerald-400 text-xs font-medium">
                          <ArrowUpRight size={14} /> +{formatMoney(profitDiff)}
                        </span>
                      ) : profitDiff < 0 ? (
                        <span className="inline-flex items-center gap-1 text-red-400 text-xs font-medium">
                          <ArrowDownRight size={14} /> {formatMoney(profitDiff)}
                        </span>
                      ) : (
                        <span className={`text-xs ${t.muted}`}>—</span>
                      )}
                    </td>
                  </tr>
                );
              })}
            </tbody>
            <tfoot>
              <tr className={`border-t ${t.borderStrong}`}>
                <td className={`px-4 py-3 font-bold ${t.title}`}>Cəmi</td>
                <td className={`px-4 py-3 text-right font-bold text-emerald-400`}>
                  {formatMoney(forecast.reduce((s, p) => s + p.income, 0))} AZN
                </td>
                <td className={`px-4 py-3 text-right font-bold text-red-400`}>
                  {formatMoney(forecast.reduce((s, p) => s + p.expenses, 0))} AZN
                </td>
                <td className={`px-4 py-3 text-right font-bold ${forecast.reduce((s, p) => s + p.netProfit, 0) >= 0 ? 'text-indigo-400' : 'text-red-400'}`}>
                  {formatMoney(forecast.reduce((s, p) => s + p.netProfit, 0))} AZN
                </td>
                <td />
              </tr>
            </tfoot>
          </table>
        </div>
      </motion.div>

      {/* Historical Summary */}
      <motion.div
        initial={{ opacity: 0, y: 20 }}
        animate={{ opacity: 1, y: 0 }}
        transition={{ delay: 0.35 }}
        className={`${t.cardBg} border ${t.border} rounded-xl p-5`}
      >
        <h2 className={`text-lg font-semibold mb-4 ${t.title}`}>Son 12 Ayın Xülasəsi</h2>
        <div className="grid grid-cols-1 md:grid-cols-3 gap-4">
          <div className={`p-4 rounded-lg ${t.itemBg}`}>
            <p className={`text-sm ${t.muted}`}>Ümumi Gəlir</p>
            <p className="text-xl font-bold text-emerald-400">
              {formatMoney(history.reduce((s, p) => s + p.income, 0))} AZN
            </p>
            <p className={`text-xs mt-1 ${t.muted}`}>
              orta: {formatMoney(trend.averageMonthlyIncome)} AZN/ay
            </p>
          </div>
          <div className={`p-4 rounded-lg ${t.itemBg}`}>
            <p className={`text-sm ${t.muted}`}>Ümumi Xərc</p>
            <p className="text-xl font-bold text-red-400">
              {formatMoney(history.reduce((s, p) => s + p.expenses, 0))} AZN
            </p>
            <p className={`text-xs mt-1 ${t.muted}`}>
              orta: {formatMoney(trend.averageMonthlyExpenses)} AZN/ay
            </p>
          </div>
          <div className={`p-4 rounded-lg border border-indigo-500/30 ${t.itemBg}`}>
            <p className={`text-sm ${t.muted}`}>Xalis Mənfəət</p>
            <p className={`text-xl font-bold ${history.reduce((s, p) => s + p.netProfit, 0) >= 0 ? 'text-indigo-400' : 'text-red-400'}`}>
              {formatMoney(history.reduce((s, p) => s + p.netProfit, 0))} AZN
            </p>
            <p className={`text-xs mt-1 ${t.muted}`}>
              orta: {formatMoney(trend.averageMonthlyProfit)} AZN/ay
            </p>
          </div>
        </div>
      </motion.div>
    </div>
  );
}
