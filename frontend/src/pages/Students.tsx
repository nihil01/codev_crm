import React, { useState } from 'react';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { motion, AnimatePresence } from 'framer-motion';
import { studentsApi } from '../api/students';
import { Student } from '../types';
import { useThemeClasses } from '../lib/theme';
import { Plus, Search, Edit2, Trash2, Archive, ArchiveRestore, X } from 'lucide-react';
import toast from 'react-hot-toast';

const emptyStudent: Partial<Student> = { firstName: '', lastName: '', fin: '', email: '', phone: '', birthDate: '', status: 'AKTIV', parentName: '', parentPhone: '', address: '', note: '', source: 'INSTAGRAM', exitReason: '', reminderConsent: false };

export const Students: React.FC = () => {
  const t = useThemeClasses();
  const [search, setSearch] = useState('');
  const [showModal, setShowModal] = useState(false);
  const [editingStudent, setEditingStudent] = useState<Partial<Student> | null>(null);
  const [form, setForm] = useState<Partial<Student>>(emptyStudent);
  const queryClient = useQueryClient();

  const { data: students = [], isLoading } = useQuery({
    queryKey: ['students', search],
    queryFn: () => (search ? studentsApi.search(search) : studentsApi.getAll()),
  });

  const createMutation = useMutation({
    mutationFn: studentsApi.create,
    onSuccess: () => { queryClient.invalidateQueries({ queryKey: ['students'] }); toast.success('Tələbə yaradıldı'); closeModal(); },
    onError: (err: any) => toast.error(err.response?.data?.message || 'Xəta'),
  });
  const updateMutation = useMutation({
    mutationFn: ({ id, data }: { id: number; data: Partial<Student> }) => studentsApi.update(id, data),
    onSuccess: () => { queryClient.invalidateQueries({ queryKey: ['students'] }); toast.success('Tələbə yeniləndi'); closeModal(); },
    onError: (err: any) => toast.error(err.response?.data?.message || 'Xəta'),
  });
  const deleteMutation = useMutation({
    mutationFn: studentsApi.delete,
    onSuccess: () => { queryClient.invalidateQueries({ queryKey: ['students'] }); toast.success('Tələbə silindi'); },
  });
  const toggleArchiveMutation = useMutation({
    mutationFn: (student: Student) => studentsApi.update(student.id, { ...student, archived: !student.archived }),
    onSuccess: (_, student) => {
      queryClient.invalidateQueries({ queryKey: ['students'] });
      toast.success(student.archived ? 'Tələbə arxivdən çıxarıldı' : 'Tələbə arxivləndi');
    },
  });

  const openCreate = () => { setEditingStudent(null); setForm(emptyStudent); setShowModal(true); };
  const openEdit = (s: Student) => { setEditingStudent(s); setForm({ ...s }); setShowModal(true); };
  const closeModal = () => { setShowModal(false); setEditingStudent(null); setForm(emptyStudent); };
  const handleSubmit = (e: React.FormEvent) => {
    e.preventDefault();
    editingStudent?.id ? updateMutation.mutate({ id: editingStudent.id, data: form }) : createMutation.mutate(form);
  };

  const inputCls = `w-full px-3 py-2 rounded-lg focus:outline-none focus:ring-2 focus:ring-indigo-500 transition-colors duration-300 ${t.inputBgModal}`;

  return (
    <div className={`space-y-6 transition-colors duration-300`}>
      <div className="flex flex-col sm:flex-row items-start sm:items-center justify-between gap-4">
        <h1 className={`text-2xl font-bold transition-colors duration-300 ${t.title}`}>Tələbələr</h1>
        <button onClick={openCreate} className="flex items-center gap-2 px-4 py-2 bg-indigo-600 hover:bg-indigo-700 text-white font-medium rounded-lg transition-colors">
          <Plus size={18} /> Yeni Tələbə
        </button>
      </div>

      <div className="relative">
        <Search size={18} className="absolute left-3 top-1/2 -translate-y-1/2 text-slate-500" />
        <input type="text" value={search} onChange={(e) => setSearch(e.target.value)} placeholder="Ad, soyad və ya FİN ilə axtar..."
          className={`w-full pl-10 pr-4 py-2.5 rounded-lg focus:outline-none focus:ring-2 focus:ring-indigo-500 transition-colors duration-300 ${t.inputBg}`} />
      </div>

      <div className={`${t.cardBg} border rounded-xl overflow-hidden transition-colors duration-300`}>
        <div className="overflow-x-auto">
          <table className="w-full text-sm">
            <thead>
              <tr className={`${t.thClass} transition-colors duration-300`}>
                <th className="text-left py-3 px-4">Ad Soyad</th>
                <th className="text-left py-3 px-4">FİN</th>
                <th className="text-left py-3 px-4">Doğum</th>
                <th className="text-left py-3 px-4">E-poçt</th>
                <th className="text-left py-3 px-4">Telefon</th>
                <th className="text-right py-3 px-4">Əməliyyatlar</th>
              </tr>
            </thead>
            <tbody>
              {isLoading ? (
                <tr><td colSpan={6} className={`text-center py-8 ${t.muted}`}>Yüklənir...</td></tr>
              ) : students.length === 0 ? (
                <tr><td colSpan={6} className={`text-center py-8 ${t.muted}`}>Tələbə tapılmadı</td></tr>
              ) : students.map((s, i) => (
                <motion.tr key={s.id} initial={{ opacity: 0 }} animate={{ opacity: 1 }} transition={{ delay: i * 0.03 }} className={`border-b ${t.rowBorder} ${t.rowHover} transition-colors duration-300`}>
                  <td className={`py-3 px-4 font-medium transition-colors duration-300 ${t.tdClass}`}>
                    {s.firstName} {s.lastName}
                    {s.archived && <span className="ml-2 text-xs bg-slate-700 text-slate-400 px-2 py-0.5 rounded">Arxiv</span>}
                  </td>
                  <td className={`py-3 px-4 font-mono transition-colors duration-300 ${t.tdMuted}`}>{s.fin}</td>
                  <td className={`py-3 px-4 transition-colors duration-300 ${t.tdSubtext}`}>{s.birthDate ? new Date(s.birthDate).toLocaleDateString('az-AZ') : '—'}</td>
                  <td className={`py-3 px-4 transition-colors duration-300 ${t.tdMuted}`}>{s.email}</td>
                  <td className={`py-3 px-4 transition-colors duration-300 ${t.tdMuted}`}>{s.phone || '—'}</td>
                  <td className="py-3 px-4">
                    <div className="flex items-center justify-end gap-1">
                      <button onClick={() => openEdit(s)} className={`p-1.5 rounded-lg transition-colors ${t.iconBtn}`} title="Redaktə"><Edit2 size={15} /></button>
                      <button onClick={() => toggleArchiveMutation.mutate(s)} className={`p-1.5 rounded-lg transition-colors ${t.iconBtnWarn}`} title={s.archived ? 'Arxivdən çıxar' : 'Arxivlə'}>
                        {s.archived ? <ArchiveRestore size={15} /> : <Archive size={15} />}
                      </button>
                      <button onClick={() => { if (confirm('Silmək istəyirsiniz?')) deleteMutation.mutate(s.id); }} className={`p-1.5 rounded-lg transition-colors ${t.iconBtnDanger}`} title="Sil"><Trash2 size={15} /></button>
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
              <div className={`${t.modalBg} border rounded-xl w-full max-w-md p-6 max-h-[90vh] overflow-y-auto transition-colors duration-300`}>
                <div className="flex items-center justify-between mb-6">
                  <h2 className={`text-lg font-semibold transition-colors duration-300 ${t.title}`}>{editingStudent ? 'Tələbəni Redaktə Et' : 'Yeni Tələbə'}</h2>
                  <button onClick={closeModal} className={t.iconBtn}><X size={20} /></button>
                </div>
                <form onSubmit={handleSubmit} className="space-y-4">
                  <div className="grid grid-cols-2 gap-4">
                    <div><label className={`block text-sm mb-1 ${t.label}`}>Ad</label><input type="text" value={form.firstName || ''} onChange={(e) => setForm({ ...form, firstName: e.target.value })} className={inputCls} required /></div>
                    <div><label className={`block text-sm mb-1 ${t.label}`}>Soyad</label><input type="text" value={form.lastName || ''} onChange={(e) => setForm({ ...form, lastName: e.target.value })} className={inputCls} required /></div>
                  </div>
                  <div><label className={`block text-sm mb-1 ${t.label}`}>FİN (7 simvol)</label><input type="text" value={form.fin || ''} onChange={(e) => setForm({ ...form, fin: e.target.value })} className={`${inputCls} font-mono`} maxLength={7} /></div>
                  <div><label className={`block text-sm mb-1 ${t.label}`}>Doğum tarixi</label><input type="date" value={form.birthDate || ''} onChange={(e) => setForm({ ...form, birthDate: e.target.value })} className={inputCls} /></div>
                  <div><label className={`block text-sm mb-1 ${t.label}`}>E-poçt</label><input type="email" value={form.email || ''} onChange={(e) => setForm({ ...form, email: e.target.value })} className={inputCls} /></div>
                  <div><label className={`block text-sm mb-1 ${t.label}`}>Telefon</label><input type="text" value={form.phone || ''} onChange={(e) => setForm({ ...form, phone: e.target.value })} className={inputCls} /></div>
                  <label className="flex gap-2 items-center">
                    <input type="checkbox" checked={!!form.reminderConsent}
                      onChange={e=>setForm({...form,reminderConsent:e.target.checked})}/>
                    <span className={t.label}>Ödəniş xatırlatmaları üçün mesaj göndərilməsinə razılıq</span>
                  </label>
                  <div><label className={`block text-sm mb-1 ${t.label}`}>Mənbə</label><select value={form.source || ''} onChange={e=>setForm({...form,source:e.target.value as Student['source']})} className={inputCls}><option value="">Seçin</option><option value="INSTAGRAM">Instagram</option><option value="WHATSAPP">WhatsApp</option><option value="TIKTOK">TikTok</option><option value="ZENG">Zəng</option><option value="TOVSIYE">Tövsiyə</option><option value="SAYT">Sayt</option><option value="BIRBASA_GELIS">Birbaşa gəliş</option></select></div>
                  <div><label className={`block text-sm mb-1 ${t.label}`}>Valideyn adı</label><input className={inputCls} value={form.parentName || ''} onChange={e=>setForm({...form,parentName:e.target.value})}/></div>
                  <div><label className={`block text-sm mb-1 ${t.label}`}>Valideyn telefonu</label><input className={inputCls} value={form.parentPhone || ''} onChange={e=>setForm({...form,parentPhone:e.target.value})}/></div>
                  <div><label className={`block text-sm mb-1 ${t.label}`}>Ünvan</label><input className={inputCls} value={form.address || ''} onChange={e=>setForm({...form,address:e.target.value})}/></div>
                  <div><label className={`block text-sm mb-1 ${t.label}`}>Status</label><select value={form.status || 'AKTIV'} onChange={e=>setForm({...form,status:e.target.value as Student['status']})} className={inputCls}><option value="AKTIV">Aktiv</option><option value="DONDURULUB">Dondurulub</option><option value="BITIRIB">Bitirib</option><option value="AYRILIB">Ayrılıb</option></select></div>
                  {form.status==='AYRILIB' && <div><label className={`block text-sm mb-1 ${t.label}`}>Ayrılma səbəbi</label><textarea required className={inputCls} value={form.exitReason || ''} onChange={e=>setForm({...form,exitReason:e.target.value})}/></div>}
                  <div><label className={`block text-sm mb-1 ${t.label}`}>Qeyd</label><textarea className={inputCls} value={form.note || ''} onChange={e=>setForm({...form,note:e.target.value})}/></div>
                  <div className="flex gap-3 pt-2">
                    <button type="button" onClick={closeModal} className={`flex-1 py-2 rounded-lg transition-colors ${t.cancelBtn}`}>Ləğv Et</button>
                    <button type="submit" disabled={createMutation.isPending || updateMutation.isPending} className="flex-1 py-2 bg-indigo-600 hover:bg-indigo-700 text-white font-medium rounded-lg transition-colors disabled:opacity-50">
                      {editingStudent ? 'Yenilə' : 'Yarat'}
                    </button>
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
