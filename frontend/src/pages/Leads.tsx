import React, { useState } from 'react';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import toast from 'react-hot-toast';
import { useAuth } from '../context/AuthContext';
import api from '../api/client';
import { leadsApi } from '../api/leads';
import type { Lead } from '../types';

type Manager = { id: number; fullName: string; active: boolean };
const sources: Record<Lead['source'], string> = {
  INSTAGRAM: 'Instagram', WHATSAPP: 'WhatsApp', TIKTOK: 'TikTok', ZENG: 'Zəng',
  TOVSIYE: 'Tövsiyə', SAYT: 'Sayt', BIRBASA_GELIS: 'Birbaşa gəliş'
};
const stages: {id:Lead['stage'];name:string}[] = [
  {id:'YENI',name:'Yeni'}, {id:'ELAQE_SAXLANILDI',name:'Əlaqə saxlanıldı'},
  {id:'SINAQ_KONSULTASIYA',name:'Sınaq / konsultasiya'}, {id:'DUSUNUR',name:'Düşünür'},
  {id:'QEYDIYYAT',name:'Qeydiyyat'}, {id:'ITIRILDI',name:'İtirildi'}
];
const blank: Partial<Lead> = {
  fullName:'', phone:'', source:'INSTAGRAM', stage:'YENI', assignedManagerId:null,
  interestedCourse:'', consultationAt:'', lostReason:'', note:''
};

