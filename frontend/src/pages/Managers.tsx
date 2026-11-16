import React, { useState } from 'react';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { useThemeClasses } from '../lib/theme';
import api from '../api/client';
import toast from 'react-hot-toast';

interface Manager { id: number; email: string; fullName: string; active: boolean }
export const Managers: React.FC = () => {
  const t = useThemeClasses();
  const cache = useQueryClient();
  const [form, setForm] = useState({ email: '', fullName: '', password: '' });
  const {data = [], isLoading} = useQuery<Manager[]>({
    queryKey: ['managers'], queryFn: async () => (await api.get('/admin/managers')).data
  });
  const add = useMutation({
    mutationFn: async () => (await api.post('/admin/managers', form)).data,
    onSuccess: () => {cache.invalidateQueries({queryKey:['managers']}); setForm({email:'',fullName:'',password:''});toast.success('Menecer yaradıldı');},
    onError: () => toast.error('Menecer yaradılmadı')
  });
  const toggle = useMutation({
    mutationFn: async (m:Manager) => (await api.put('/admin/managers/' + m.id + '/active', null, {params:{active:!m.active}})).data,
    onSuccess:()=>cache.invalidateQueries({queryKey:['managers']}),
    onError:()=>toast.error('Status dəyişdirilmədi')
  });
  const input = `w-full rounded-lg px-3 py-2 ${t.inputBgModal}`;
  return <section className="space-y-6">
    <h1 className={`text-2xl font-bold ${t.title}`}>Menecerlər</h1>
    <form className={`p-5 rounded-xl border space-y-3 ${t.cardBg}`} onSubmit={e=>{e.preventDefault();add.mutate();}}>
      <h2 className={`font-semibold ${t.title}`}>Yeni menecer</h2>
      <div className="grid gap-3 sm:grid-cols-3">
        <label className={t.label}>Ad və soyad<input className={input} value={form.fullName} required onChange={e=>setForm({...form,fullName:e.target.value})}/></label>
        <label className={t.label}>E-poçt<input className={input} type="email" value={form.email} required onChange={e=>setForm({...form,email:e.target.value})}/></label>
        <label className={t.label}>Şifrə (12+ simvol)<input className={input} type="password" minLength={12} autoComplete="new-password" value={form.password} required onChange={e=>setForm({...form,password:e.target.value})}/></label>
      </div>
      <button type="submit" disabled={add.isPending} className="bg-indigo-600 text-white px-4 py-2 rounded-lg disabled:opacity-50">Əlavə et</button>
    </form>
    <div className={`rounded-xl border p-4 ${t.cardBg}`}>
      {isLoading ? 'Yüklənir...' : data.length===0 ? 'Menecer yoxdur' :
      <div className="space-y-3">{data.map(m=><div key={m.id} className="flex items-center justify-between gap-3 border-b border-slate-700/20 pb-3">
        <div><p className={`font-semibold ${t.title}`}>{m.fullName}</p><p className={t.muted}>{m.email} · {m.active?'Aktiv':'Deaktiv'}</p></div>
        <button onClick={()=>toggle.mutate(m)} disabled={toggle.isPending} className="border rounded-lg px-3 py-2">{m.active?'Deaktiv et':'Aktiv et'}</button>
      </div>)}</div>}
    </div>
  </section>;
};
