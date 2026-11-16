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
  const [draggingId,setDraggingId] = useState<number|null>(null);
  const [dragOverStage,setDragOverStage] = useState<Lead['stage']|null>(null);
  const {data:leads=[], isLoading} = useQuery({queryKey:['leads'],queryFn:leadsApi.getAll});
  const {data:managers=[]} = useQuery<Manager[]>({
    queryKey:['managers'],queryFn:async()=> (await api.get('/admin/managers')).data,enabled:isAdmin
  });
  const stageMove = useMutation({
    mutationFn: ({id,stage,lostReason,consultationAt}:{
      id:number;stage:Lead['stage'];lostReason?:string;consultationAt?:string
    }) => leadsApi.moveStage(id,{stage,lostReason,consultationAt}),
    onSuccess:()=>{
      qc.invalidateQueries({queryKey:['leads']});
      toast.success('Lead yeni mərhələyə keçirildi');
    },
    onError:(error:any)=>toast.error(error?.response?.data?.message||
      'Mərhələ dəyişdirilmədi',{duration:6000})
  });
  const save = useMutation({
    mutationFn:()=> edit ? leadsApi.update(edit.id,form) : leadsApi.create(form),
    onSuccess:()=>{qc.invalidateQueries({queryKey:['leads']});setModal(false);toast.success('Lead yadda saxlanıldı');},
    onError:(error:any)=>toast.error(error?.response?.data?.message||'Lead saxlanılmadı',{duration:6000})
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
  const changeStage = (lead:Lead,target:Lead['stage'])=>{
    if(target===lead.stage || stageMove.isPending || convert.isPending)return;
    if(lead.convertedStudentId && target!=='QEYDIYYAT'){
      toast.error('Qeydiyyatdan keçmiş lead başqa mərhələyə keçirilmir');
      return;
    }
    if(target==='QEYDIYYAT'){
      if(!window.confirm('Bu leaddən yeni tələbə yaradılacaq. Davam edilsin?'))return;
      convert.mutate(lead.id);
      return;
    }
    let lostReason: string|undefined;
    let consultationAt: string|undefined;
    if(target==='ITIRILDI'){
      const reason=window.prompt('Lead niyə itirilib? Səbəbi yazın:');
      if(!reason?.trim())return;
      lostReason=reason.trim();
    }
    if(target==='SINAQ_KONSULTASIYA'){
      const entered=window.prompt('Sınaq / konsultasiya tarixi (YYYY-MM-DDTHH:mm):',
        lead.consultationAt?.slice(0,16)||'');
      if(!entered)return;
      if(!/^\d{4}-\d{2}-\d{2}T\d{2}:\d{2}$/.test(entered) ||
         Number.isNaN(new Date(entered).getTime())){
        toast.error('Tarix formatı yanlışdır. Nümunə: 2026-11-20T14:30');return;
      }
      consultationAt=entered;
    }
    stageMove.mutate({id:lead.id,stage:target,lostReason,consultationAt});
  };
  const dropLead=(event:React.DragEvent<HTMLElement>,stage:Lead['stage'])=>{
    event.preventDefault();
    const raw=event.dataTransfer.getData('text/plain');
    const id=Number(raw||draggingId);
    setDraggingId(null);setDragOverStage(null);
    const lead=leads.find(l=>l.id===id);
    if(lead)changeStage(lead,stage);
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
      {stages.map(stage=><div key={stage.id}
        aria-label={`Mərhələ: ${stage.name}`}
        onDragOver={e=>{e.preventDefault();e.dataTransfer.dropEffect='move';setDragOverStage(stage.id);}}
        onDragLeave={e=>{if(!e.currentTarget.contains(e.relatedTarget as Node))setDragOverStage(null);}}
        onDrop={e=>dropLead(e,stage.id)}
        className={`rounded-xl border p-3 min-h-48 transition-colors ${dragOverStage===stage.id
          ? 'border-indigo-500 bg-indigo-500/20 ring-2 ring-indigo-500/40'
          : 'border-slate-300/30 bg-slate-500/5'}`}>
        <h2 className="mb-3 font-semibold">{stage.name} ({filtered.filter(l=>l.stage===stage.id).length})</h2>
        <div className="space-y-2">{filtered.filter(l=>l.stage===stage.id).map(l=>
          <div key={l.id} draggable={!stageMove.isPending && !convert.isPending}
            onDragStart={e=>{e.dataTransfer.setData('text/plain',String(l.id));e.dataTransfer.effectAllowed='move';setDraggingId(l.id);}}
            onDragEnd={()=>{setDraggingId(null);setDragOverStage(null);}}
            className={`rounded-lg border border-slate-300/20 bg-white/10 p-3 cursor-grab active:cursor-grabbing
              ${draggingId===l.id?'opacity-40':''}`}>
            <div className="font-semibold">{l.fullName}</div>
            <div className="text-sm opacity-70">{l.phone} · {sources[l.source]}</div>
            {l.interestedCourse && <div className="text-sm">{l.interestedCourse}</div>}
            {l.assignedManagerName && <p className="text-xs opacity-70">Menecer: {l.assignedManagerName}</p>}
            <div className="mt-2">
              <label className="text-xs opacity-70" htmlFor={`lead-stage-${l.id}`}>Mərhələni dəyiş</label>
              <select id={`lead-stage-${l.id}`} aria-label="Mərhələni dəyiş"
                className="mt-1 w-full rounded border border-slate-300/30 bg-slate-900 text-white p-1 text-xs"
                value={l.stage} disabled={stageMove.isPending||convert.isPending}
                onChange={e=>changeStage(l,e.target.value as Lead['stage'])}>
                {stages.map(option=><option key={option.id} value={option.id}>{option.name}</option>)}
              </select>
            </div>
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
