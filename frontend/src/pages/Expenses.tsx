import React, { useState } from 'react';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { motion, AnimatePresence } from 'framer-motion';
import { expensesApi } from '../api/expenses';
import { Expense, ExpenseType } from '../types';
import { useThemeClasses } from '../lib/theme';
import { Plus, Edit2, Trash2, X, Repeat } from 'lucide-react';
import toast from 'react-hot-toast';

const expenseTypeLabels: Record<string, string> = {
  YER: 'Yer (İcarə)',
  ISCHI_MAASI: 'İşçi Maaşı',
  INTERNET: 'İnternet',
  ELEKTRIK: 'Elektrik',
  SU: 'Su',
  QAZ: 'Qaz',
  DIGER: 'Digər',
};
const monthNames = ['', 'Yanvar', 'Fevral', 'Mart', 'Aprel', 'May', 'İyun', 'İyul', 'Avqust', 'Sentyabr', 'Oktyabr', 'Noyabr', 'Dekabr'];

export const Expenses: React.FC = () => {
  const t = useThemeClasses();
  const now = new Date();
  const [year, setYear] = useState(now.getFullYear());
  const [month, setMonth] = useState(now.getMonth() + 1);
  const [showSixMonths, setShowSixMonths] = useState(false);
  const [showModal, setShowModal] = useState(false);
  const [editing, setEditing] = useState<Expense | null>(null);
  const [form, setForm] = useState({ type: ExpenseType.YER, amount: 0, recurring: false, description: '', expenseYear: now.getFullYear(), expenseMonth: now.getMonth() + 1 });
  const queryClient = useQueryClient();

  const { data: expenses = [], isLoading } = useQuery({ queryKey: ['expenses', year, month], queryFn: () => expensesApi.getByPeriod(year, month) });

  // 6 months data
  const sixMonthsData = useQuery({
    queryKey: ['expenses-six-months'],
    queryFn: async () => {
      const results = [];
      for (let i = 5; i >= 0; i--) {
        let m = now.getMonth() + 1 - i;
        let y = now.getFullYear();
        while (m <= 0) { m += 12; y -= 1; }
        const data = await expensesApi.getByPeriod(y, m);
        results.push({ year: y, month: m, total: data.reduce((sum, e) => sum + e.amount, 0), count: data.length });
      }
      return results;
    },
    enabled: showSixMonths,
  });

  const createMutation = useMutation({
    mutationFn: (data: any) => expensesApi.create({ ...data, expenseYear: year, expenseMonth: month }),
    onSuccess: () => { queryClient.invalidateQueries({ queryKey: ['expenses'] }); toast.success('Xərc əlavə edildi'); closeModal(); },
    onError: (err: any) => toast.error(err.response?.data?.message || 'Xəta'),
  });
  const updateMutation = useMutation({
    mutationFn: ({ id, data }: any) => expensesApi.update(id, { ...data, expenseYear: form.expenseYear, expenseMonth: form.expenseMonth }),
    onSuccess: () => { queryClient.invalidateQueries({ queryKey: ['expenses'] }); toast.success('Xərc yeniləndi'); closeModal(); },
  });
  const deleteMutation = useMutation({
    mutationFn: expensesApi.delete,
    onSuccess: () => { queryClient.invalidateQueries({ queryKey: ['expenses'] }); toast.success('Xərc silindi'); },
  });

  const openCreate = () => { setEditing(null); setForm({ type: ExpenseType.YER, amount: 0, recurring: false, description: '', expenseYear: year, expenseMonth: month }); setShowModal(true); };
  const openEdit = (e: Expense) => { setEditing(e); setForm({ type: e.type, amount: e.amount, recurring: e.recurring, description: e.description || '', expenseYear: e.expenseYear, expenseMonth: e.expenseMonth }); setShowModal(true); };
  const closeModal = () => { setShowModal(false); setEditing(null); };
  const handleSubmit = (e: React.FormEvent) => { e.preventDefault(); editing?.id ? updateMutation.mutate({ id: editing.id, data: form }) : createMutation.mutate(form); };
  const total = expenses.reduce((sum, e) => sum + e.amount, 0);
  const inputCls = `w-full px-3 py-2 rounded-lg focus:outline-none focus:ring-2 focus:ring-indigo-500 transition-colors duration-300 ${t.inputBgModal}`;

  return (
    <div className="space-y-6">
      <div className="flex flex-col sm:flex-row items-start sm:items-center justify-between gap-4">
        <h1 className={`text-2xl font-bold transition-colors duration-300 ${t.title}`}>Xərclər</h1>
        <div className="flex gap-2 items-center">
          <button onClick={() => setShowSixMonths(!showSixMonths)} className={`px-3 py-2 text-sm rounded-lg transition-colors ${showSixMonths ? 'bg-indigo-600 text-white' : `${t.cancelBtn}`}`}>
            Son 6 ay
          </button>
          {!showSixMonths && (
            <>
              <select value={month} onChange={(e) => setMonth(+e.target.value)} className={`px-3 py-2 text-sm rounded-lg focus:outline-none focus:ring-2 focus:ring-indigo-500 transition-colors duration-300 ${t.inputBgModal}`}>
                {monthNames.slice(1).map((name, i) => <option key={i + 1} value={i + 1}>{name}</option>)}
              </select>
              <select value={year} onChange={(e) => setYear(+e.target.value)} className={`px-3 py-2 text-sm rounded-lg focus:outline-none focus:ring-2 focus:ring-indigo-500 transition-colors duration-300 ${t.inputBgModal}`}>
                {[2024, 2025, 2026, 2027].map((y) => <option key={y} value={y}>{y}</option>)}
              </select>
            </>
          )}
          <button onClick={openCreate} className="flex items-center gap-2 px-4 py-2 bg-indigo-600 hover:bg-indigo-700 text-white font-medium rounded-lg"><Plus size={18} /> Yeni</button>
        </div>
      </div>

      <div className={`${t.cardBg} border rounded-xl p-4 transition-colors duration-300`}>
        <p className={`text-sm ${t.muted}`}>Cəmi Xərc - {monthNames[month]} {year}</p>
        <p className="text-2xl font-bold text-red-400">{total.toLocaleString()} AZN</p>
      </div>

      {/* 6 Months Summary */}
      {showSixMonths && sixMonthsData.data && (
        <motion.div initial={{ opacity: 0, y: 20 }} animate={{ opacity: 1, y: 0 }} className={`${t.cardBg} border rounded-xl p-5 transition-colors duration-300`}>
          <h2 className={`text-lg font-semibold mb-4 transition-colors duration-300 ${t.title}`}>Son 6 Ayın Xərcləri</h2>
          <div className="grid grid-cols-2 sm:grid-cols-3 lg:grid-cols-6 gap-3">
            {sixMonthsData.data.map((m) => (
              <div key={`${m.year}-${m.month}`} className={`p-3 rounded-lg text-center transition-colors duration-300 ${t.itemBg}`}>
                <p className={`text-xs ${t.muted}`}>{monthNames[m.month]}</p>
                <p className="text-lg font-bold text-red-400">{m.total.toLocaleString()}</p>
                <p className={`text-xs ${t.muted}`}>{m.count} xərc</p>
              </div>
            ))}
          </div>
          <div className={`mt-4 pt-4 border-t flex justify-between items-center transition-colors duration-300 ${t.border}`}>
            <span className={`font-medium transition-colors duration-300 ${t.title}`}>Cəmi (6 ay)</span>
            <span className="text-xl font-bold text-red-400">{sixMonthsData.data.reduce((sum, m) => sum + m.total, 0).toLocaleString()} AZN</span>
          </div>
        </motion.div>
      )}

      <div className={`${t.cardBg} border rounded-xl overflow-hidden transition-colors duration-300`}>
        <div className="overflow-x-auto">
          <table className="w-full text-sm">
            <thead><tr className={`${t.thClass} transition-colors duration-300`}>
              <th className="text-left py-3 px-4">Növ</th><th className="text-left py-3 px-4">Məbləğ</th><th className="text-left py-3 px-4">Təsvir</th><th className="text-left py-3 px-4">Daimi</th><th className="text-right py-3 px-4">Əməliyyatlar</th>
            </tr></thead>
            <tbody>
              {isLoading ? <tr><td colSpan={5} className={`text-center py-8 ${t.muted}`}>Yüklənir...</td></tr>
              : expenses.length === 0 ? <tr><td colSpan={5} className={`text-center py-8 ${t.muted}`}>Xərc tapılmadı</td></tr>
              : expenses.map((exp, i) => (
                <motion.tr key={exp.id} initial={{ opacity: 0 }} animate={{ opacity: 1 }} transition={{ delay: i * 0.03 }} className={`border-b ${t.rowBorder} ${t.rowHover} transition-colors duration-300`}>
                  <td className={`py-3 px-4 font-medium transition-colors duration-300 ${t.tdClass}`}>{expenseTypeLabels[exp.type]}</td>
                  <td className="py-3 px-4 text-red-400 font-medium">{exp.amount.toLocaleString()} AZN</td>
                  <td className={`py-3 px-4 transition-colors duration-300 ${t.tdMuted}`}>{exp.description || '—'}</td>
                  <td className="py-3 px-4">{exp.recurring && <Repeat size={14} className="text-slate-400" />}</td>
                  <td className="py-3 px-4">
                    <div className="flex items-center justify-end gap-1">
                      <button onClick={() => openEdit(exp)} className={`p-1.5 rounded-lg transition-colors ${t.iconBtn}`}><Edit2 size={15} /></button>
                      <button onClick={() => { if (confirm('Silmək istəyirsiniz?')) deleteMutation.mutate(exp.id); }} className={`p-1.5 rounded-lg transition-colors ${t.iconBtnDanger}`}><Trash2 size={15} /></button>
                    </div>
                  </td>
                </motion.tr>
              ))}
            </tbody>
          </table>
        </div>
      </div>

      <AnimatePresence>
        {showModal && (
          <>
            <motion.div initial={{ opacity: 0 }} animate={{ opacity: 1 }} exit={{ opacity: 0 }} className={`fixed inset-0 z-40 ${t.overlay}`} onClick={closeModal} />
            <motion.div initial={{ opacity: 0, scale: 0.95 }} animate={{ opacity: 1, scale: 1 }} exit={{ opacity: 0, scale: 0.95 }} className="fixed inset-0 z-50 flex items-center justify-center p-4">
              <div className={`${t.modalBg} border rounded-xl w-full max-w-md p-6 transition-colors duration-300`}>
                <div className="flex items-center justify-between mb-6">
                  <h2 className={`text-lg font-semibold transition-colors duration-300 ${t.title}`}>{editing ? 'Xərci Redaktə Et' : 'Yeni Xərc'}</h2>
                  <button onClick={closeModal} className={t.iconBtn}><X size={20} /></button>
                </div>
                <form onSubmit={handleSubmit} className="space-y-4">
                  <div><label className={`block text-sm mb-1 ${t.label}`}>Xərc Növü</label>
                    <select value={form.type} onChange={(e) => setForm({ ...form, type: e.target.value as ExpenseType })} className={inputCls}>
                      {Object.entries(expenseTypeLabels).map(([key, label]) => <option key={key} value={key}>{label}</option>)}
                    </select>
                  </div>
                  <div><label className={`block text-sm mb-1 ${t.label}`}>Məbləğ (AZN)</label><input type="number" value={form.amount || ''} onChange={(e) => setForm({ ...form, amount: +e.target.value })} className={inputCls} min={0} step={0.01} required /></div>
                  <div><label className={`block text-sm mb-1 ${t.label}`}>Təsvir</label><input type="text" value={form.description} onChange={(e) => setForm({ ...form, description: e.target.value })} className={inputCls} /></div>
                  <div className="flex items-center gap-2">
                    <input type="checkbox" id="recurring" checked={form.recurring} onChange={(e) => setForm({ ...form, recurring: e.target.checked })} className="w-4 h-4 rounded" />
                    <label htmlFor="recurring" className={`text-sm transition-colors duration-300 ${t.muted}`}>Daimi xərc (hər ay avtomatik)</label>
                  </div>
                  <div className="flex gap-3 pt-2">
                    <button type="button" onClick={closeModal} className={`flex-1 py-2 rounded-lg transition-colors ${t.cancelBtn}`}>Ləğv Et</button>
                    <button type="submit" className="flex-1 py-2 bg-indigo-600 hover:bg-indigo-700 text-white font-medium rounded-lg transition-colors">{editing ? 'Yenilə' : 'Əlavə Et'}</button>
                  </div>
                </form>
              </div>
            </motion.div>
          </>
        )}
      </AnimatePresence>
    </div>
  );
};