export const Leads: React.FC = () => {
  const {user} = useAuth();
  const isAdmin = user?.role === 'ADMIN';
  const qc = useQueryClient();
  const [edit,setEdit] = useState<Lead|null>(null);
  const [form,setForm] = useState<Partial<Lead>>(blank);
  const [modal,setModal] = useState(false);
  const [query,setQuery] = useState('');
  const {data:leads=[], isLoading} = useQuery({queryKey:['leads'],queryFn:leadsApi.getAll});
  const {data:managers=[]} = useQuery<Manager[]>({
    queryKey:['managers'],queryFn:async()=> (await api.get('/admin/managers')).data,enabled:isAdmin
  });
  const save = useMutation({
    mutationFn:()=> edit ? leadsApi.update(edit.id,form) : leadsApi.create(form),
    onSuccess:()=>{qc.invalidateQueries({queryKey:['leads']});setModal(false);toast.success('Lead yadda saxlanıldı');},
    onError:()=>toast.error('Lead saxlanılmadı')
  });
  const convert = useMutation({
    mutationFn:leadsApi.convert,
    onSuccess:({studentId})=>{qc.invalidateQueries({queryKey:['leads']});qc.invalidateQueries({queryKey:['students']});toast.success(`Tələbə yaradıldı (#${studentId})`);},
    onError:(e:any)=>toast.error(e.response?.data?.message||'Qeydiyyat uğursuz oldu')
  });
  const remove = useMutation({
    mutationFn:leadsApi.delete,
    onSuccess:()=>{qc.invalidateQueries({queryKey:['leads']});toast.success('Lead silindi');},
    onError:()=>toast.error('Silmək mümkün olmadı')
  });
  const open = (lead?:Lead)=>{
    setEdit(lead||null);
    setForm(lead?{...lead,consultationAt:lead.consultationAt?.slice(0,16)||''}:{...blank});
    setModal(true);
  };
  const filtered=leads.filter(l=>(l.fullName+' '+l.phone+' '+(l.interestedCourse||'')).toLowerCase().includes(query.toLowerCase()));
  const input='w-full rounded-lg border border-slate-300 bg-white p-2 text-slate-900';
  return <section className="space-y-5">
    <div className="flex flex-wrap items-center justify-between gap-3">
      <h1 className="text-2xl font-bold">Leadlər</h1>
      <button onClick={()=>open()} className="rounded-lg bg-indigo-600 px-4 py-2 text-white">Yeni lead</button>
    </div>
    <input className={input} placeholder="Ad, telefon və kurs üzrə axtarış" value={query} onChange={e=>setQuery(e.target.value)}/>
    {isLoading && <p>Yüklənir...</p>}
    <div className="grid gap-4 md:grid-cols-2 xl:grid-cols-3">
      {stages.map(stage=><div key={stage.id} className="rounded-xl border border-slate-300/30 bg-slate-500/5 p-3 min-h-48">
        <h2 className="mb-3 font-semibold">{stage.name} ({filtered.filter(l=>l.stage===stage.id).length})</h2>
        <div className="space-y-2">{filtered.filter(l=>l.stage===stage.id).map(l=>
          <div key={l.id} className="rounded-lg border border-slate-300/20 bg-white/10 p-3">
            <div className="font-semibold">{l.fullName}</div>
            <div className="text-sm opacity-70">{l.phone} · {sources[l.source]}</div>
            {l.interestedCourse && <div className="text-sm">{l.interestedCourse}</div>}
            {l.assignedManagerName && <p className="text-xs opacity-70">Menecer: {l.assignedManagerName}</p>}
            <div className="mt-3 flex gap-3 text-sm">
              <button onClick={()=>open(l)} className="text-indigo-500">Dəyiş</button>
              {!l.convertedStudentId&&<button disabled={convert.isPending} onClick={()=>{if(window.confirm('Leaddən tələbə yaratmaq istəyirsiniz?'))convert.mutate(l.id);}} className="text-emerald-500">Qeydiyyata al</button>}
              {l.convertedStudentId&&<span className="text-xs text-emerald-500">Tələbə #{l.convertedStudentId}</span>}
              {isAdmin&&<button onClick={()=>{if(window.confirm('Leadi silmək istəyirsiniz?'))remove.mutate(l.id);}} className="text-red-500">Sil</button>}
            </div>
          </div>)}</div>
      </div>)}
    </div>
    {modal&&<div role="dialog" aria-modal="true" className="fixed inset-0 z-50 grid place-items-center bg-black/60 p-4">
      <div className="w-full max-w-xl max-h-[92vh] overflow-auto rounded-xl bg-white p-6 text-slate-900">
        <h2 className="mb-4 text-xl font-semibold">{edit?'Leadi redaktə et':'Yeni lead'}</h2>
        <form className="space-y-3" onSubmit={e=>{e.preventDefault();save.mutate();}}>
          <label className="block text-sm">Ad soyad<input className={input} required value={form.fullName||''} onChange={e=>setForm({...form,fullName:e.target.value})}/></label>
          <label className="block text-sm">Telefon<input className={input} required value={form.phone||''} onChange={e=>setForm({...form,phone:e.target.value})}/></label>
          <label className="block text-sm">Mənbə<select className={input} value={form.source} onChange={e=>setForm({...form,source:e.target.value as Lead['source']})}>
            {Object.entries(sources).map(([key,label])=><option key={key} value={key}>{label}</option>)}
          </select></label>
          <label className="block text-sm">Mərhələ<select className={input} value={form.stage} onChange={e=>setForm({...form,stage:e.target.value as Lead['stage']})}>
            {stages.map(s=><option key={s.id} value={s.id}>{s.name}</option>)}
          </select></label>
          {form.stage==='SINAQ_KONSULTASIYA'&&<label className="block text-sm">Sınaq / konsultasiya tarixi<input className={input} type="datetime-local" required value={form.consultationAt?.slice(0,16)||''} onChange={e=>setForm({...form,consultationAt:e.target.value})}/></label>}
          {form.stage==='ITIRILDI'&&<label className="block text-sm">İtirmə səbəbi<textarea className={input} required value={form.lostReason||''} onChange={e=>setForm({...form,lostReason:e.target.value})}/></label>}
          <label className="block text-sm">Maraqlanılan kurs<input className={input} value={form.interestedCourse||''} onChange={e=>setForm({...form,interestedCourse:e.target.value})}/></label>
          {isAdmin&&<label className="block text-sm">Menecer<select className={input} value={form.assignedManagerId??''} onChange={e=>setForm({...form,assignedManagerId:e.target.value?Number(e.target.value):null})}>
            <option value="">Təyin edilməyib</option>
            {managers.filter(m=>m.active).map(m=><option key={m.id} value={m.id}>{m.fullName}</option>)}
          </select></label>}
          <label className="block text-sm">Qeyd<textarea className={input} value={form.note||''} onChange={e=>setForm({...form,note:e.target.value})}/></label>
          <div className="flex justify-end gap-3 pt-3">
            <button type="button" onClick={()=>setModal(false)} className="rounded-lg border px-4 py-2">Bağla</button>
            <button type="submit" disabled={save.isPending} className="rounded-lg bg-indigo-600 px-4 py-2 text-white disabled:opacity-50">Yadda saxla</button>
          </div>
        </form>
      </div>
    </div>}
  </section>;
};
