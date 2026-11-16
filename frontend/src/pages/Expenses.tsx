import { useState } from 'react';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { motion, AnimatePresence } from 'framer-motion';
import { expensesApi } from '../api/expenses';
import { Expense, ExpenseType } from '../types';
import { useThemeClasses } from '../lib/theme';
import { Plus, Edit2, Trash2, X, Repeat, CalendarDays } from 'lucide-react';
import toast from 'react-hot-toast';

const typeLabels: Record<string, string> = {
  YER: 'Yer (İcarə)', ISCHI_MAASI: 'İşçi Maaşı', INTERNET: 'İnternet',
  ELEKTRIK: 'Elektrik', SU: 'Su', QAZ: 'Qaz', DIGER: 'Digər', REKLAM: 'Reklam', KOMMUNAL: 'Kommunal', AVADANLIQ: 'Avadanlıq',
};
const MN = ['', 'Yanvar', 'Fevral', 'Mart', 'Aprel', 'May', 'İyun', 'İyul', 'Avqust', 'Sentyabr', 'Oktyabr', 'Noyabr', 'Dekabr'];

export function Expenses() {
  const t = useThemeClasses();
  const now = new Date();
  const cy = now.getFullYear();
  const cm = now.getMonth() + 1;

  const [year, setYear] = useState(cy);
  const [month, setMonth] = useState(cm);
  const [view6, setView6] = useState(false);
  const [showModal, setShowModal] = useState(false);
  const [editing, setEditing] = useState<Expense | null>(null);
  const [form, setForm] = useState({ type: ExpenseType.YER, amount: 0, recurring: false, description: '', adChannel: '', expenseYear: cy, expenseMonth: cm });
  const qc = useQueryClient();

  const { data: list = [], isLoading } = useQuery({
    queryKey: ['exp', year, month],
    queryFn: () => expensesApi.getByPeriod(year, month),
  });

  const { data: sixMonths } = useQuery({
    queryKey: ['exp6'],
    queryFn: async () => {
      const r: { y: number; m: number; total: number; count: number }[] = [];
      for (let i = 5; i >= 0; i--) {
        let m = cm - i, y = cy;
        if (m <= 0) { m += 12; y -= 1; }
        const d = await expensesApi.getByPeriod(y, m);
        r.push({ y, m, total: d.filter(e=>!e.cancelled).reduce((s, e) => s + e.amount, 0), count: d.filter(e=>!e.cancelled).length });
      }
      return r;
    },
    enabled: view6,
  });

  const mk = (data: any) => {qc.invalidateQueries({ queryKey: ['exp'] }); qc.invalidateQueries({queryKey:['expense-rules']});};

  const createMut = useMutation({
    mutationFn: (d: any) => expensesApi.create({ ...d, expenseYear: year, expenseMonth: month }),
    onSuccess: () => { mk(null); toast.success('Əlavə edildi'); closeModal(); },
    onError: (e: any) => toast.error(e.response?.data?.message || 'Xəta'),
  });
  const updateMut = useMutation({
    mutationFn: ({ id, data }: any) => expensesApi.update(id, data),
    onSuccess: () => { mk(null); toast.success('Yeniləndi'); closeModal(); },
    onError: (e: any) => toast.error(e.response?.data?.message || 'Xəta'),
  });
  const deleteMut = useMutation({
    mutationFn: expensesApi.delete,
    onSuccess: () => { mk(null); toast.success('Silindi'); },
  });

  const restoreMut = useMutation({
    mutationFn:expensesApi.restoreMonth,
    onSuccess:()=>{mk(null);toast.success('Aylıq xərc bərpa edildi');},
    onError:(e:any)=>toast.error(e.response?.data?.message||'Bərpa edilmədi')
  });

  const openNew = () => { setEditing(null); setForm({ type: ExpenseType.YER, amount: 0, recurring: false, description: '', adChannel: '', expenseYear: year, expenseMonth: month }); setShowModal(true); };
  const openEdit = (e: Expense) => { setEditing(e); setForm({ type: e.type, amount: e.amount, recurring: e.recurring, description: e.description || '', adChannel: e.adChannel || '', expenseYear: e.expenseYear, expenseMonth: e.expenseMonth }); setShowModal(true); };
  const closeModal = () => { setShowModal(false); setEditing(null); };
  const onSubmit = (ev: React.FormEvent) => { ev.preventDefault(); editing?.id ? updateMut.mutate({ id: editing.id, data: form }) : createMut.mutate(form); };
  const total = list.filter(e=>!e.cancelled).reduce((s, e) => s + e.amount, 0);
  const input = `w-full px-3 py-2 rounded-lg focus:outline-none focus:ring-2 focus:ring-indigo-500 transition-colors ${t.inputBgModal}`;

  return (
    <div className="space-y-6">
      <div className="flex flex-col sm:flex-row items-start sm:items-center justify-between gap-4">
        <h1 className={`text-2xl font-bold ${t.title}`}>Xərclər</h1>
        <div className="flex gap-2 items-center flex-wrap">
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
          <button onClick={openNew} className="flex items-center gap-1.5 px-4 py-2 bg-indigo-600 hover:bg-indigo-700 text-white text-sm font-medium rounded-lg"><Plus size={16} /> Yeni</button>
        </div>
      </div>

      <div className={`${t.cardBg} border ${t.border} rounded-xl p-4`}>
        <p className={`text-sm ${t.muted}`}>Cəmi Xərc — {view6 ? 'Son 6 ay' : `${MN[month]} ${year}`}</p>
        <p className="text-2xl font-bold text-red-400">
          {view6 && sixMonths ? sixMonths.reduce((s, m) => s + m.total, 0).toLocaleString() : total.toLocaleString()} AZN
        </p>
      </div>

      {view6 && sixMonths && (
        <div className={`${t.cardBg} border ${t.border} rounded-xl p-5`}>
          <h2 className={`text-lg font-semibold mb-4 ${t.title}`}>Aylıq Xərclər</h2>
          <div className="grid grid-cols-2 sm:grid-cols-3 lg:grid-cols-6 gap-3">
            {sixMonths.map(m => (
              <div key={`${m.y}-${m.m}`} className={`p-3 rounded-lg text-center ${t.itemBg}`}>
                <p className={`text-xs font-medium ${t.muted}`}>{MN[m.m]} {m.y}</p>
                <p className="text-lg font-bold text-red-400 mt-1">{m.total.toLocaleString()}</p>
                <p className={`text-xs ${t.muted}`}>{m.count} xərc</p>
              </div>
            ))}
          </div>
        </div>
      )}

      {!view6 && (
        <div className={`${t.cardBg} border ${t.border} rounded-xl overflow-hidden`}>
          <table className="w-full text-sm">
            <thead>
              <tr className={`${t.thClass}`}>
                <th className="text-left py-3 px-4">Növ</th>
                <th className="text-left py-3 px-4">Məbləğ</th>
                <th className="text-left py-3 px-4">Təsvir</th>
                <th className="text-left py-3 px-4">Daimi</th>
                <th className="text-right py-3 px-4">Əməliyyat</th>
              </tr>
            </thead>
            <tbody>
              {isLoading ? (
                <tr><td colSpan={5} className={`text-center py-8 ${t.muted}`}>Yüklənir...</td></tr>
              ) : list.length === 0 ? (
                <tr><td colSpan={5} className={`text-center py-8 ${t.muted}`}>Xərc tapılmadı</td></tr>
              ) : list.map((e, i) => (
                <motion.tr key={e.id} initial={{ opacity: 0 }} animate={{ opacity: 1 }} transition={{ delay: i * 0.03 }} className={`border-b ${t.rowBorder} ${t.rowHover}`}>
                  <td className={`py-3 px-4 font-medium ${t.tdClass}`}>{typeLabels[e.type]}</td>
                  <td className="py-3 px-4 text-red-400 font-medium">{e.cancelled?'Ləğv edilib':e.amount.toLocaleString()+' AZN'}</td>
                  <td className={`py-3 px-4 ${t.tdMuted}`}>{e.description || '—'}</td>
                  <td className="py-3 px-4">{e.recurring ? <Repeat size={14} className="text-indigo-400" /> : null}</td>
                  <td className="py-3 px-4 text-right">
                    <div className="flex items-center justify-end gap-1">
                      {!e.cancelled&&<button onClick={() => openEdit(e)} className={`p-1.5 rounded-lg ${t.iconBtn}`}><Edit2 size={15} /></button>}
                      {e.cancelled ? <button onClick={()=>restoreMut.mutate(e.id)} className="text-sm text-emerald-500">Bərpa et</button> :
                        <button onClick={() => { if (confirm(e.recurringRuleId?'Yalnız bu ayın xərcini ləğv etmək istəyirsiniz?':'Bu xərci silmək istəyirsiniz?')) deleteMut.mutate(e.id); }} className={`p-1.5 rounded-lg ${t.iconBtnDanger}`}><Trash2 size={15} /></button>}
                    </div>
                  </td>
                </motion.tr>
              ))}
            </tbody>
          </table>
        </div>
      )}

      <AnimatePresence>
        {showModal && (
          <>
            <motion.div initial={{ opacity: 0 }} animate={{ opacity: 1 }} exit={{ opacity: 0 }} className={`fixed inset-0 z-40 ${t.overlay}`} onClick={closeModal} />
            <motion.div initial={{ opacity: 0, scale: 0.95 }} animate={{ opacity: 1, scale: 1 }} exit={{ opacity: 0, scale: 0.95 }} className="fixed inset-0 z-50 flex items-center justify-center p-4">
              <div className={`${t.modalBg} border ${t.border} rounded-xl w-full max-w-md p-6`}>
                <div className="flex items-center justify-between mb-6">
                  <h2 className={`text-lg font-semibold ${t.title}`}>{editing ? 'Redaktə Et' : 'Yeni Xərc'}</h2>
                  <button onClick={closeModal} className={t.iconBtn}><X size={20} /></button>
                </div>
                <form onSubmit={onSubmit} className="space-y-4">
                  <div>
                    <label className={`block text-sm mb-1 ${t.label}`}>Növ</label>
                    <select value={form.type} onChange={e => setForm({ ...form, type: e.target.value as ExpenseType })} className={input}>
                      {Object.entries(typeLabels).map(([k, v]) => <option key={k} value={k}>{v}</option>)}
                    </select>
                  </div>
                  <div>
                    <label className={`block text-sm mb-1 ${t.label}`}>Məbləğ (AZN)</label>
                    <input type="number" value={form.amount || ''} onChange={e => setForm({ ...form, amount: +e.target.value })} className={input} min={0} step={0.01} required />
                  </div>
                  {form.type === ExpenseType.REKLAM && <div>
                    <label className={`block text-sm mb-1 ${t.label}`}>Reklam kanalı</label>
                    <select className={input} value={form.adChannel||''} onChange={e=>setForm({...form,adChannel:e.target.value})} required>
                      <option value="">Seçin</option><option value="INSTAGRAM">Instagram</option><option value="WHATSAPP">WhatsApp</option>
                      <option value="TIKTOK">TikTok</option><option value="SAYT">Sayt</option><option value="ZENG">Zəng</option>
                      <option value="TOVSIYE">Tövsiyə</option><option value="BIRBASA_GELIS">Birbaşa gəliş</option>
                    </select>
                  </div>}
                  <div>
                    <label className={`block text-sm mb-1 ${t.label}`}>Təsvir</label>
                    <input type="text" value={form.description} onChange={e => setForm({ ...form, description: e.target.value })} className={input} />
                  </div>
                  <label className="flex items-center gap-2 cursor-pointer">
                    <input type="checkbox" checked={form.recurring} disabled={!!editing?.recurringRuleId} onChange={e => setForm({ ...form, recurring: e.target.checked })} className="w-4 h-4 rounded" />
                    <span className={`text-sm ${t.muted}`}>{editing?.recurringRuleId?'Daimi xərcin yalnız bu ayı redaktə edilir':'Daimi xərc (hər ay avtomatik)'}</span>
                  </label>
                  <div className="flex gap-3 pt-2">
                    <button type="button" onClick={closeModal} className={`flex-1 py-2 rounded-lg ${t.cancelBtn}`}>Ləğv</button>
                    <button type="submit" className="flex-1 py-2 bg-indigo-600 hover:bg-indigo-700 text-white font-medium rounded-lg">{editing ? 'Yenilə' : 'Əlavə Et'}</button>
                  </div>
                </form>
              </div>
            </motion.div>
          </>
        )}
      </AnimatePresence>
    </div>
  );
}
