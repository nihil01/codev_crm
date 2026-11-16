import React, { useState } from 'react';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { motion, AnimatePresence } from 'framer-motion';
import { paymentsApi } from '../api/payments';
import { groupsApi } from '../api/groups';
import { studentsApi } from '../api/students';
import { Payment, PaymentStatus, PaymentMethod } from '../types';
import { useThemeClasses } from '../lib/theme';
import { Plus, Trash2, Check, X as XIcon, Clock, AlertTriangle, X } from 'lucide-react';
import toast from 'react-hot-toast';
import { useAuth } from '../context/AuthContext';

const statusConfig: Record<string, { color: string; icon: React.ReactNode; label: string }> = {
  ODENILIB: { color: 'text-emerald-400 bg-emerald-500/10', icon: <Check size={14} />, label: 'Ödənilib' },
  GOZLENILIR: { color: 'text-blue-400 bg-blue-500/10', icon: <Clock size={14} />, label: 'Gözlənilir' },
  GECIKIB: { color: 'text-red-400 bg-red-500/10', icon: <AlertTriangle size={14} />, label: 'Gecikib' },
  BAGISLANIB: { color: 'text-slate-400 bg-slate-500/10', icon: <XIcon size={14} />, label: 'Bağışlanıb' },
  DONDURULUB: { color: 'text-slate-400 bg-slate-500/10', icon: <Clock size={14}/>, label: 'Dondurulub' },
};
const monthNames = ['', 'Yanvar', 'Fevral', 'Mart', 'Aprel', 'May', 'İyun', 'İyul', 'Avqust', 'Sentyabr', 'Oktyabr', 'Noyabr', 'Dekabr'];

