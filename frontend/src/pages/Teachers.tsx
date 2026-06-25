import React, { useState } from 'react';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { motion, AnimatePresence } from 'framer-motion';
import { teachersApi } from '../api/teachers';
import { Teacher } from '../types';
import { useThemeClasses } from '../lib/theme';
import { Plus, Edit2, Trash2, Archive, X } from 'lucide-react';
import toast from 'react-hot-toast';

const emptyTeacher: Partial<Teacher> = { firstName: '', lastName: '', fin: '', email: '', phone: '' };

export const Teachers: React.FC = () => {
  const t = useThemeClasses();
  const [showModal, setShowModal] = useState(false);
  const [editing, setEditing] = useState<Teacher | null>(null);
  const [form, setForm] = useState<Partial<Teacher>>(emptyTeacher);
  const queryClient = useQueryClient();

  const { data: teachers = [], isLoading } = useQuery({ queryKey: ['teachers'], queryFn: teachersApi.getAll });

  const createMutation = useMutation({
    mutationFn: teachersApi.create,
    onSuccess: () => { queryClient.invalidateQueries({ queryKey: ['teachers'] }); toast.success('Müəllim yaradıldı'); closeModal(); },
    onError: (err: any) => toast.error(err.response?.data?.message || 'Xəta'),
  });
  const updateMutation = useMutation({
    mutationFn: ({ id, data }: { id: number; data: Partial<Teacher> }) => teachersApi.update(id, data),
    onSuccess: () => { queryClient.invalidateQueries({ queryKey: ['teachers'] }); toast.success('Müəllim yeniləndi'); closeModal(); },
    onError: (err: any) => toast.error(err.response?.data?.message || 'Xəta'),
  });
  const deleteMutation = useMutation({ mutationFn: teachersApi.delete, onSuccess: () => { queryClient.invalidateQueries({ queryKey: ['teachers'] }); toast.success('Müəllim silindi'); } });
  const archiveMutation = useMutation({ mutationFn: teachersApi.archive, onSuccess: () => { queryClient.invalidateQueries({ queryKey: ['teachers'] }); toast.success('Müəllim arxivləndi'); } });

  const openCreate = () => { setEditing(null); setForm(emptyTeacher); setShowModal(true); };
  const openEdit = (t: Teacher) => { setEditing(t); setForm({ ...t }); setShowModal(true); };
  const closeModal = () => { setShowModal(false); setEditing(null); setForm(emptyTeacher); };
  const handleSubmit = (e: React.FormEvent) => { e.preventDefault(); editing?.id ? updateMutation.mutate({ id: editing.id, data: form }) : createMutation.mutate(form); };

  const inputCls = `w-full px-3 py-2 rounded-lg focus:outline-none focus:ring-2 focus:ring-indigo-500 transition-colors duration-300 ${t.inputBgModal}`;

  return (
    <div className={`space-y-6 transition-colors duration-300`}>
      <div className="flex items-center justify-between">
        <h1 className={`text-2xl font-bold transition-colors duration-300 ${t.title}`}>Müəllimlər</h1>
        <button onClick={openCreate} className="flex items-center gap-2 px-4 py-2 bg-indigo-600 hover:bg-indigo-700 text-white font-medium rounded-lg"><Plus size={18} /> Yeni Müəllim</button>
      </div>

      <div className={`${t.cardBg} border rounded-xl overflow-hidden transition-colors duration-300`}>
        <div className="overflow-x-auto">
          <table className="w-full text-sm">
            <thead>
              <tr className={`${t.thClass} transition-colors duration-300`}>
                <th className="text-left py-3 px-4">Ad Soyad</th>
                <th className="text-left py-3 px-4">FİN</th>
                <th className="text-left py-3 px-4">E-poçt</th>
                <th className="text-left py-3 px-4">Telefon</th>
                <th className="text-left py-3 px-4">Status</th>
                <th className="text-right py-3 px-4">Əməliyyatlar</th>
              </tr>
            </thead>
            <tbody>
              {isLoading ? <tr><td colSpan={6} className={`text-center py-8 ${t.muted}`}>Yüklənir...</td></tr> : teachers.map((tch, i) => (
                <motion.tr key={tch.id} initial={{ opacity: 0 }} animate={{ opacity: 1 }} transition={{ delay: i * 0.03 }} className={`border-b ${t.rowBorder} ${t.rowHover} transition-colors duration-300`}>
                  <td className={`py-3 px-4 font-medium transition-colors duration-300 ${t.tdClass}`}>{tch.firstName} {tch.lastName}</td>
                  <td className={`py-3 px-4 font-mono transition-colors duration-300 ${t.tdMuted}`}>{tch.fin}</td>
                  <td className={`py-3 px-4 transition-colors duration-300 ${t.tdMuted}`}>{tch.email}</td>
                  <td className={`py-3 px-4 transition-colors duration-300 ${t.tdMuted}`}>{tch.phone || '—'}</td>
                  <td className="py-3 px-4">
                    <span className={`px-2 py-1 rounded text-xs ${tch.status === 'AKTIV' ? 'bg-emerald-500/10 text-emerald-400' : 'bg-slate-700 text-slate-400'}`}>
                      {tch.status === 'AKTIV' ? 'Aktiv' : 'Arxiv'}
                    </span>
                  </td>
                  <td className="py-3 px-4">
                    <div className="flex items-center justify-end gap-1">
                      <button onClick={() => openEdit(tch)} className={`p-1.5 rounded-lg transition-colors ${t.iconBtn}`}><Edit2 size={15} /></button>
                      <button onClick={() => archiveMutation.mutate(tch.id)} className={`p-1.5 rounded-lg transition-colors ${t.iconBtnWarn}`}><Archive size={15} /></button>
                      <button onClick={() => { if (confirm('Silmək istəyirsiniz?')) deleteMutation.mutate(tch.id); }} className={`p-1.5 rounded-lg transition-colors ${t.iconBtnDanger}`}><Trash2 size={15} /></button>
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
                  <h2 className={`text-lg font-semibold transition-colors duration-300 ${t.title}`}>{editing ? 'Redaktə Et' : 'Yeni Müəllim'}</h2>
                  <button onClick={closeModal} className={t.iconBtn}><X size={20} /></button>
                </div>
                <form onSubmit={handleSubmit} className="space-y-4">
                  <div className="grid grid-cols-2 gap-4">
                    <div><label className={`block text-sm mb-1 ${t.label}`}>Ad</label><input type="text" value={form.firstName || ''} onChange={(e) => setForm({ ...form, firstName: e.target.value })} className={inputCls} required /></div>
                    <div><label className={`block text-sm mb-1 ${t.label}`}>Soyad</label><input type="text" value={form.lastName || ''} onChange={(e) => setForm({ ...form, lastName: e.target.value })} className={inputCls} required /></div>
                  </div>
                  <div><label className={`block text-sm mb-1 ${t.label}`}>FİN (7 simvol)</label><input type="text" value={form.fin || ''} onChange={(e) => setForm({ ...form, fin: e.target.value })} className={`${inputCls} font-mono`} maxLength={7} required /></div>
                  <div><label className={`block text-sm mb-1 ${t.label}`}>E-poçt</label><input type="email" value={form.email || ''} onChange={(e) => setForm({ ...form, email: e.target.value })} className={inputCls} required /></div>
                  <div><label className={`block text-sm mb-1 ${t.label}`}>Telefon</label><input type="text" value={form.phone || ''} onChange={(e) => setForm({ ...form, phone: e.target.value })} className={inputCls} /></div>
                  <div className="flex gap-3 pt-2">
                    <button type="button" onClick={closeModal} className={`flex-1 py-2 rounded-lg transition-colors ${t.cancelBtn}`}>Ləğv Et</button>
                    <button type="submit" className="flex-1 py-2 bg-indigo-600 hover:bg-indigo-700 text-white rounded-lg transition-colors">{editing ? 'Yenilə' : 'Yarat'}</button>
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