export const Payments: React.FC = () => {
  const t = useThemeClasses();
  const { user } = useAuth();
  const isAdmin = user?.role === 'ADMIN';
  const now = new Date();
  const [year, setYear] = useState(now.getFullYear());
  const [month, setMonth] = useState(now.getMonth() + 1);
  const [selectedGroup, setSelectedGroup] = useState<number | null>(null);
  const [payModal, setPayModal] = useState<Payment | null>(null);
  const [payMethod, setPayMethod] = useState<PaymentMethod>(PaymentMethod.NAGD);
  const [payAmount,setPayAmount] = useState(0);
  const [historyId,setHistoryId] = useState<number|null>(null);
  const [showCreate, setShowCreate] = useState(false);
  const [createForm, setCreateForm] = useState({ studentId: 0, groupId: 0, paymentYear: now.getFullYear(), paymentMonth: now.getMonth() + 1, amount: 0, note: '' });
  const queryClient = useQueryClient();

  const { data: groups = [] } = useQuery({ queryKey: ['groups-active'], queryFn: groupsApi.getActive });
  const { data: students = [] } = useQuery({ queryKey: ['students-active'], queryFn: studentsApi.getActive });
  const { data: payments = [], isLoading } = useQuery({
    queryKey: ['payments', year, month, selectedGroup],
    queryFn: () => selectedGroup ? paymentsApi.getByGroupAndPeriod(selectedGroup, year, month) : paymentsApi.getByPeriod(year, month),
  });

  const recordPaymentMutation = useMutation({
    mutationFn: ({ id, method, amount }: { id: number; method: PaymentMethod; amount: number }) => paymentsApi.partial(id, amount, method),
    onSuccess: () => { queryClient.invalidateQueries({ queryKey: ['payments'] }); toast.success('Ödəniş qeydə alındı'); setPayModal(null); },
    onError: (err: any) => toast.error(err.response?.data?.message || 'Xəta'),
  });
  const createPaymentMutation = useMutation({
    mutationFn: paymentsApi.create,
    onSuccess: () => { queryClient.invalidateQueries({ queryKey: ['payments'] }); toast.success('Ödəniş yaradıldı'); setShowCreate(false); setCreateForm({ studentId: 0, groupId: 0, paymentYear: now.getFullYear(), paymentMonth: now.getMonth() + 1, amount: 0, note: '' }); },
    onError: (err: any) => toast.error(err.response?.data?.message || 'Xəta'),
  });
  const deletePaymentMutation = useMutation({
    mutationFn: paymentsApi.delete,
    onSuccess: () => { queryClient.invalidateQueries({ queryKey: ['payments'] }); toast.success('Ödəniş silindi'); },
    onError: (err: any) => toast.error(err.response?.data?.message || 'Xəta'),
  });

  const {data:transactions=[]} = useQuery({
    queryKey:['transactions',historyId],
    queryFn:()=>paymentsApi.transactions(historyId!),
    enabled:historyId !== null
  });
  const freezeMutation=useMutation({
    mutationFn:({id,frozen}:{id:number;frozen:boolean})=>paymentsApi.freeze(id,frozen),
    onSuccess:()=>{queryClient.invalidateQueries({queryKey:['payments']});toast.success('Dondurma statusu yeniləndi');},
    onError:()=>toast.error('Status yenilənmədi')
  });
  const discountMutation=useMutation({
    mutationFn:({id,amount,reason}:{id:number;amount:number;reason:string})=>paymentsApi.discount(id,amount,reason),
    onSuccess:()=>{queryClient.invalidateQueries({queryKey:['payments']});toast.success('Endirim qeydə alındı');},
    onError:(err:any)=>toast.error(err.response?.data?.message||'Endirim uğursuz oldu')
  });
  const applyDiscount=(invoice:Payment)=>{
    const text=window.prompt('Endirim məbləği (AZN):','0');
    if(text===null) return;
    const amount=Number(text);
    if(!Number.isFinite(amount) || amount<=0) return toast.error('Məbləğ düzgün deyil');
    const reason=window.prompt('Endirim səbəbi:');
    if(!reason?.trim()) return toast.error('Səbəb məcburidir');
    discountMutation.mutate({id:invoice.id,amount,reason});
  };

  const grouped = {
    paid: payments.filter((p) => p.status === PaymentStatus.ODENILIB),
    pending: payments.filter((p) => p.status === PaymentStatus.GOZLENILIR),
    overdue: payments.filter((p) => p.status === PaymentStatus.GECIKIB),
  };
  const totalCollected = payments.reduce((sum,p)=>sum+(p.paidAmount||0),0);
  const totalExpected = payments.reduce((sum,p)=>sum+p.amount-(p.discountAmount||0),0);
  const inputCls = `w-full px-3 py-2 rounded-lg focus:outline-none focus:ring-2 focus:ring-indigo-500 transition-colors duration-300 ${t.inputBgModal}`;

  return (
    <div className="space-y-6">
      <div className="flex flex-col sm:flex-row items-start sm:items-center justify-between gap-4">
        <h1 className={`text-2xl font-bold transition-colors duration-300 ${t.title}`}>Ödənişlər</h1>
        <div className="flex gap-2">
          <select value={selectedGroup || ''} onChange={(e) => setSelectedGroup(e.target.value ? +e.target.value : null)} className={`px-3 py-2 text-sm rounded-lg focus:outline-none focus:ring-2 focus:ring-indigo-500 transition-colors duration-300 ${t.inputBgModal}`}>
            <option value="">Bütün qruplar</option>
            {groups.map((g) => <option key={g.id} value={g.id}>{g.groupNumber}</option>)}
          </select>
          <select value={month} onChange={(e) => setMonth(+e.target.value)} className={`px-3 py-2 text-sm rounded-lg focus:outline-none focus:ring-2 focus:ring-indigo-500 transition-colors duration-300 ${t.inputBgModal}`}>
            {monthNames.slice(1).map((name, i) => <option key={i + 1} value={i + 1}>{name}</option>)}
          </select>
          <select value={year} onChange={(e) => setYear(+e.target.value)} className={`px-3 py-2 text-sm rounded-lg focus:outline-none focus:ring-2 focus:ring-indigo-500 transition-colors duration-300 ${t.inputBgModal}`}>
            {[2024, 2025, 2026, 2027].map((y) => <option key={y} value={y}>{y}</option>)}
          </select>
          <button onClick={() => setShowCreate(true)} className="flex items-center gap-1 px-3 py-2 bg-indigo-600 hover:bg-indigo-700 text-white font-medium rounded-lg text-sm"><Plus size={16} /></button>
        </div>
      </div>

      {isAdmin && <div className="grid grid-cols-1 sm:grid-cols-4 gap-4">
        <div className={`${t.cardBg} border rounded-xl p-4 transition-colors duration-300`}><p className={`text-sm ${t.muted}`}>Toplanmış</p><p className="text-2xl font-bold text-emerald-400">{totalCollected.toLocaleString()} AZN</p></div>
        <div className={`${t.cardBg} border rounded-xl p-4 transition-colors duration-300`}><p className={`text-sm ${t.muted}`}>Gözlənilən</p><p className="text-2xl font-bold text-blue-400">{totalExpected.toLocaleString()} AZN</p></div>
        <div className={`${t.cardBg} border rounded-xl p-4 transition-colors duration-300`}><p className={`text-sm ${t.muted}`}>Ödənilib</p><p className={`text-2xl font-bold transition-colors duration-300 ${t.title}`}>{grouped.paid.length}</p></div>
        <div className={`${t.cardBg} border rounded-xl p-4 transition-colors duration-300`}><p className={`text-sm ${t.muted}`}>Gecikib</p><p className="text-2xl font-bold text-red-400">{grouped.overdue.length}</p></div>
      </div>}

      <div className={`${t.cardBg} border rounded-xl overflow-hidden transition-colors duration-300`}>
        <div className="overflow-x-auto">
          <table className="w-full text-sm">
            <thead><tr className={`${t.thClass} transition-colors duration-300`}>
              <th className="text-left py-3 px-4">Tələbə</th><th className="text-left py-3 px-4">Qrup</th><th className="text-left py-3 px-4">Ay</th><th className="text-right py-3 px-4">Məbləğ</th><th className="text-right py-3 px-4">Ödənib</th><th className="text-right py-3 px-4">Qalıq</th><th className="text-left py-3 px-4">Son tarix</th><th className="text-left py-3 px-4">Status</th><th className="text-right py-3 px-4">Əməliyyat</th>
            </tr></thead>
            <tbody>
              {isLoading ? <tr><td colSpan={9} className={`text-center py-8 ${t.muted}`}>Yüklənir...</td></tr>
              : payments.length === 0 ? <tr><td colSpan={9} className={`text-center py-8 ${t.muted}`}>Ödəniş tapılmadı</td></tr>
              : payments.map((p, i) => {
                const st = statusConfig[p.status];
                return (
                  <motion.tr key={p.id} initial={{ opacity: 0 }} animate={{ opacity: 1 }} transition={{ delay: i * 0.02 }} className={`border-b ${t.rowBorder} ${t.rowHover} transition-colors duration-300`}>
                    <td className={`py-3 px-4 font-medium transition-colors duration-300 ${t.tdClass}`}>{p.studentFirstName} {p.studentLastName}</td>
                    <td className={`py-3 px-4 transition-colors duration-300 ${t.tdMuted}`}>{p.groupNumber}</td>
                    <td className={`py-3 px-4 transition-colors duration-300 ${t.tdMuted}`}>{monthNames[p.paymentMonth]} {p.paymentYear}</td>
                    <td className={`py-3 px-4 text-right font-medium transition-colors duration-300 ${t.tdClass}`}>{p.amount.toLocaleString()} AZN</td>
                    <td className={`py-3 px-4 text-right ${t.tdClass}`}>{(p.paidAmount||0).toLocaleString()} AZN</td>
                    <td className={`py-3 px-4 text-right ${t.tdClass}`}>{(p.remainingAmount??p.amount).toLocaleString()} AZN</td>
                    <td className={`py-3 px-4 ${t.tdMuted}`}>{p.dueDate||'—'}</td>
                    <td className="py-3 px-4"><span className={`inline-flex items-center gap-1 px-2 py-1 rounded text-xs ${st.color}`}>{st.icon} {st.label}</span></td>
                    <td className="py-3 px-4 text-right">
                      <div className="flex items-center justify-end gap-1">
                        {(p.remainingAmount??p.amount)>0 && !p.frozen && (
                          <button onClick={() => {setPayAmount(p.remainingAmount??p.amount);setPayModal(p);}} className="px-3 py-1 bg-emerald-600 hover:bg-emerald-700 text-white text-xs rounded-lg transition-colors">Ödə</button>
                        )}
                        <button onClick={()=>setHistoryId(p.id)} className="text-xs text-blue-500">Qəbzlər</button>
                        <button onClick={()=>freezeMutation.mutate({id:p.id,frozen:!p.frozen})} disabled={freezeMutation.isPending} className="text-xs text-sky-500">{p.frozen?'Aç':'Dondur'}</button>
                        {isAdmin && <button onClick={()=>applyDiscount(p)} className="text-xs text-amber-500">Endirim</button>}
                        {isAdmin && <button onClick={() => { if (confirm('Silmək istəyirsiniz?')) deletePaymentMutation.mutate(p.id); }} className={`p-1 rounded transition-colors ${t.iconBtnDanger}`} title="Sil"><Trash2 size={14} /></button>}
                      </div>
                    </td>
                  </motion.tr>
                );
              })}
            </tbody>
          </table>
        </div>
      </div>

      {/* Pay Modal */}
      <AnimatePresence>
        {payModal && (
          <>
            <motion.div initial={{ opacity: 0 }} animate={{ opacity: 1 }} exit={{ opacity: 0 }} className={`fixed inset-0 z-40 ${t.overlay}`} onClick={() => setPayModal(null)} />
            <motion.div initial={{ opacity: 0, scale: 0.95 }} animate={{ opacity: 1, scale: 1 }} exit={{ opacity: 0, scale: 0.95 }} className="fixed inset-0 z-50 flex items-center justify-center p-4">
              <div className={`${t.modalBg} border rounded-xl w-full max-w-sm p-6 transition-colors duration-300`}>
                <h2 className={`text-lg font-semibold mb-2 transition-colors duration-300 ${t.title}`}>Ödəniş Qeydə Al</h2>
                <p className={`text-sm mb-4 transition-colors duration-300 ${t.muted}`}>{payModal.studentFirstName} {payModal.studentLastName} - {payModal.amount} AZN</p>
                <div className="mb-4">
                  <label className={`block text-sm mb-2 transition-colors duration-300 ${t.muted}`}>Ödənilən məbləğ (maks: {payModal.remainingAmount??payModal.amount} AZN)</label>
                  <input className={inputCls} type="number" min={0.01} max={payModal.remainingAmount??payModal.amount} step={0.01} value={payAmount} onChange={e=>setPayAmount(Number(e.target.value))} required/>
                </div>
                <div className="mb-4">
                  <label className={`block text-sm mb-2 transition-colors duration-300 ${t.muted}`}>Ödəniş Üsulu</label>
                  <div className="grid grid-cols-3 gap-2">
                    {[{ value: PaymentMethod.NAGD, label: 'Nağd' }, { value: PaymentMethod.KART, label: 'Kart' }, { value: PaymentMethod.KOCURME, label: 'Köçürmə' }].map((m) => (
                      <button key={m.value} onClick={() => setPayMethod(m.value)} className={`py-2 rounded-lg text-sm transition-colors ${payMethod === m.value ? 'bg-indigo-600 text-white' : `${t.cancelBtn}`}`}>{m.label}</button>
                    ))}
                  </div>
                </div>
                <div className="flex gap-3">
                  <button onClick={() => setPayModal(null)} className={`flex-1 py-2 rounded-lg transition-colors ${t.cancelBtn}`}>Ləğv</button>
                  <button onClick={() => recordPaymentMutation.mutate({ id: payModal.id, method: payMethod, amount:payAmount })} disabled={recordPaymentMutation.isPending || payAmount<=0 || payAmount>(payModal.remainingAmount??payModal.amount)} className="flex-1 py-2 bg-emerald-600 hover:bg-emerald-700 text-white rounded-lg disabled:opacity-50">Təsdiqlə</button>
                </div>
              </div>
            </motion.div>
          </>
        )}
      </AnimatePresence>

      {historyId !== null && <div role="dialog" aria-modal="true" className="fixed inset-0 z-50 bg-black/60 grid place-items-center p-4">
        <div className={`w-full max-w-lg p-6 rounded-xl space-y-3 ${t.modalBg}`}>
          <div className="flex justify-between"><h2 className={`text-lg font-bold ${t.title}`}>Ödəniş qəbzləri</h2><button onClick={()=>setHistoryId(null)}>Bağla</button></div>
          {transactions.length===0?<p>Hələ ödəniş yoxdur</p>:transactions.map(tx=><div key={tx.id} className="flex justify-between gap-3 items-center border-b py-2">
            <span>{tx.paidAt.slice(0,16).replace('T',' ')} · {tx.amount} AZN · {tx.method}</span>
            <button className="text-indigo-500" onClick={()=>paymentsApi.downloadReceipt(tx.id).catch(()=>toast.error('Qəbz yüklənmədi'))}>PDF</button>
          </div>)}
        </div>
      </div>}

      {/* Create Payment Modal */}
      <AnimatePresence>
        {showCreate && (
          <>
            <motion.div initial={{ opacity: 0 }} animate={{ opacity: 1 }} exit={{ opacity: 0 }} className={`fixed inset-0 z-40 ${t.overlay}`} onClick={() => setShowCreate(false)} />
            <motion.div initial={{ opacity: 0, scale: 0.95 }} animate={{ opacity: 1, scale: 1 }} exit={{ opacity: 0, scale: 0.95 }} className="fixed inset-0 z-50 flex items-center justify-center p-4">
              <div className={`${t.modalBg} border rounded-xl w-full max-w-md p-6 transition-colors duration-300`}>
                <div className="flex items-center justify-between mb-6">
                  <h2 className={`text-lg font-semibold transition-colors duration-300 ${t.title}`}>Yeni Ödəniş</h2>
                  <button onClick={() => setShowCreate(false)} className={t.iconBtn}><X size={20} /></button>
                </div>
                <div className="space-y-4">
                  <div><label className={`block text-sm mb-1 ${t.label}`}>Tələbə</label><select value={createForm.studentId} onChange={(e) => setCreateForm({ ...createForm, studentId: +e.target.value })} className={inputCls}><option value={0}>Seçin...</option>{students.map((s) => <option key={s.id} value={s.id}>{s.firstName} {s.lastName}</option>)}</select></div>
                  <div><label className={`block text-sm mb-1 ${t.label}`}>Qrup</label><select value={createForm.groupId} onChange={(e) => setCreateForm({ ...createForm, groupId: +e.target.value })} className={inputCls}><option value={0}>Seçin...</option>{groups.map((g) => <option key={g.id} value={g.id}>{g.groupNumber}</option>)}</select></div>
                  <div className="grid grid-cols-2 gap-4">
                    <div><label className={`block text-sm mb-1 ${t.label}`}>İl</label><input type="number" value={createForm.paymentYear} onChange={(e) => setCreateForm({ ...createForm, paymentYear: +e.target.value })} className={inputCls} /></div>
                    <div><label className={`block text-sm mb-1 ${t.label}`}>Ay</label><select value={createForm.paymentMonth} onChange={(e) => setCreateForm({ ...createForm, paymentMonth: +e.target.value })} className={inputCls}>{monthNames.slice(1).map((n, i) => <option key={i + 1} value={i + 1}>{n}</option>)}</select></div>
                  </div>
                  <div><label className={`block text-sm mb-1 ${t.label}`}>Məbləğ (AZN)</label><input type="number" value={createForm.amount || ''} onChange={(e) => setCreateForm({ ...createForm, amount: +e.target.value })} className={inputCls} min={0} step={0.01} required /></div>
                  <div><label className={`block text-sm mb-1 ${t.label}`}>Qeyd</label><input type="text" value={createForm.note} onChange={(e) => setCreateForm({ ...createForm, note: e.target.value })} className={inputCls} /></div>
                  <button onClick={() => { if (createForm.studentId && createForm.groupId) createPaymentMutation.mutate(createForm); }} disabled={!createForm.studentId || !createForm.groupId || createPaymentMutation.isPending} className="w-full py-2 bg-indigo-600 hover:bg-indigo-700 text-white font-medium rounded-lg disabled:opacity-50">Yarat</button>
                </div>
              </div>
            </motion.div>
          </>
        )}
      </AnimatePresence>
    </div>
  );
};
